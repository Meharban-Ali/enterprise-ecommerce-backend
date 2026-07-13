package com.redis.notification.entity;

import com.redis.notification.service.NotificationRetryService;

import com.redis.infrastructure.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationRetryPolicyTest {

    @Autowired
    private NotificationRetryService retryService;

    @Test
    void testBackoffCalculations() {
        LocalDateTime now = LocalDateTime.now();

        // 1st retry: 1 minute delay
        LocalDateTime retry1 = retryService.calculateNextRetry(1);
        long delay1 = Duration.between(now, retry1).toMinutes();
        assertTrue(delay1 >= 0 && delay1 <= 1);

        // 2nd retry: 2 minutes delay
        LocalDateTime retry2 = retryService.calculateNextRetry(2);
        long delay2 = Duration.between(now, retry2).toMinutes();
        assertTrue(delay2 >= 1 && delay2 <= 2);

        // 3rd retry: 4 minutes delay
        LocalDateTime retry3 = retryService.calculateNextRetry(3);
        long delay3 = Duration.between(now, retry3).toMinutes();
        assertTrue(delay3 >= 3 && delay3 <= 4);
    }
}
