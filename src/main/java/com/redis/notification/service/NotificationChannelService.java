package com.redis.notification.service;

import com.redis.notification.entity.Notification;
import com.redis.notification.entity.NotificationChannel;

public interface NotificationChannelService {
    void send(Notification notification);
    boolean supports(NotificationChannel channel);
}
