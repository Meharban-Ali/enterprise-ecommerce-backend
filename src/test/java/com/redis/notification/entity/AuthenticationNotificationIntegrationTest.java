package com.redis.notification.entity;

import com.redis.user.service.UserService;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.auth.dto.request.RegisterRequest;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class AuthenticationNotificationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private MailClient mailClient;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testUserRegistrationTriggersWelcomeNotification() {
        RegisterRequest request = RegisterRequest.builder()
                .username("reguser")
                .email("reg@example.com")
                .password("Password123!")
                .securityQuestion("Quest")
                .securityAnswer("Answ")
                .build();

        userService.registerUser(request);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Notification> list = notificationRepository.findAll();
            assertFalse(list.isEmpty());
            assertTrue(list.stream().anyMatch(n -> n.getTitle().contains("Welcome")));
        });
    }
}
