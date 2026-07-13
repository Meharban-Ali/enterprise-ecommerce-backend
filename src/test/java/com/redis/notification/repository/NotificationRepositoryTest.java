package com.redis.notification.repository;

import com.redis.notification.entity.NotificationChannel;
import com.redis.order.entity.Order;
import com.redis.notification.entity.NotificationType;
import com.redis.user.repository.UserRepository;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationPriority;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
public class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("notifuser")
                .email("notifuser@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testSaveAndRetrieveNotification() {
        Notification notification = Notification.builder()
                .user(testUser)
                .title("Order Confirmed")
                .message("Your order #12345 has been placed successfully.")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.PENDING)
                .readStatus(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());

        Page<Notification> foundPage = notificationRepository.findByUserId(testUser.getId(), PageRequest.of(0, 10));
        assertFalse(foundPage.isEmpty());
        assertEquals(1, foundPage.getTotalElements());
        assertEquals("Order Confirmed", foundPage.getContent().get(0).getTitle());
    }

    @Test
    void testFindUnreadAndCount() {
        Notification notification1 = Notification.builder()
                .user(testUser)
                .title("Unread Title")
                .message("Unread Body")
                .type(NotificationType.SYSTEM)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.LOW)
                .status(NotificationStatus.SENT)
                .readStatus(false)
                .build();

        Notification notification2 = Notification.builder()
                .user(testUser)
                .title("Read Title")
                .message("Read Body")
                .type(NotificationType.SYSTEM)
                .channel(NotificationChannel.IN_APP)
                .priority(NotificationPriority.LOW)
                .status(NotificationStatus.SENT)
                .readStatus(true)
                .build();

        notificationRepository.save(notification1);
        notificationRepository.save(notification2);

        long unreadCount = notificationRepository.countByUserIdAndReadStatus(testUser.getId(), false);
        assertEquals(1, unreadCount);

        Page<Notification> unreadPage = notificationRepository.findByUserIdAndReadStatus(testUser.getId(), false, PageRequest.of(0, 10));
        assertEquals(1, unreadPage.getTotalElements());
        assertEquals("Unread Title", unreadPage.getContent().get(0).getTitle());
    }

    @Test
    void testFindTop20RecentNotifications() {
        for (int i = 1; i <= 25; i++) {
            Notification notification = Notification.builder()
                    .user(testUser)
                    .title("Notification " + i)
                    .message("Message Body " + i)
                    .type(NotificationType.SYSTEM)
                    .channel(NotificationChannel.WEBSOCKET)
                    .priority(NotificationPriority.LOW)
                    .status(NotificationStatus.SENT)
                    .readStatus(false)
                    .build();
            notificationRepository.save(notification);
        }

        List<Notification> top20 = notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertEquals(20, top20.size());
        // Verify they are ordered descending (newer first)
        assertTrue(top20.get(0).getCreatedAt().isAfter(top20.get(19).getCreatedAt()) || top20.get(0).getCreatedAt().isEqual(top20.get(19).getCreatedAt()));
    }
}
