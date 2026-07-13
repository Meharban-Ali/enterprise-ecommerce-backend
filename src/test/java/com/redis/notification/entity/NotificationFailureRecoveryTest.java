package com.redis.notification.entity;

import com.redis.user.entity.Role;
import com.redis.notification.event.NotificationEventPublisher;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.order.event.OrderNotificationEvent;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationStatus;
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
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(properties = "app.reliability.resilience-enabled=false")
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationFailureRecoveryTest {

    @Autowired
    private NotificationEventPublisher publisher;

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
                .username("failuser")
                .email("fail@example.com")
                .password("Password123!")
                .role(com.redis.user.entity.Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testDeliveryFailureSchedulesRetryAndKeepsStateFailed() {
        // Force the delivery strategy service to throw an exception by stubbing mailClient
        doThrow(new RuntimeException("SMTP Host Unreachable")).when(mailClient)
                .sendEmail(anyString(), anyString(), anyString(), anyBoolean());

        OrderNotificationEvent event = new OrderNotificationEvent(
                this,
                testUser.getId(),
                "Transient Failure Alert",
                "Body content",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        );

        publisher.publishEvent(event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> list = notificationRepository.findAll();
            assertFalse(list.isEmpty());
            Notification notification = list.get(0);
            assertEquals(NotificationStatus.FAILED, notification.getStatus());
            assertEquals(1, notification.getRetryCount());
            assertEquals("SMTP Host Unreachable", notification.getFailureReason());
            assertNotNull(notification.getNextRetryAt());
        });
    }
}
