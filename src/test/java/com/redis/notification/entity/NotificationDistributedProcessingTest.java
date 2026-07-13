package com.redis.notification.entity;

import com.redis.common.entity.OutboxStatus;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.entity.Notification;
import com.redis.notification.entity.NotificationOutbox;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.cart.repository.CartRepository;
import com.redis.notification.repository.NotificationOutboxRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationDistributedProcessingTest {

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("distuser")
                .email("dist@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testConcurrentOutboxClaims() throws Exception {
        NotificationOutbox outbox = NotificationOutbox.builder()
                .eventId(UUID.randomUUID())
                .eventType("WelcomeEvent")
                .payload("{}")
                .status(OutboxStatus.PENDING)
                .retryCount(0)
                .build();
        outbox = outboxRepository.saveAndFlush(outbox);

        final Long outboxId = outbox.getId();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successClaims = new AtomicInteger(0);

        List<Future<Integer>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> outboxRepository.claimEvent(outboxId)));
        }

        for (Future<Integer> f : futures) {
            successClaims.addAndGet(f.get());
        }
        executor.shutdown();

        assertEquals(1, successClaims.get());
    }

    @Test
    void testConcurrentNotificationClaims() throws Exception {
        Notification n = Notification.builder()
                .user(testUser)
                .title("Concurrent")
                .message("body")
                .type(NotificationType.AUTH)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.PENDING)
                .build();
        n = notificationRepository.saveAndFlush(n);

        final Long notificationId = n.getId();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        AtomicInteger successClaims = new AtomicInteger(0);

        List<Future<Integer>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> notificationRepository.transitionToDelivering(notificationId)));
        }

        for (Future<Integer> f : futures) {
            successClaims.addAndGet(f.get());
        }
        executor.shutdown();

        assertEquals(1, successClaims.get());
    }
}
