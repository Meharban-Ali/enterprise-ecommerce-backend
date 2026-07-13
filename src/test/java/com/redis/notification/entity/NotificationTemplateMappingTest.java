package com.redis.notification.entity;

import com.redis.notification.service.EmailNotificationService;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.notification.entity.NotificationTemplate;
import com.redis.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class NotificationTemplateMappingTest {

    @Autowired
    private EmailNotificationService emailNotificationService;

    @MockBean
    private MailClient mailClient;

    @Test
    void testDetermineTemplateMapping() {
        User user = User.builder().username("testuser").email("test@example.com").role(Role.ROLE_USER).build();

        Notification orderShipped = Notification.builder()
                .user(user)
                .type(NotificationType.ORDER)
                .title("Your order has been Shipped!")
                .build();

        Notification welcome = Notification.builder()
                .user(user)
                .type(NotificationType.AUTH)
                .title("Welcome to E-Commerce!")
                .build();

        Notification paymentFailed = Notification.builder()
                .user(user)
                .type(NotificationType.PAYMENT)
                .title("Transaction Failed")
                .build();

        NotificationTemplate temp1 = ReflectionTestUtils.invokeMethod(emailNotificationService, "determineTemplate", orderShipped);
        NotificationTemplate temp2 = ReflectionTestUtils.invokeMethod(emailNotificationService, "determineTemplate", welcome);
        NotificationTemplate temp3 = ReflectionTestUtils.invokeMethod(emailNotificationService, "determineTemplate", paymentFailed);

        assertEquals(NotificationTemplate.ORDER_SHIPPED, temp1);
        assertEquals(NotificationTemplate.WELCOME, temp2);
        assertEquals(NotificationTemplate.PAYMENT_FAILED, temp3);
    }
}
