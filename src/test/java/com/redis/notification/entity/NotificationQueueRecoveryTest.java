package com.redis.notification.entity;

import com.redis.notification.service.NotificationRetryService;
import com.redis.notification.service.NotificationQueueService;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.cart.repository.CartRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationQueueRecoveryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private NotificationRetryService retryService;

    @Autowired
    private NotificationQueueService queueService;

    @Autowired
    private NotificationProperties properties;

    @MockBean
    private MailClient mailClient;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("recoveryuser")
                .email("recovery@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testFailureRequeueRecovery() {
        boolean oldQueue = properties.isQueueEnabled();
        properties.setQueueEnabled(true);

        Notification n = Notification.builder()
                .user(testUser)
                .title("Fail")
                .message("body")
                .type(NotificationType.AUTH)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.PENDING)
                .build();
        n = notificationRepository.saveAndFlush(n);

        retryService.scheduleRetry(n, new RuntimeException("Network timeout"));

        Notification updated = notificationRepository.findById(n.getId()).orElse(null);
        assertNotNull(updated);
        assertEquals(NotificationStatus.FAILED, updated.getStatus());
        assertEquals(1, updated.getRetryCount());

        retryService.executeRetry(updated.getId());

        Notification retrying = notificationRepository.findById(n.getId()).orElse(null);
        assertNotNull(retrying);
        assertEquals(NotificationStatus.RETRYING, retrying.getStatus());

        Long queuedId = queueService.dequeue();
        assertEquals(n.getId(), queuedId);

        properties.setQueueEnabled(oldQueue);
    }
}
