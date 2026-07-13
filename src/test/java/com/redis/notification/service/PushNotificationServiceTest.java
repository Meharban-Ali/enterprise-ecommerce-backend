package com.redis.notification.service;

import com.redis.notification.entity.NotificationChannel;

import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class PushNotificationServiceTest {

    @InjectMocks
    private PushNotificationService pushNotificationService;

    @Test
    void testSupportsPushChannelOnly() {
        assertTrue(pushNotificationService.supports(NotificationChannel.PUSH));
    }

    @Test
    void testSendMockPushExecution() {
        User user = User.builder().username("john").build();
        Notification notification = Notification.builder()
                .user(user)
                .title("New Alert")
                .message("Price drop on item A")
                .channel(NotificationChannel.PUSH)
                .build();

        assertDoesNotThrow(() -> pushNotificationService.send(notification));
    }
}
