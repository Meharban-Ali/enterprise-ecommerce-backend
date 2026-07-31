package com.redis.infrastructure.security;

import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.audit.entity.AuditStatus;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.audit.entity.AuditActionType;
import com.redis.security.service.ApiAbuseDetectionService;
import com.redis.infrastructure.governance.service.ApiGovernanceService;
import com.redis.security.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Autowired(required = false)
    private RateLimitService rateLimitService;

    @Autowired(required = false)
    private ApiSecurityProperties properties = new ApiSecurityProperties();

    @Autowired(required = false)
    private ApiAbuseDetectionService abuseDetectionService;

    @Autowired(required = false)
    private ApiGovernanceService governanceService;

    @Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    public RateLimitingFilter() {}

    public RateLimitingFilter(RateLimitService rateLimitService, ApiSecurityProperties properties,
                              ApiAbuseDetectionService abuseDetectionService, ApiGovernanceService governanceService) {
        this.rateLimitService = rateLimitService;
        this.properties = properties != null ? properties : new ApiSecurityProperties();
        this.abuseDetectionService = abuseDetectionService;
        this.governanceService = governanceService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain)
            throws ServletException, IOException {

        boolean isEnabled = properties != null && properties.isRateLimitEnabled();
        if (!isEnabled || rateLimitService == null) {
            filterChain.doFilter(httpRequest, httpResponse);
            return;
        }

        // ===== RAILWAY PROXY HEADER DIAGNOSTIC LOG (Empirical Verification) =====
        String xRealIp = httpRequest.getHeader("X-Real-IP");
        String xffRaw = httpRequest.getHeader("X-Forwarded-For");
        String cfIp = httpRequest.getHeader("CF-Connecting-IP");
        String remoteAddr = httpRequest.getRemoteAddr();

        log.info("[Railway Proxy Header Audit] URI={} | remoteAddr={} | X-Real-IP={} | CF-Connecting-IP={} | X-Forwarded-For={}",
                httpRequest.getRequestURI(), remoteAddr, xRealIp, cfIp, xffRaw);

        /*
         * CLIENT IP EXTRACTION & TRUST BOUNDARY DOCUMENTATION:
         * Empirically logged on 2026-07-29 to inspect live Railway edge proxy behavior.
         * Railway reverse proxy header behavior can shift if CDN layers or edge routing change.
         * Order of precedence:
         * 1. CF-Connecting-IP (if Cloudflare CDN is active in front of Railway)
         * 2. X-Real-IP (set/overwritten by edge proxy)
         * 3. X-Forwarded-For (rightmost IP appended by proxy)
         * 4. HttpServletRequest.getRemoteAddr() fallback
         *
         * LIMITATION & DEFENSE-IN-DEPTH NOTICE:
         * IP-based rate limiting on unauthenticated endpoints (/api/auth/login, /api/auth/register)
         * serves as a best-effort deterrent. Post-login, account-based rate limiting (keyed by user/API key)
         * acts as the primary, un-spoofable security control.
         */
        String clientIp = cfIp;
        if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = xRealIp;
        }
        if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
            if (xffRaw != null && !xffRaw.isBlank() && !"unknown".equalsIgnoreCase(xffRaw)) {
                String[] ips = xffRaw.split(",");
                clientIp = ips[ips.length - 1].trim();
            }
        }
        if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = remoteAddr;
        }
        String limitKey;
        int limit;
        int window = 60; 

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            String apiKeyId = org.slf4j.MDC.get("apiKeyId");
            if (apiKeyId != null) {
                limitKey = "apikey:" + apiKeyId;
                limit = properties.isAdaptiveRateLimitEnabled() ? 500 : properties.getDefaultRateLimitUser();
            } else {
                limitKey = "user:" + auth.getName();
                if (properties.isAdaptiveRateLimitEnabled()) {
                    boolean isAdmin = auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPER_ADMIN"));
                    if (isAdmin || "system".equals(auth.getName()) || auth.getName().contains("scheduler")) {
                        limit = Integer.MAX_VALUE;
                    } else {
                        limit = 100;
                    }
                } else {
                    limit = properties.getDefaultRateLimitUser();
                }
            }
        } else {
            limitKey = "ip:" + clientIp;
            limit = properties.getDefaultRateLimitAnonymous();
        }

        Map<String, Integer> endpointLimits = properties.getParsedEndpointRateLimits();
        if (properties.isEndpointRateLimitEnabled() && endpointLimits != null) {
        String lookupKey = httpRequest.getMethod() + " " + httpRequest.getRequestURI();
        Integer override = endpointLimits.get(lookupKey);
        if (override == null) {
            override = endpointLimits.get(httpRequest.getRequestURI());
        }
        if (override != null) {
            limit = override;
            limitKey = limitKey + ":" + httpRequest.getMethod() + ":" + httpRequest.getRequestURI();
        }
        }

        boolean allowed = true;
        try {
            allowed = rateLimitService.isAllowed(limitKey, limit, window);
        } catch (Exception e) {
            log.warn("RateLimitService check failed for key {}: {} — bypassing rate limit filter", limitKey, e.getMessage());
            allowed = true;
        }

        if (!allowed) {
            int retryAfter = 60;
            try {
                retryAfter = rateLimitService.getRetryAfterSeconds(limitKey, limit, window);
            } catch (Exception e) {
                log.warn("Failed to get retry after seconds: {}", e.getMessage());
            }
            httpResponse.setStatus(429);
            httpResponse.setHeader("Retry-After", String.valueOf(retryAfter));
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"message\":\"Too Many Requests — please try again later.\",\"code\":\"RATE_LIMIT_EXCEEDED\"}");
            httpResponse.getWriter().flush();

            if (abuseDetectionService != null) {
                try { abuseDetectionService.recordViolation(clientIp, "RATE_LIMIT_EXCEEDED"); } catch (Exception ignored) {}
            }
            
            httpRequest.setAttribute("rateLimitExceeded", true);
            httpRequest.setAttribute("consumerKey", limitKey);
            return;
        }

        filterChain.doFilter(httpRequest, httpResponse);

    }
}
