package com.redis.notification.service;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.order.dto.OrderEmailTemplateData;
import com.redis.product.entity.Product;
import com.redis.common.dto.WelcomeTemplateData;
import com.redis.payment.dto.RefundEmailTemplateData;
import com.redis.notification.dto.NotificationTemplateData;
import com.redis.notification.entity.MailClient;
import com.redis.common.dto.PasswordResetTemplateData;
import com.redis.reliability.service.PlatformResilienceService;
import com.redis.payment.dto.PaymentEmailTemplateData;

import com.redis.notification.entity.Notification;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationChannelService {

    private final MailClient mailClient;
    private final TemplateEngine templateEngine;
    private final com.redis.infrastructure.config.NotificationProperties properties;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PlatformResilienceService resilienceService;

    @Override
    public void send(Notification notification) {
        log.info("Sending Email notification to: {}", notification.getUser().getEmail());

        String htmlBody;
        if (properties.isTemplateManagementEnabled()) {
            htmlBody = notification.getMessage();
        } else {
            NotificationTemplate template = determineTemplate(notification);
            NotificationTemplateData data = buildTemplateData(notification, template);

            Context context = new Context();
            context.setVariable("data", data);
            context.setVariable("message", notification.getMessage());

            htmlBody = templateEngine.process(template.getTemplateName(), context);
        }

        if (resilienceService != null) {
            resilienceService.execute("emailSmtp",
                    () -> {
                        mailClient.sendEmail(
                                notification.getUser().getEmail(),
                                notification.getTitle(),
                                htmlBody,
                                true
                        );
                        return null;
                    },
                    () -> {
                        throw new RuntimeException("SMTP Server Unavailable (fallback)");
                    }
            );
        } else {
            mailClient.sendEmail(
                    notification.getUser().getEmail(),
                    notification.getTitle(),
                    htmlBody,
                    true
            );
        }
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.EMAIL;
    }

    private NotificationTemplate determineTemplate(Notification notification) {
        String title = notification.getTitle().toLowerCase();
        switch (notification.getType()) {
            case ORDER:
                if (title.contains("shipped")) return NotificationTemplate.ORDER_SHIPPED;
                if (title.contains("delivered")) return NotificationTemplate.ORDER_DELIVERED;
                return NotificationTemplate.ORDER_PLACED;
            case PAYMENT:
                if (title.contains("fail")) return NotificationTemplate.PAYMENT_FAILED;
                if (title.contains("refund")) return NotificationTemplate.REFUND_SUCCESS;
                return NotificationTemplate.PAYMENT_SUCCESS;
            case AUTH:
                if (title.contains("reset") || title.contains("password")) return NotificationTemplate.PASSWORD_RESET;
                return NotificationTemplate.WELCOME;
            case SECURITY:
                return NotificationTemplate.PASSWORD_RESET;
            case SYSTEM:
            default:
                return NotificationTemplate.WELCOME;
        }
    }

    private NotificationTemplateData buildTemplateData(Notification notification, NotificationTemplate template) {
        String customerName = notification.getUser().getUsername();
        switch (template) {
            case ORDER_PLACED:
            case ORDER_SHIPPED:
            case ORDER_DELIVERED:
                return OrderEmailTemplateData.builder()
                        .customerName(customerName)
                        .orderId(12345L)
                        .totalAmount(new BigDecimal("99.99"))
                        .productList(List.of("Product A", "Product B"))
                        .companyName("E-Commerce Corp")
                        .supportContact("support@ecommerce.com")
                        .build();
            case PAYMENT_SUCCESS:
            case PAYMENT_FAILED:
                return PaymentEmailTemplateData.builder()
                        .customerName(customerName)
                        .orderId(12345L)
                        .paymentAmount(new BigDecimal("99.99"))
                        .paymentGateway("Stripe")
                        .companyName("E-Commerce Corp")
                        .supportContact("support@ecommerce.com")
                        .build();
            case REFUND_SUCCESS:
                return RefundEmailTemplateData.builder()
                        .customerName(customerName)
                        .paymentId(500L)
                        .refundAmount(new BigDecimal("99.99"))
                        .companyName("E-Commerce Corp")
                        .supportContact("support@ecommerce.com")
                        .build();
            case PASSWORD_RESET:
                return PasswordResetTemplateData.builder()
                        .customerName(customerName)
                        .resetUrl("https://ecommerce.com/reset?token=abc")
                        .companyName("E-Commerce Corp")
                        .supportContact("support@ecommerce.com")
                        .build();
            case WELCOME:
            default:
                return WelcomeTemplateData.builder()
                        .customerName(customerName)
                        .companyName("E-Commerce Corp")
                        .supportContact("support@ecommerce.com")
                        .build();
        }
    }
}
