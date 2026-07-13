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
public class SmsNotificationServiceTest {

    @InjectMocks
    private SmsNotificationService smsNotificationService;

    @Test
    void testSupportsSmsChannelOnly() {
        assertTrue(smsNotificationService.supports(NotificationChannel.SMS));
    }

    @Test
    void testSendMockSmsExecution() {
        User user = User.builder().username("john").build();
        Notification notification = Notification.builder()
                .user(user)
                .title("SMS Code")
                .message("Your OTP is 123456")
                .channel(NotificationChannel.SMS)
                .build();

        assertDoesNotThrow(() -> smsNotificationService.send(notification));
    }
}
