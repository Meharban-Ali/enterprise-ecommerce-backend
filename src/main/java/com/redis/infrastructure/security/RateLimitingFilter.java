package com.redis.infrastructure.security;

import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.audit.entity.AuditStatus;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.audit.entity.AuditActionType;
import com.redis.security.service.ApiAbuseDetectionService;
import com.redis.infrastructure.governance.service.ApiGovernanceService;
import com.redis.security.service.RateLimitService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Slf4j
@Component
public class RateLimitingFilter implements Filter {

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
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        boolean isEnabled = properties != null && properties.isRateLimitEnabled();
        if (!isEnabled || rateLimitService == null || !(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = httpRequest.getRemoteAddr();
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

        if (properties.isEndpointRateLimitEnabled() && properties.getEndpointRateLimits() != null) {
            String lookupKey = httpRequest.getMethod() + " " + httpRequest.getRequestURI();
            Integer override = properties.getEndpointRateLimits().get(lookupKey);
            if (override == null) {
                override = properties.getEndpointRateLimits().get(httpRequest.getRequestURI());
            }
            if (override != null) {
                limit = override;
                limitKey = limitKey + ":" + httpRequest.getMethod() + ":" + httpRequest.getRequestURI();
            }
        }

        if (limit == Integer.MAX_VALUE) {
            chain.doFilter(request, response);
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
            
            request.setAttribute("rateLimitExceeded", true);
            request.setAttribute("consumerKey", limitKey);

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

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
