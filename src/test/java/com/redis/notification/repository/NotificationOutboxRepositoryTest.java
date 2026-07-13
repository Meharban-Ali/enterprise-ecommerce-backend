package com.redis.notification.repository;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationOutboxRepositoryTest {

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void testOutboxRepositoryCrud() {
        NotificationOutbox record = NotificationOutbox.builder()
                .eventId(UUID.randomUUID())
                .eventType("WelcomeEvent")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();

        record = outboxRepository.save(record);
        assertNotNull(record.getId());

        List<NotificationOutbox> pending = outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.PENDING);
        assertFalse(pending.isEmpty());
        assertEquals(1, pending.size());

        int claimed = outboxRepository.claimEvent(record.getId());
        assertEquals(1, claimed);

        long pendingCount = outboxRepository.countByStatus(OutboxStatus.PENDING);
        assertEquals(0, pendingCount);

        long processingCount = outboxRepository.countByStatus(OutboxStatus.PROCESSING);
        assertEquals(1, processingCount);
    }
}
