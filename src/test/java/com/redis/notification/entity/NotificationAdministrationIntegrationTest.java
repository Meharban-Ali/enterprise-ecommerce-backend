package com.redis.notification.entity;

import com.redis.notification.service.UserNotificationPreferenceService;
import com.redis.notification.event.NotificationEventListener;
import com.redis.notification.service.NotificationTemplateService;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.event.NotificationEvent;
import com.redis.notification.event.SystemNotificationEvent;
import com.redis.notification.dto.request.NotificationPreferenceRequest;
import com.redis.notification.dto.response.NotificationPreviewResponse;
import com.redis.notification.dto.request.NotificationTemplateRequest;
import com.redis.notification.dto.response.NotificationTemplateResponse;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationType;
import com.redis.cart.repository.CartRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.notification.repository.NotificationTemplateRepository;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationAdministrationIntegrationTest {

    @Autowired
    private NotificationTemplateService templateService;

    @Autowired
    private UserNotificationPreferenceService preferenceService;

    @Autowired
    private NotificationEventListener eventListener;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private NotificationProperties properties;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.redis.notification.entity.MailClient mailClient;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        templateRepository.deleteAll();
        cartRepository.deleteAll();
        userRepository.deleteAll();

        properties.setQueueEnabled(false);
        properties.setOutboxEnabled(false);
        properties.setTemplateManagementEnabled(true);
        properties.setPreferenceValidationEnabled(true);
        properties.setRateLimitingEnabled(false);

        testUser = User.builder()
                .username("admin_test_user")
                .email("admin_test@example.com")
                .password("Password123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testTemplatePreviewAndVersioning() {
        NotificationTemplateRequest req = NotificationTemplateRequest.builder()
                .templateCode("PREVIEW_CODE")
                .templateName("Preview Name")
                .notificationType(NotificationType.SYSTEM)
                .notificationChannel(NotificationChannel.EMAIL)
                .subject("System Update for [[${username}]]")
                .htmlTemplate("<p>Hi [[${username}]]</p>")
                .build();

        NotificationTemplateResponse v1 = templateService.createTemplate(req);
        assertEquals(1, v1.getVersion());

        Map<String, Object> vars = new HashMap<>();
        vars.put("username", "JohnDoe");
        NotificationPreviewResponse preview = templateService.previewTemplate(v1.getId(), vars);
        assertEquals("System Update for JohnDoe", preview.getSubject());
        assertEquals("<p>Hi JohnDoe</p>", preview.getRenderedHtml());

        req.setSubject("New System Update for [[${username}]]");
        NotificationTemplateResponse v2 = templateService.createTemplate(req);
        assertEquals(2, v2.getVersion());

        NotificationTemplateResponse rolled = templateService.rollbackVersion("PREVIEW_CODE", 1);
        assertEquals(1, rolled.getVersion());
        assertTrue(rolled.isActive());
    }

    @Test
    void testUserPreferencesSkippingAndSecurityBypass() throws Exception {
        NotificationPreferenceRequest prefReq = NotificationPreferenceRequest.builder()
                .emailEnabled(false)
                .smsEnabled(true)
                .pushEnabled(true)
                .inAppEnabled(true)
                .marketingEnabled(true)
                .build();
        preferenceService.updatePreferences(testUser.getId(), prefReq);

        NotificationEvent systemEvent = new SystemNotificationEvent(
                this, testUser.getId(), "System Alert", "Check updates", 
                NotificationChannel.EMAIL, com.redis.notification.entity.NotificationPriority.LOW);
        
        eventListener.handleNotificationEvent(systemEvent);
        Thread.sleep(1000);

        List<Notification> notifications = notificationRepository.findAll();
        assertEquals(1, notifications.size());
        assertEquals(NotificationStatus.SKIPPED, notifications.get(0).getStatus());

        NotificationEvent securityEvent = new NotificationEvent(
                this, testUser.getId(), "Security Alert", "Reset your pass", 
                NotificationChannel.EMAIL, com.redis.notification.entity.NotificationPriority.HIGH, NotificationType.SECURITY) {};

        notificationRepository.deleteAll();
        eventListener.handleNotificationEvent(securityEvent);
        Thread.sleep(1000);

        List<Notification> secNotifications = notificationRepository.findAll();
        assertEquals(1, secNotifications.size());
        assertNotEquals(NotificationStatus.SKIPPED, secNotifications.get(0).getStatus());
    }
}
