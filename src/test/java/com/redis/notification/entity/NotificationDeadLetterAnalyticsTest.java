package com.redis.notification.entity;

import com.redis.notification.service.NotificationAnalyticsService;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.dto.response.NotificationResponse;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationDeadLetterAnalyticsTest {

    @Autowired
    private NotificationAnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MailClient mailClient;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("dlqanuser")
                .email("dlqan@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testDeadLetterSummary() {
        Notification dlq = Notification.builder()
                .user(testUser)
                .title("DLQ Alert")
                .message("body")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.DEAD_LETTER)
                .build();
        notificationRepository.save(dlq);

        List<NotificationResponse> list = analyticsService.getDeadLetterSummary(null, null, 0, 10);
        assertEquals(1, list.size());
        assertEquals("DLQ Alert", list.get(0).getTitle());
    }
}
