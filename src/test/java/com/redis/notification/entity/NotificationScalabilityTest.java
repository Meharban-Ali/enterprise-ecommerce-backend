package com.redis.notification.entity;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationScalabilityTest {

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
    }

    @Test
    void testOutboxScalability() throws Exception {
        int recordCount = 200;
        List<NotificationOutbox> records = new ArrayList<>();
        for (int i = 0; i < recordCount; i++) {
            records.add(NotificationOutbox.builder()
                    .eventId(UUID.randomUUID())
                    .eventType("ScalabilityEvent")
                    .payload("{}")
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build());
        }
        outboxRepository.saveAllAndFlush(records);

        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            futures.add(executor.submit(() -> {
                while (true) {
                    List<NotificationOutbox> pending = outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.PENDING);
                    if (pending.isEmpty()) {
                        break;
                    }
                    for (NotificationOutbox record : pending) {
                        outboxRepository.claimEvent(record.getId());
                    }
                }
                return null;
            }));
        }

        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdown();

        long pending = outboxRepository.countByStatus(OutboxStatus.PENDING);
        long processing = outboxRepository.countByStatus(OutboxStatus.PROCESSING);
        long published = outboxRepository.countByStatus(OutboxStatus.PUBLISHED);
        long failed = outboxRepository.countByStatus(OutboxStatus.FAILED);
        
        assertEquals(0, pending);
        assertEquals(recordCount, processing + published + failed);
    }
}
