package com.redis.notification.service;

import com.redis.auth.dto.request.RegisterRequest;
import com.redis.user.service.UserService;
import com.redis.notification.repository.NotificationRepository;
import com.redis.notification.entity.NotificationStatus;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.concurrent.TimeUnit;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
public class RealWelcomeEmailIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void testUserRegistrationSendsEmail() {
        // Ensure clean state for user and notifications
        notificationRepository.deleteAll();
        userRepository.findByEmail("supportecommerces@gmail.com").ifPresent(u -> {
            userRepository.delete(u);
        });

        RegisterRequest request = RegisterRequest.builder()
                .username("supportecommerces")
                .email("supportecommerces@gmail.com")
                .password("Password123!")
                .securityQuestion("Quest")
                .securityAnswer("Answ")
                .build();

        System.out.println("TEST START: Registering user to trigger welcome email...");
        userService.registerUser(request);
        System.out.println("TEST STEP: User registered. Waiting for asynchronous email delivery...");

        // Wait for status to become SENT or FAILED
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var notifications = notificationRepository.findAll();
            var welcomeNotif = notifications.stream()
                    .filter(n -> n.getTitle().contains("Welcome"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(welcomeNotif, "Welcome notification should be created");
            System.out.println("Found welcome notification in DB: ID=" + welcomeNotif.getId() + ", Status=" + welcomeNotif.getStatus());
            assertEquals(NotificationStatus.SENT, welcomeNotif.getStatus(), "Notification status should transition to SENT");
        });
    }
}
