package com.redis.notification.service;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;

public interface NotificationRateLimitService {

    boolean shouldSend(Long userId, NotificationType type, NotificationChannel channel);

    void incrementCounter(Long userId, NotificationType type, NotificationChannel channel);

    long calculateRemainingWindow(Long userId, NotificationType type, NotificationChannel channel, String windowType);

    void resetLimits(Long userId);
}
