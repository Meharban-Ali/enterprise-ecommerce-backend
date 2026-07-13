package com.redis.notification.entity;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.repository.NotificationRepository;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate; // prevent context failures from websocket mocks

    private User alice;
    private User bob;
    private Notification aliceNotification;

    @BeforeEach
    void setUp() {
        // Clean up database tables cleanly in order of dependency references to avoid key violations
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        alice = User.builder()
                .username("alice")
                .email("alice@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        alice = userRepository.save(alice);

        bob = User.builder()
                .username("bob")
                .email("bob@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        bob = userRepository.save(bob);

        aliceNotification = Notification.builder()
                .user(alice)
                .title("Alice's Alert")
                .message("Secret message")
                .type(NotificationType.SECURITY)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.HIGH)
                .status(NotificationStatus.SENT)
                .readStatus(false)
                .build();
        aliceNotification = notificationRepository.save(aliceNotification);
    }

    @Test
    void testBobCannotAccessAliceNotification() throws Exception {
        mockMvc.perform(get("/api/notifications/" + aliceNotification.getId())
                        .with(user(bob))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void testBobCannotDeleteAliceNotification() throws Exception {
        mockMvc.perform(delete("/api/notifications/" + aliceNotification.getId())
                        .with(user(bob))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
