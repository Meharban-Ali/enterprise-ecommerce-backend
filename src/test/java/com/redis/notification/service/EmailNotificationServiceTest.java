package com.redis.notification.service;

import com.redis.notification.entity.NotificationChannel;
import com.redis.infrastructure.config.NotificationProperties;
import com.redis.order.entity.Order;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.MailClient;

import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailNotificationServiceTest {

    @Mock
    private MailClient mailClient;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private com.redis.infrastructure.config.NotificationProperties properties;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .id(1L)
                .username("john")
                .email("john@example.com")
                .build();

        testNotification = Notification.builder()
                .id(100L)
                .user(user)
                .title("Order Shipped")
                .message("Your order has shipped.")
                .type(NotificationType.ORDER)
                .channel(NotificationChannel.EMAIL)
                .priority(NotificationPriority.MEDIUM)
                .status(NotificationStatus.PENDING)
                .build();
    }

    @Test
    void testSupportsEmailChannelOnly() {
        assertTrue(emailNotificationService.supports(NotificationChannel.EMAIL));
    }

    @Test
    void testSendEmailSuccess() {
        when(properties.isTemplateManagementEnabled()).thenReturn(false);
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Mock Body</html>");

        emailNotificationService.send(testNotification);

        verify(templateEngine, times(1)).process(eq("order_shipped"), any(Context.class));
        verify(mailClient, times(1)).sendEmail(
                eq("john@example.com"),
                eq("Order Shipped"),
                eq("<html>Mock Body</html>"),
                eq(true)
        );
    }
}
