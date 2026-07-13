package com.redis.notification.service;

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
public class NotificationQueueServiceTest {

    @Autowired
    private NotificationQueueService queueService;

    @Test
    void testEnqueueDequeueAckRequeue() {
        while (queueService.dequeue() != null) {}

        queueService.enqueue(456L);
        Long id = queueService.dequeue();
        assertEquals(456L, id);

        queueService.requeue(456L);
        id = queueService.dequeue();
        assertEquals(456L, id);

        queueService.acknowledge(id);
        assertNull(queueService.dequeue());
    }
}
