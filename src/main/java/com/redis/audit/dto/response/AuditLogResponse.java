package com.redis.audit.dto.response;

import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String eventId;
    private String correlationId;
    private String requestId;
    private Long userId;
    private String email;
    private AuditActionType actionType;
    private AuditStatus status;
    private ResourceType resourceType;
    private String resourceId;
    private String description;
    private String ipAddress;
    private String userAgent;
    private String requestUri;
    private String httpMethod;
    private String actorType;
    private String roleName;
    private String sessionId;
    private String authenticationMethod;
    private String clientType;
    private Integer httpStatus;
    private Long executionTimeMs;
    private Integer entityVersion;
    private LocalDateTime createdAt;
}
