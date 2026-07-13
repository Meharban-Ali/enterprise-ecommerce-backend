package com.redis.notification.service;

import com.redis.infrastructure.config.NotificationProperties;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.repository.NotificationRateLimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationRateLimitServiceTest {

    @Autowired
    private NotificationRateLimitService rateLimitService;

    @Autowired
    private NotificationRateLimitRepository rateLimitRepository;

    @Autowired
    private com.redis.infrastructure.config.NotificationProperties properties;

    @BeforeEach
    void setUp() {
        rateLimitRepository.deleteAll();
    }

    @Test
    void testRateLimiting() {
        properties.setRateLimitingEnabled(true);
        properties.setDefaultRateLimitPerHour(2);

        Long userId = 9999L;
        assertTrue(rateLimitService.shouldSend(userId, NotificationType.AUTH, NotificationChannel.EMAIL));
        assertTrue(rateLimitService.shouldSend(userId, NotificationType.AUTH, NotificationChannel.EMAIL));
        assertFalse(rateLimitService.shouldSend(userId, NotificationType.AUTH, NotificationChannel.EMAIL));

        rateLimitService.resetLimits(userId);
        assertTrue(rateLimitService.shouldSend(userId, NotificationType.AUTH, NotificationChannel.EMAIL));
    }
}
