package com.redis.payment.event;

import com.redis.notification.event.NotificationEvent;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationType;

public class PaymentNotificationEvent extends NotificationEvent {

    public PaymentNotificationEvent(Object source, Long userId, String title, String message,
                                    NotificationChannel channel, NotificationPriority priority) {
        super(source, userId, title, message, channel, priority, NotificationType.PAYMENT);
    }
}
