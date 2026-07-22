package com.redis.webhook.entity;

import com.redis.notification.entity.NotificationChannel;
import com.redis.common.entity.CircuitState;
import com.redis.notification.entity.NotificationPriority;
import com.redis.common.entity.IntegrationEventType;
import com.redis.monitoring.entity.AlertSeverity;

import com.redis.common.base.AuditableEntity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(
    name = "webhook_endpoints",
    indexes = {
        @Index(name = "idx_webhook_endpoints_enabled", columnList = "enabled"),
        @Index(name = "idx_webhook_endpoints_target_url", columnList = "target_url")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEndpoint extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    @Column(name = "name", nullable = false)
    private String name;

    @NotBlank(message = "Target URL is required")
    @Pattern(regexp = "^https?://.*$", message = "Target URL must be a valid HTTP or HTTPS URL")
    @Column(name = "target_url", nullable = false, length = 1000)
    private String targetUrl;

    @NotBlank(message = "Secret key is required")
    @Column(name = "secret_key", nullable = false, length = 255)
    private String secretKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Min(value = 1000, message = "Timeout must be at least 1000 ms")
    @Column(name = "timeout_ms", nullable = false)
    private int timeoutMs;

    @Column(name = "retry_enabled", nullable = false)
    private boolean retryEnabled;

    @Min(value = 0)
    @Max(value = 10)
    @Column(name = "max_retry_count", nullable = false)
    private int maxRetryCount;

    @ElementCollection(targetClass = IntegrationEventType.class, fetch = FetchType.EAGER)
    @CollectionTable(
        name = "webhook_endpoint_events",
        joinColumns = @JoinColumn(name = "endpoint_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private Set<IntegrationEventType> subscribedEvents;

    @NotBlank(message = "Webhook version is required")
    @Column(name = "webhook_version", nullable = false, length = 20)
    private String webhookVersion;

    // Filtering Fields
    @Enumerated(EnumType.STRING)
    @Column(name = "filter_priority", length = 30)
    private NotificationPriority filterPriority;

    @Enumerated(EnumType.STRING)
    @Column(name = "filter_channel", length = 30)
    private NotificationChannel filterChannel;

    @Enumerated(EnumType.STRING)
    @Column(name = "filter_severity", length = 30)
    private AlertSeverity filterSeverity;

    // Rate Limiting
    @Min(value = 1)
    @Column(name = "requests_per_minute")
    private Integer requestsPerMinute;

    @Min(value = 1)
    @Column(name = "requests_per_hour")
    private Integer requestsPerHour;

    // Batching
    @Column(name = "batch_enabled", nullable = false)
    private boolean batchEnabled;

    @Min(value = 1)
    @Column(name = "batch_size")
    private Integer batchSize;

    @Min(value = 1)
    @Column(name = "batch_interval_seconds")
    private Integer batchIntervalSeconds;

    @Column(name = "compression_enabled", nullable = false)
    private boolean compressionEnabled;

    // Circuit Breaker State
    @Enumerated(EnumType.STRING)
    @Column(name = "circuit_state", nullable = false, length = 30)
    private CircuitState circuitState;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @Column(name = "last_failure_time")
    private LocalDateTime lastFailureTime;

    

    

    
}