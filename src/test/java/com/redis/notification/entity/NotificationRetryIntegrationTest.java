package com.redis.notification.entity;

import com.redis.notification.service.NotificationRetryService;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationRetryIntegrationTest {

    @Autowired
    private NotificationRetryService retryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MailClient mailClient;

    private User testUser;
    private Notification notification;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("intretryuser")
                .email("intretry@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);

        notification = Notification.builder()
                .user(testUser)
                .title("Initial Failed Session")
                .message("body")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.FAILED)
                .build();
        notification = notificationRepository.save(notification);
    }

    @Test
    void testSuccessfulRetryTransition() {
        retryService.executeRetry(notification.getId());

        Notification updated = notificationRepository.findById(notification.getId()).orElseThrow();
        assertEquals(NotificationStatus.SENT, updated.getStatus());
        assertNotNull(updated.getResolvedAt());
    }
}
