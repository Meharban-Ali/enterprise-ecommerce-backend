package com.redis.notification.service;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.Role;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.MailClient;
import com.redis.notification.dto.response.NotificationAnalyticsResponse;
import com.redis.user.entity.User;

import com.redis.infrastructure.config.TestRedisConfig;
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

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationAnalyticsServiceTest {

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
                .username("analyticsuser")
                .email("analytics@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testGetOverallAnalytics() {
        Notification n = Notification.builder()
                .user(testUser)
                .title("T")
                .message("B")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.SENT)
                .build();
        notificationRepository.save(n);

        NotificationAnalyticsResponse summary = analyticsService.getOverallAnalytics(null, null);
        assertEquals(1, summary.getTotalNotifications());
        assertEquals(1, summary.getTotalSent());
    }
}
