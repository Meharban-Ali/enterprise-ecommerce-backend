package com.redis.notification.entity;

import com.redis.notification.service.RedisNotificationQueueService;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.infrastructure.config.TestRedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class RedisNotificationQueueTest {

    @Autowired
    private RedisNotificationQueueService redisQueueService;

    @Autowired
    private NotificationProperties properties;

    @Test
    void testRedisQueueOperations() {
        String testQueue = "notification:test-queue";
        
        while (redisQueueService.dequeue(testQueue) != null) {}

        redisQueueService.enqueue(testQueue, 789L);
        Long id = redisQueueService.dequeue(testQueue);
        assertEquals(789L, id);
        
        assertNull(redisQueueService.dequeue(testQueue));
    }
}
