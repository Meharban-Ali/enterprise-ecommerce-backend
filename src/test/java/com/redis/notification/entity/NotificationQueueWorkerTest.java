package com.redis.notification.entity;

import com.redis.notification.service.NotificationQueueService;

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

@SpringBootTest(properties = {
        "app.notification.queue-enabled=true",
        "app.notification.worker-threads=2"
})
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationQueueWorkerTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private NotificationQueueService queueService;

    @MockBean
    private MailClient mailClient;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("workeruser")
                .email("worker@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testWorkerProcessing() throws Exception {
        Notification n = Notification.builder()
                .user(testUser)
                .title("Worker test")
                .message("body")
                .type(NotificationType.AUTH)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.HIGH)
                .status(NotificationStatus.PENDING)
                .build();
        n = notificationRepository.saveAndFlush(n);

        queueService.enqueue(n.getId());

        int elapsed = 0;
        Notification result = null;
        while (elapsed < 3000) {
            result = notificationRepository.findById(n.getId()).orElse(null);
            if (result != null && result.getStatus() == NotificationStatus.SENT) {
                break;
            }
            Thread.sleep(100);
            elapsed += 100;
        }

        assertNotNull(result);
        assertEquals(NotificationStatus.SENT, result.getStatus());
        assertNotNull(result.getDeliveredAt());
    }
}
