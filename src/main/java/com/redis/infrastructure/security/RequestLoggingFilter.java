package com.redis.infrastructure.security;

import com.redis.observability.entity.StructuredLogger;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.infrastructure.security.MultiReadHttpServletRequestWrapper;
import com.redis.audit.entity.AuditStatus;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.audit.entity.AuditActionType;
import com.redis.infrastructure.governance.service.ApiGovernanceService;
import com.redis.common.util.SensitiveDataMasker;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;
import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLoggingFilter implements Filter {

    @Autowired(required = false)
    private ApiSecurityProperties properties = new ApiSecurityProperties();

    @Autowired(required = false)
    private ApiGovernanceService governanceService;

    @Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    public RequestLoggingFilter() {}

    public RequestLoggingFilter(ApiSecurityProperties properties, ApiGovernanceService governanceService, com.redis.audit.event.AuditEventPublisher auditEventPublisher) {
        this.properties = properties;
        this.governanceService = governanceService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        MultiReadHttpServletRequestWrapper requestWrapper = new MultiReadHttpServletRequestWrapper(httpRequest);
        
        // Payload size protection
        boolean payloadProtectionEnabled = properties != null && properties.isPayloadProtectionEnabled();
        if (payloadProtectionEnabled) {
            long maxLimit = properties.getMaxRequestBodySize();
            long reqSize = requestWrapper.getBody().length;
            if (reqSize > maxLimit) {
                log.warn("Payload size validation failed: {} bytes exceeds limit of {} bytes", reqSize, maxLimit);
                httpResponse.setStatus(413);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"message\":\"Payload Too Large\",\"code\":\"PAYLOAD_TOO_LARGE\"}");
                httpResponse.getWriter().flush();

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auditEventPublisher != null) {
                    auditEventPublisher.publish(
                            null,
                            auth != null ? auth.getName() : "anonymous@ecommerce.com",
                            AuditActionType.PAYLOAD_SIZE_EXCEEDED,
                            com.redis.audit.entity.AuditStatus.FAILED,
                            com.redis.common.entity.ResourceType.SYSTEM,
                            "0",
                            String.format("Payload size %d bytes exceeded configured limit of %d bytes", reqSize, maxLimit)
                    );
                }
                return;
            }
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
        long startTime = System.currentTimeMillis();
        try {
            chain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            String username = "anonymous";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                username = auth.getName();
            }

            int reqSize = requestWrapper.getBody().length;
            int resSize = responseWrapper.getContentSize();
            String uri = httpRequest.getRequestURI();
            String method = httpRequest.getMethod();
            int status = httpResponse.getStatus();
            String correlationId = MDC.get("correlationId");

            java.util.Map<String, Object> extraFields = new java.util.LinkedHashMap<>();
            extraFields.put("method", method);
            extraFields.put("uri", uri);
            extraFields.put("status", status);
            extraFields.put("durationMs", duration);
            extraFields.put("requestSize", reqSize);
            extraFields.put("responseSize", resSize);
            extraFields.put("user", username);
            
            com.redis.observability.entity.StructuredLogger.info("API_REQUEST", extraFields);

            // Record Metrics in ApiGovernanceService
            if (governanceService != null) {
                Boolean isHit = Boolean.TRUE.equals(requestWrapper.getAttribute("idempotencyHit"));
                Boolean isReplay = Boolean.TRUE.equals(requestWrapper.getAttribute("idempotencyReplay"));
                Boolean isRateLimit = Boolean.TRUE.equals(requestWrapper.getAttribute("rateLimitExceeded"));
                Boolean isValidation = Boolean.TRUE.equals(requestWrapper.getAttribute("validationError"));
                
                String apiKeyHeader = httpRequest.getHeader("X-API-Key");
                String consumer = (String) requestWrapper.getAttribute("consumerKey");
                if (consumer == null) {
                    consumer = apiKeyHeader != null ? "key:" + apiKeyHeader : username;
                }

                governanceService.recordRequest(
                        uri, method, duration, status, reqSize, resSize,
                        consumer, isHit, isReplay, isRateLimit, isValidation
                );
            }

            responseWrapper.copyBodyToResponse();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
