package com.redis.notification.service;

import com.redis.notification.entity.Notification;
import com.redis.notification.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class InAppNotificationService implements NotificationChannelService {

    @Override
    public void send(Notification notification) {
        log.info("Sending In-App notification for user: {}. Title: {}, Body: {}",
                notification.getUser().getUsername(), notification.getTitle(), notification.getMessage());
        // In-App notifications live purely in the database.
        // No external system execution is required, so this is a successful no-op.
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.IN_APP;
    }
}
