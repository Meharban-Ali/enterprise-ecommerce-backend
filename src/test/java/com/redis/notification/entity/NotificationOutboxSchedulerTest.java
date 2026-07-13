package com.redis.notification.entity;

import com.redis.notification.service.NotificationOutboxService;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.event.AuthenticationNotificationEvent;
import com.redis.notification.entity.NotificationOutbox;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.common.entity.OutboxStatus;
import com.redis.notification.repository.NotificationOutboxRepository;
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
public class NotificationOutboxSchedulerTest {

    @Autowired
    private NotificationOutboxScheduler outboxScheduler;

    @Autowired
    private NotificationOutboxService outboxService;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private NotificationProperties properties;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void testSchedulerProcessing() throws Exception {
        boolean oldOutbox = properties.isOutboxEnabled();
        properties.setOutboxEnabled(true);

        AuthenticationNotificationEvent event = new AuthenticationNotificationEvent(
                this, 123L, "Sched Welcome", "Hello Sched", NotificationChannel.EMAIL, NotificationPriority.HIGH
        );
        outboxService.saveEvent(event);

        outboxScheduler.processOutbox();

        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        assertEquals(0, pending);

        long published = outboxRepository.countByStatus(OutboxStatus.PUBLISHED);
        assertEquals(1, published);

        properties.setOutboxEnabled(oldOutbox);
    }
}
