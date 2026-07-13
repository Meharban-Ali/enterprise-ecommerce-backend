package com.redis.notification.entity;

import com.redis.notification.dto.response.NotificationDashboardResponse;
import com.redis.user.entity.Role;
import com.redis.notification.service.NotificationAnalyticsService;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationDashboardTest {

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
                .username("dashuser")
                .email("dash@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testGetDashboardResponseLayout() {
        Notification n = Notification.builder()
                .user(testUser)
                .title("Dash")
                .message("Message")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.SENT)
                .build();
        notificationRepository.save(n);

        NotificationDashboardResponse response = analyticsService.getDashboard(null, null);
        assertNotNull(response.getOverallSummary());
        assertNotNull(response.getChannelStatistics());
        assertNotNull(response.getTypeStatistics());
        assertNotNull(response.getPriorityStatistics());
    }
}
