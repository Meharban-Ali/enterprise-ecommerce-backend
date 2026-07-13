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

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationRetrySchedulerTest {

    @Autowired
    private NotificationRetryScheduler scheduler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private NotificationRetryService retryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("scheduser")
                .email("sched@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testSchedulerProcessesEligibleCandidates() {
        Notification eligible = Notification.builder()
                .user(testUser)
                .title("Eligible")
                .message("body")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.FAILED)
                .nextRetryAt(LocalDateTime.now().minusMinutes(5)) // in the past
                .build();
        notificationRepository.save(eligible);

        Notification nonEligible = Notification.builder()
                .user(testUser)
                .title("Non-eligible")
                .message("body")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.FAILED)
                .nextRetryAt(LocalDateTime.now().plusMinutes(5)) // in the future
                .build();
        notificationRepository.save(nonEligible);

        scheduler.processPendingRetries();

        verify(retryService, times(1)).executeRetry(eq(eligible.getId()));
        verify(retryService, never()).executeRetry(eq(nonEligible.getId()));
    }
}
