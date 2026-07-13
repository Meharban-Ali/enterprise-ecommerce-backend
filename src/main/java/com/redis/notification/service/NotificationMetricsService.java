package com.redis.notification.service;

import com.redis.notification.entity.NotificationChannel;
import java.util.Map;

public interface NotificationMetricsService {
    long totalSent();
    long totalFailed();
    long totalDeadLetters();
    double retrySuccessRate();
    double averageRetryCount();
    double averageDeliveryTime();
    double averageRetryResolutionTime();
    Map<NotificationChannel, Long> channelStatistics();
}
