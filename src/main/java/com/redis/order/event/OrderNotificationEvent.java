package com.redis.order.event;

import com.redis.notification.event.NotificationEvent;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationType;

public class OrderNotificationEvent extends NotificationEvent {

    public OrderNotificationEvent(Object source, Long userId, String title, String message,
                                  NotificationChannel channel, NotificationPriority priority) {
        super(source, userId, title, message, channel, priority, NotificationType.ORDER);
    }
}
