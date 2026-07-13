package com.redis.webhook.dto.response;

import com.redis.common.entity.IntegrationEventType;
import com.redis.webhook.entity.WebhookStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookDeliveryResponse {
    private Long id;
    private Long webhookEndpointId;
    private String webhookEndpointName;
    private String targetUrl;
    private IntegrationEventType eventType;
    private String payload;
    private String requestHeaders;
    private Integer responseStatus;
    private String responseBody;
    private WebhookStatus deliveryStatus;
    private int retryCount;
    private Long executionTimeMs;
    private String correlationId;
    private String idempotencyKey;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
}
