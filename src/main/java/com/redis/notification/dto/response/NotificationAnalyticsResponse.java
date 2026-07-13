package com.redis.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationAnalyticsResponse {
    private long totalNotifications;
    private long totalSent;
    private long totalFailed;
    private long totalRetrying;
    private long totalDeadLetters;
    private double retrySuccessRate;
    private double deliverySuccessRate;
    private double averageRetryCount;
    private double averageDeliveryTimeMillis;
    private double averageResolutionTimeMillis;
}
