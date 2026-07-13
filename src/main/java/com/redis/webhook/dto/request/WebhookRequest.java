package com.redis.webhook.dto.request;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.common.entity.IntegrationEventType;
import com.redis.monitoring.entity.AlertSeverity;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Target URL is required")
    @Pattern(regexp = "^https?://.*$", message = "Target URL must be a valid HTTP or HTTPS URL")
    private String targetUrl;

    @NotBlank(message = "Secret key is required")
    private String secretKey;

    private boolean enabled;

    @Min(value = 1000, message = "Timeout must be at least 1000 ms")
    private int timeoutMs;

    private boolean retryEnabled;

    @Min(value = 0)
    @Max(value = 10)
    private int maxRetryCount;

    @NotEmpty(message = "At least one subscribed event type is required")
    private Set<IntegrationEventType> subscribedEvents;

    @NotBlank(message = "Webhook version is required")
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
}
