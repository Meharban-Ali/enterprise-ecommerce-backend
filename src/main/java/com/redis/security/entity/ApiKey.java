package com.redis.security.entity;

import com.redis.common.base.AuditableEntity;

import com.redis.common.entity.Permission;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "api_keys", indexes = {
    @Index(name = "idx_api_key_hash", columnList = "keyHash"),
    @Index(name = "idx_api_key_rotation_hash", columnList = "rotationKeyHash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ApiKey extends AuditableEntity {

    

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String keyHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "api_key_permissions", joinColumns = @JoinColumn(name = "api_key_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    private String rotationKeyHash;

    private LocalDateTime rotationExpiresAt;

    

    

    // --- Usage Analytics properties ---
    @Column(nullable = false)
    @Builder.Default
    private Long totalRequests = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long failedRequests = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Double averageLatencyMs = 0.0;

    private LocalDateTime lastUsedTime;

    private String lastIpAddress;

    @Column(nullable = false)
    @Builder.Default
    private Long rateLimitViolations = 0L;

    private LocalDateTime lastSuccessfulAuthentication;

    @Column(nullable = false)
    @Builder.Default
    private Integer failedAuthenticationCount = 0;

    private LocalDateTime lockUntil;

    @Column(nullable = false)
    @Builder.Default
    private Long requestsPerHour = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Long requestsPerDay = 0L;

    @Column(nullable = false)
    @Builder.Default
    private Double errorRate = 0.0;

    @Column(nullable = false)
    @Builder.Default
    private Double successRate = 100.0;

    @Column(nullable = false)
    @Builder.Default
    private Integer peakUsageHour = 0;

    @Column(length = 2048)
    private String topEndpointsJson;
}