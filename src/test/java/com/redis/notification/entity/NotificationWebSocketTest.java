package com.redis.notification.entity;

import com.redis.notification.event.NotificationEventPublisher;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.order.event.OrderNotificationEvent;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationWebSocketTest {

    @Autowired
    private NotificationEventPublisher publisher;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @MockBean
    private MailClient mailClient;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("websocketuser")
                .email("ws@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testNotificationEventTriggersWebSocketPushToUserDestination() {
        OrderNotificationEvent event = new OrderNotificationEvent(
                this,
                testUser.getId(),
                "WebSocket Alert",
                "STOMP message body",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        );

        publisher.publishEvent(event);

        // Await until the async listener executes and triggers WebSocket message delivery
        // Since getUsername() returns email address (ws@example.com) for authentication logic, check for email value.
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(messagingTemplate, times(1)).convertAndSendToUser(
                    eq(testUser.getUsername()),
                    eq("/queue/notifications"),
                    any(Object.class)
            );
        });
    }
}
