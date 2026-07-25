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

import com.redis.reliability.service.PlatformResilienceService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationChannelService {

    private final MailClient mailClient;
    private final TemplateEngine templateEngine;
    private final com.redis.infrastructure.config.NotificationProperties properties;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private PlatformResilienceService resilienceService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.notification.repository.NotificationRepository notificationRepository;

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
                        notification.setStatus(com.redis.notification.entity.NotificationStatus.SENT);
                        notification.setDeliveredAt(java.time.LocalDateTime.now());
                        if (notificationRepository != null) {
                            notificationRepository.save(notification);
                        }
                        return null;
                    },
                    () -> {
                        notification.setStatus(com.redis.notification.entity.NotificationStatus.FAILED);
                        if (notificationRepository != null) {
                            notificationRepository.save(notification);
                        }
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
            notification.setStatus(com.redis.notification.entity.NotificationStatus.SENT);
            notification.setDeliveredAt(java.time.LocalDateTime.now());
            if (notificationRepository != null) {
                notificationRepository.save(notification);
            }
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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.order.repository.OrderRepository orderRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.payment.repository.PaymentRepository paymentRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.payment.repository.RefundRepository refundRepository;

    private NotificationTemplateData buildTemplateData(Notification notification, NotificationTemplate template) {
        String customerName = (notification.getUser() != null && notification.getUser().getUsername() != null)
                ? notification.getUser().getUsername()
                : "Valued Customer";

        Long refId = notification.getReferenceEntityId();
        String refType = notification.getReferenceEntityType();

        switch (template) {
            case ORDER_PLACED:
            case ORDER_SHIPPED:
            case ORDER_DELIVERED: {
                Long orderId = refId != null ? refId : 0L;
                BigDecimal totalAmount = null;
                List<String> productList = new ArrayList<>();

                if (refId != null && orderRepository != null) {
                    java.util.Optional<com.redis.order.entity.Order> orderOpt = orderRepository.findById(refId);
                    if (orderOpt.isPresent()) {
                        com.redis.order.entity.Order order = orderOpt.get();
                        orderId = order.getId();
                        totalAmount = order.getTotalAmount();
                        if (order.getItems() != null && !order.getItems().isEmpty()) {
                            productList = order.getItems().stream()
                                    .map(i -> i.getProduct() != null ? i.getProduct().getName() : "Item #" + i.getId())
                                    .filter(Objects::nonNull)
                                    .toList();
                        }
                    } else {
                        log.warn("Referenced Order ID {} not found in DB for Notification ID {}. Rendering order status notification without fabricated totals.", refId, notification.getId());
                    }
                }

                if (productList.isEmpty()) {
                    productList = List.of(orderId > 0 ? "Order #" + orderId + " Items (Processing)" : "Order Details Processing");
                }

                return OrderEmailTemplateData.builder()
                        .customerName(customerName)
                        .orderId(orderId)
                        .totalAmount(totalAmount)
                        .productList(productList)
                        .companyName("E-Commerce Corp")
                        .supportContact("support@ecommerce.com")
                        .build();
            }

            case PAYMENT_SUCCESS:
            case PAYMENT_FAILED: {
                Long orderId = refId != null ? refId : 0L;
                BigDecimal paymentAmount = null;
                String gateway = "Standard Gateway";

                if (refId != null && paymentRepository != null) {
                    java.util.Optional<com.redis.payment.entity.Payment> paymentOpt = paymentRepository.findByOrderId(refId);
                    if (paymentOpt.isPresent()) {
                        com.redis.payment.entity.Payment p = paymentOpt.get();
                        paymentAmount = p.getAmount();
                        if (p.getPaymentGateway() != null) gateway = p.getPaymentGateway().name();
                    } else {
                        log.warn("Payment for Order ID {} not found in DB for Notification ID {}. Rendering notification without fabricated payment totals.", refId, notification.getId());
                    }
                }

                return PaymentEmailTemplateData.builder()
                        .customerName(customerName)
                        .orderId(orderId)
                        .paymentAmount(paymentAmount)
                        .paymentGateway(gateway)
                        .companyName("E-Commerce Corp")
                        .supportContact("support@ecommerce.com")
                        .build();
            }

            case REFUND_SUCCESS: {
                Long paymentId = refId != null ? refId : 0L;
                BigDecimal refundAmount = null;

                if (refId != null) {
                    if (refundRepository != null) {
                        java.util.Optional<com.redis.payment.entity.Refund> refundOpt = refundRepository.findById(refId);
                        if (refundOpt.isPresent()) {
                            com.redis.payment.entity.Refund refund = refundOpt.get();
                            refundAmount = refund.getAmount();
                            if (refund.getPayment() != null) {
                                paymentId = refund.getPayment().getId();
                            }
                        } else {
                            List<com.redis.payment.entity.Refund> refunds = refundRepository.findByPaymentId(refId);
                            if (!refunds.isEmpty()) {
                                refundAmount = refunds.get(0).getAmount();
                            }
                        }
                    }

                    if (refundAmount == null && paymentRepository != null) {
                        java.util.Optional<com.redis.payment.entity.Payment> paymentOpt = paymentRepository.findById(refId);
                        if (paymentOpt.isPresent()) {
                            paymentId = paymentOpt.get().getId();
                            refundAmount = paymentOpt.get().getAmount();
                        } else {
                            log.warn("Payment/Refund ID {} not found in DB for Notification ID {}. Rendering refund notification without fabricated totals.", refId, notification.getId());
                        }
                    }
                }

                return RefundEmailTemplateData.builder()
                        .customerName(customerName)
                        .paymentId(paymentId)
                        .refundAmount(refundAmount)
                        .companyName("E-Commerce Corp")
                        .supportContact("support@ecommerce.com")
                        .build();
            }

            case PASSWORD_RESET: {
                String resetUrl = (notification.getActionUrl() != null && !notification.getActionUrl().isBlank())
                        ? notification.getActionUrl()
                        : "https://ecommerce.com/reset";

                return PasswordResetTemplateData.builder()
                        .customerName(customerName)
                        .resetUrl(resetUrl)
                        .companyName("E-Commerce Corp")
                        .supportContact("support@ecommerce.com")
                        .build();
            }

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
