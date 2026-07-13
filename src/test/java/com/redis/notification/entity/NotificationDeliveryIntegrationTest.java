package com.redis.notification.entity;

import com.redis.order.entity.Order;
import com.redis.notification.event.NotificationEventPublisher;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.order.event.OrderNotificationEvent;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.notification.repository.NotificationRepository;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationDeliveryIntegrationTest {

    @Autowired
    private NotificationEventPublisher publisher;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private MailClient mailClient;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        testUser = User.builder()
                .username("deliveryuser")
                .email("deliveryuser@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testSMTPFailureStatusTransitionToFailed() {
        // Mock SMTP failure
        doThrow(new RuntimeException("SMTP Host Unreachable"))
                .when(mailClient).sendEmail(anyString(), anyString(), anyString(), anyBoolean());

        OrderNotificationEvent event = new OrderNotificationEvent(
                this,
                testUser.getId(),
                "Failure Test Order",
                "Test Body",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        );

        // Perform event publication
        publisher.publishEvent(event);

        // Verify that the listener catches the failure and updates status to FAILED
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertFalse(notifications.isEmpty());
            Notification notification = notifications.get(0);
            assertEquals("Failure Test Order", notification.getTitle());
            assertEquals(NotificationStatus.FAILED, notification.getStatus());
        });
    }
}
