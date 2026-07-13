package com.redis.notification.service;

import com.redis.notification.entity.Notification;
import com.redis.notification.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PushNotificationService implements NotificationChannelService {

    @Override
    public void send(Notification notification) {
        log.info("Sending Mobile Push notification to mock provider for user: {}. Title: {}, Body: {}",
                notification.getUser().getUsername(), notification.getTitle(), notification.getMessage());
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.PUSH;
    }
}
