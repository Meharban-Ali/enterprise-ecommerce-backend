package com.redis.notification.event;

import com.redis.payment.entity.Payment;
import com.redis.order.entity.Order;
import com.redis.notification.entity.MailClient;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.notification.entity.Notification;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationEventPublishingTest {

    @Autowired
    private NotificationEventPublisher publisher;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MailClient mailClient; // prevent SMTP host exceptions

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("publisheruser")
                .email("publisher@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testPublishOrderCreatedEvent() {
        publisher.publishOrderCreated(testUser.getId(), 1001L, new BigDecimal("199.99"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> list = notificationRepository.findAll();
            assertFalse(list.isEmpty());
            assertTrue(list.stream().anyMatch(n -> n.getTitle().contains("Order Placed Successfully")));
        });
    }

    @Test
    void testPublishPaymentSuccessEvent() {
        publisher.publishPaymentSuccess(testUser.getId(), 1001L, new BigDecimal("199.99"));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> list = notificationRepository.findAll();
            assertFalse(list.isEmpty());
            assertTrue(list.stream().anyMatch(n -> n.getTitle().contains("Payment Successful")));
        });
    }
}
