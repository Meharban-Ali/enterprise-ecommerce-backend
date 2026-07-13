package com.redis.webhook.entity;

import com.redis.common.base.AuditableEntity;

import com.redis.common.entity.IntegrationEventType;
import com.redis.webhook.entity.WebhookStatus;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "webhook_deliveries",
    indexes = {
        @Index(name = "idx_webhook_deliveries_status", columnList = "delivery_status"),
        @Index(name = "idx_webhook_deliveries_event_type", columnList = "event_type"),
        @Index(name = "idx_webhook_deliveries_correlation", columnList = "correlation_id"),
        @Index(name = "idx_webhook_deliveries_idempotency", columnList = "idempotency_key"),
        @Index(name = "idx_webhook_deliveries_created_at", columnList = "created_at"),
        @Index(name = "idx_webhook_deliveries_aggregate", columnList = "aggregate_type, aggregate_id")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDelivery extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_endpoint_id", nullable = false)
    private WebhookEndpoint webhookEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private IntegrationEventType eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 30)
    private WebhookStatus deliveryStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    // Delivery ordering attributes
    @Column(name = "aggregate_type", length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}