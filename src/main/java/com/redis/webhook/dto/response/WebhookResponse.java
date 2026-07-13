package com.redis.webhook.dto.response;

import com.redis.notification.entity.NotificationPriority;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.common.entity.IntegrationEventType;
import com.redis.notification.entity.NotificationChannel;
import com.redis.common.entity.CircuitState;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookResponse {
    private Long id;
    private String name;
    private String targetUrl;
    private String secretKey; // Will be masked as ********************
    private boolean enabled;
    private int timeoutMs;
    private boolean retryEnabled;
    private int maxRetryCount;
    private Set<IntegrationEventType> subscribedEvents;
    private String webhookVersion;
    
    // Filters
    private NotificationPriority filterPriority;
    private NotificationChannel filterChannel;
    private AlertSeverity filterSeverity;

    private Integer requestsPerMinute;
    private Integer requestsPerHour;
    private boolean batchEnabled;
    private Integer batchSize;
    private Integer batchIntervalSeconds;
    private boolean compressionEnabled;
    private CircuitState circuitState;
    private int consecutiveFailures;
    private LocalDateTime lastFailureTime;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
