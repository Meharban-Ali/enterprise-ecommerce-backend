package com.redis.notification.service;

import com.redis.notification.entity.Notification;
import java.time.LocalDateTime;

public interface NotificationRetryService {
    void executeRetry(Long notificationId);
    void scheduleRetry(Notification notification, Throwable error);
    void moveToDeadLetter(Notification notification);
    LocalDateTime calculateNextRetry(int retryCount);
    boolean shouldRetry(Notification notification);
}
