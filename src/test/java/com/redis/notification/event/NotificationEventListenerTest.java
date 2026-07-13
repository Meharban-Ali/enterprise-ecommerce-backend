package com.redis.notification.event;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.MailClient;

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

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationEventListenerTest {

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
                .username("eventuser")
                .email("eventuser@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testNotificationEventListenerSavesNotificationAsynchronously() {
        OrderNotificationEvent event = new OrderNotificationEvent(
                this,
                testUser.getId(),
                "Async Title",
                "Async Message Body",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        );

        publisher.publishEvent(event);

        // Await asynchronously until the listener execution completes and saves the entity in SENT status
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> notifications = notificationRepository.findAll();
            assertFalse(notifications.isEmpty());
            Notification notification = notifications.get(0);
            assertEquals("Async Title", notification.getTitle());
            assertEquals("Async Message Body", notification.getMessage());
            assertEquals(NotificationStatus.SENT, notification.getStatus());
            assertEquals(NotificationType.ORDER, notification.getType());
        });
    }
}
