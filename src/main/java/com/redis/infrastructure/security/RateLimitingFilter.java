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

        // Harden IP extraction against header spoofing:
        // 1. Prefer X-Real-IP (overwritten & enforced by Railway/Cloudflare edge proxies)
        String clientIp = httpRequest.getHeader("X-Real-IP");
        if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
            // 2. Fall back to X-Forwarded-For: read rightmost IP appended by trusted proxy
            String xff = httpRequest.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank() && !"unknown".equalsIgnoreCase(xff)) {
                String[] ips = xff.split(",");
                clientIp = ips[ips.length - 1].trim();
            }
        }
        if (clientIp == null || clientIp.isBlank() || "unknown".equalsIgnoreCase(clientIp)) {
            clientIp = httpRequest.getRemoteAddr();
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

        if (limit == Integer.MAX_VALUE) {
            filterChain.doFilter(httpRequest, httpResponse);
            return;
        }

        boolean allowed = rateLimitService.isAllowed(limitKey, limit, window);
        if (!allowed) {
            int retryAfter = rateLimitService.getRetryAfterSeconds(limitKey, limit, window);
            httpResponse.setStatus(429);
            httpResponse.setHeader("Retry-After", String.valueOf(retryAfter));
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"message\":\"Too Many Requests — please try again later.\",\"code\":\"RATE_LIMIT_EXCEEDED\"}");
            httpResponse.getWriter().flush();

            if (abuseDetectionService != null) {
                abuseDetectionService.recordViolation(clientIp, "RATE_LIMIT_EXCEEDED");
            }
            
            httpRequest.setAttribute("rateLimitExceeded", true);
            httpRequest.setAttribute("consumerKey", limitKey);

            if (auditEventPublisher != null) {
                auditEventPublisher.publish(
                        null,
                        auth != null ? auth.getName() : "anonymous@ecommerce.com",
                        AuditActionType.RATE_LIMIT_EXCEEDED,
                        com.redis.audit.entity.AuditStatus.FAILED,
                        com.redis.common.entity.ResourceType.SYSTEM,
                        "0",
                        "Rate limit exceeded for client: " + limitKey
                );
            }
            return;
        }

        filterChain.doFilter(httpRequest, httpResponse);
    }
}
