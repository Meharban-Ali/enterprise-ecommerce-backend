package com.redis.notification.entity;

import com.redis.notification.event.NotificationEventPublisher;
import com.redis.notification.event.AuthenticationNotificationEvent;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.entity.NotificationOutbox;
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

@SpringBootTest(properties = "app.notification.outbox-enabled=true")
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationOutboxIntegrationTest {

    @Autowired
    private NotificationEventPublisher eventPublisher;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private NotificationProperties properties;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void testEndToEndOutboxIntegration() {
        eventPublisher.publishWelcome(789L, "integration@example.com");

        List<NotificationOutbox> outboxList = outboxRepository.findAll();
        assertEquals(1, outboxList.size());
        
        NotificationOutbox outbox = outboxList.get(0);
        assertEquals(OutboxStatus.PENDING, outbox.getStatus());
        assertEquals("AuthenticationNotificationEvent", outbox.getEventType());
        assertTrue(outbox.getPayload().contains("Welcome to E-Commerce!"));
    }
}
