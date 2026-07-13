package com.redis.notification.service;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationOutboxServiceTest {

    @Autowired
    private NotificationOutboxService outboxService;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void testSaveAndProcessOutbox() {
        AuthenticationNotificationEvent event = new AuthenticationNotificationEvent(
                this, 123L, "Welcome", "Hello", NotificationChannel.EMAIL, NotificationPriority.HIGH
        );

        outboxService.saveEvent(event);

        List<NotificationOutbox> pending = outboxService.getPendingEvents(10);
        assertEquals(1, pending.size());
        assertEquals("AuthenticationNotificationEvent", pending.get(0).getEventType());

        outboxService.processOutboxBatch(pending);

        List<NotificationOutbox> emptyPending = outboxService.getPendingEvents(10);
        assertEquals(0, emptyPending.size());

        NotificationOutbox processed = outboxRepository.findAll().get(0);
        assertEquals(OutboxStatus.PUBLISHED, processed.getStatus());
        assertNotNull(processed.getProcessedAt());
    }
}
