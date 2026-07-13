package com.redis.audit.event;

import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class AuditEvent extends ApplicationEvent {

    private final String eventId;
    private final String correlationId;
    private final String requestId;
    private final Long userId;
    private final String email;
    private final AuditActionType actionType;
    private final AuditStatus status;
    private final ResourceType resourceType;
    private final String resourceId;
    private final String description;
    private final String ipAddress;
    private final String userAgent;
    private final String requestUri;
    private final String httpMethod;
    private final String actorType;
    private final String roleName;
    private final String sessionId;
    private final String authenticationMethod;
    private final String clientType;
    private final Integer httpStatus;
    private final Long executionTimeMs;
    private final Integer entityVersion;

    public AuditEvent(Object source, String eventId, String correlationId, String requestId,
                      Long userId, String email, AuditActionType actionType, AuditStatus status,
                      ResourceType resourceType, String resourceId, String description,
                      String ipAddress, String userAgent, String requestUri, String httpMethod,
                      String actorType, String roleName, String sessionId, String authenticationMethod,
                      String clientType, Integer httpStatus, Long executionTimeMs, Integer entityVersion) {
        super(source);
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.requestId = requestId;
        this.userId = userId;
        this.email = email;
        this.actionType = actionType;
        this.status = status;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.description = description;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.requestUri = requestUri;
        this.httpMethod = httpMethod;
        this.actorType = actorType;
        this.roleName = roleName;
        this.sessionId = sessionId;
        this.authenticationMethod = authenticationMethod;
        this.clientType = clientType;
        this.httpStatus = httpStatus;
        this.executionTimeMs = executionTimeMs;
        this.entityVersion = entityVersion;
    }
}
