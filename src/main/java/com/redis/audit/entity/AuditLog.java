package com.redis.audit.entity;

import com.redis.common.base.AuditableEntity;

import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user", columnList = "user_id"),
        @Index(name = "idx_audit_action", columnList = "action_type"),
        @Index(name = "idx_audit_created", columnList = "created_at"),
        @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id"),
        @Index(name = "idx_audit_status", columnList = "status"),
        @Index(name = "idx_audit_correlation", columnList = "correlation_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder
@org.hibernate.annotations.Immutable
public class AuditLog extends AuditableEntity {

    

    @Column(name = "event_id", nullable = false, unique = true, length = 50)
    private String eventId;

    @Column(name = "correlation_id", nullable = false, length = 50)
    private String correlationId;

    @Column(name = "request_id", length = 50)
    private String requestId;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AuditActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuditStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 30)
    private ResourceType resourceType;

    @Column(name = "resource_id", length = 50)
    private String resourceId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "request_uri", length = 255)
    private String requestUri;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(name = "actor_type", length = 20)
    private String actorType; // USER, ADMIN, SYSTEM

    @Column(name = "role_name", length = 50)
    private String roleName;

    @Column(name = "session_id", length = 50)
    private String sessionId;

    @Column(name = "authentication_method", length = 20)
    private String authenticationMethod; // JWT, GOOGLE, GITHUB, SYSTEM

    @Column(name = "client_type", length = 20)
    private String clientType; // WEB, ANDROID, IOS, API, SYSTEM

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "entity_version")
    private Integer entityVersion;

    
}