package com.redis.audit.event;

import com.redis.user.entity.User;

import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void publish(Long userId, String email, AuditActionType actionType, AuditStatus status,
                        ResourceType resourceType, String resourceId, String description) {
        publish(userId, email, actionType, status, resourceType, resourceId, description, null, null, null);
    }

    public void publish(Long userId, String email, AuditActionType actionType, AuditStatus status,
                        ResourceType resourceType, String resourceId, String description,
                        Integer httpStatus, Long executionTimeMs, Integer entityVersion) {
        String eventId = UUID.randomUUID().toString();
        String correlationId = resolveCorrelationId();
        String requestId = resolveRequestId();

        String ipAddress = "SYSTEM";
        String userAgent = "SYSTEM";
        String requestUri = "SYSTEM";
        String httpMethod = "SYSTEM";
        String actorType = "SYSTEM";
        String roleName = "SYSTEM";
        String sessionId = "SYSTEM";
        String authenticationMethod = "SYSTEM";
        String clientType = "SYSTEM";

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            if (request != null) {
                ipAddress = request.getRemoteAddr();
                userAgent = request.getHeader("User-Agent");
                requestUri = request.getRequestURI();
                httpMethod = request.getMethod();

                String ua = userAgent != null ? userAgent.toLowerCase() : "";
                if (ua.contains("android")) {
                    clientType = "ANDROID";
                } else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
                    clientType = "IOS";
                } else if (ua.contains("mozilla") || ua.contains("chrome") || ua.contains("safari") || ua.contains("firefox")) {
                    clientType = "WEB";
                } else {
                    clientType = "API";
                }

                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    authenticationMethod = "JWT";
                } else {
                    authenticationMethod = "SESSION";
                }

                if (request.getSession(false) != null) {
                    sessionId = request.getSession(false).getId();
                }
            }
        }

        String sanitizedDescription = maskSensitiveData(description);
        String sanitizedResourceId = maskSensitiveData(resourceId);

        AuditEvent event = new AuditEvent(
                this, eventId, correlationId, requestId,
                userId, email, actionType, status,
                resourceType, sanitizedResourceId, sanitizedDescription,
                ipAddress, userAgent, requestUri, httpMethod,
                actorType, roleName, sessionId, authenticationMethod,
                clientType, httpStatus, executionTimeMs, entityVersion
        );
        eventPublisher.publishEvent(event);
    }

    private String resolveCorrelationId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            if (request != null) {
                String header = request.getHeader("X-Correlation-ID");
                if (header != null && !header.isBlank()) {
                    return header;
                }
            }
        }
        String mdcVal = MDC.get("CorrelationId");
        if (mdcVal != null && !mdcVal.isBlank()) {
            return mdcVal;
        }
        return UUID.randomUUID().toString();
    }

    private String resolveRequestId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            if (request != null) {
                String header = request.getHeader("X-Request-ID");
                if (header != null && !header.isBlank()) {
                    return header;
                }
            }
        }
        return UUID.randomUUID().toString();
    }

    private String maskSensitiveData(String input) {
        if (input == null) return null;
        return input.replaceAll("(?i)(password|token|jwt|otp|cvv|card|secret)\\s*=\\s*[^\\s&]+", "$1=***");
    }
}
