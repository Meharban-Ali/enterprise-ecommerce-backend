package com.redis.notification.service;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.dto.request.NotificationTemplateRequest;
import com.redis.notification.dto.response.NotificationTemplateResponse;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationTemplateServiceTest {

    @Autowired
    private NotificationTemplateService templateService;

    @Autowired
    private NotificationTemplateRepository templateRepository;

    @BeforeEach
    void setUp() {
        templateRepository.deleteAll();
    }

    @Test
    void testTemplateLifecycle() {
        NotificationTemplateRequest req = NotificationTemplateRequest.builder()
                .templateCode("WELCOME_CODE")
                .templateName("Welcome Template")
                .notificationType(NotificationType.AUTH)
                .notificationChannel(NotificationChannel.EMAIL)
                .subject("Welcome ${username}!")
                .htmlTemplate("<p>Hi ${username}</p>")
                .build();

        NotificationTemplateResponse created = templateService.createTemplate(req);
        assertNotNull(created.getId());
        assertEquals("WELCOME_CODE", created.getTemplateCode());
        assertEquals(1, created.getVersion());
        assertTrue(created.isActive());

        NotificationTemplateResponse deactivated = templateService.deactivateTemplate(created.getId());
        assertFalse(deactivated.isActive());

        NotificationTemplateResponse activated = templateService.activateTemplate(created.getId());
        assertTrue(activated.isActive());
    }
}
