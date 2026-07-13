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
public class InAppNotificationServiceTest {

    @InjectMocks
    private InAppNotificationService inAppNotificationService;

    @Test
    void testSupportsInAppChannelOnly() {
        assertTrue(inAppNotificationService.supports(NotificationChannel.IN_APP));
    }

    @Test
    void testSendMockInAppExecution() {
        User user = User.builder().username("john").build();
        Notification notification = Notification.builder()
                .user(user)
                .title("Welcome to app")
                .message("Enjoy your dashboard")
                .channel(NotificationChannel.IN_APP)
                .build();

        assertDoesNotThrow(() -> inAppNotificationService.send(notification));
    }
}
