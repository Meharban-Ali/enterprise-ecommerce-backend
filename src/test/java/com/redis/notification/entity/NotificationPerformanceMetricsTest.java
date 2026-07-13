package com.redis.notification.entity;

import com.redis.notification.service.NotificationAnalyticsService;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.dto.response.NotificationAnalyticsResponse;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.cart.repository.CartRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationPerformanceMetricsTest {

    @Autowired
    private NotificationAnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    @MockBean
    private MailClient mailClient;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("perfuser")
                .email("perf@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    @Transactional
    void testPerformanceMetricsTiming() {
        Notification n = Notification.builder()
                .user(testUser)
                .title("Perf")
                .message("body")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.SENT)
                .build();
        n = notificationRepository.saveAndFlush(n);

        LocalDateTime now = LocalDateTime.now();
        entityManager.createNativeQuery("UPDATE notifications SET created_at = ?, delivered_at = ?, resolved_at = ? WHERE id = ?")
                .setParameter(1, now.minusSeconds(10))
                .setParameter(2, now)
                .setParameter(3, now)
                .setParameter(4, n.getId())
                .executeUpdate();
        entityManager.clear();

        NotificationAnalyticsResponse summary = analyticsService.getOverallAnalytics(null, null);
        assertTrue(summary.getAverageDeliveryTimeMillis() >= 9000.0);
        assertTrue(summary.getAverageResolutionTimeMillis() >= 9000.0);
    }
}
