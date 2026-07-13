package com.redis.notification.event;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationType;

public class AuthenticationNotificationEvent extends NotificationEvent {

    public AuthenticationNotificationEvent(Object source, Long userId, String title, String message,
                                           NotificationChannel channel, NotificationPriority priority) {
        super(source, userId, title, message, channel, priority, NotificationType.AUTH);
    }
}
