package com.redis.notification.event;

import com.redis.product.entity.Product;
import com.redis.order.entity.Order;
import com.redis.payment.entity.Payment;
import com.redis.notification.service.NotificationOutboxService;
import com.redis.order.event.OrderNotificationEvent;
import com.redis.payment.event.PaymentNotificationEvent;
import com.redis.payment.entity.Refund;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.user.entity.User;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationType;
import com.redis.user.entity.Role;
import com.redis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final NotificationProperties properties;
    private final NotificationOutboxService outboxService;
    private final NotificationEventBusFactory eventBusFactory;

    @Autowired(required = false)
    private UserRepository userRepository;

    /**
     * Publishes notification events globally through the configured outbox or local event bus.
     * Business modules should depend only on this class to publish notifications.
     */
    public void publishEvent(NotificationEvent event) {
        log.info("Publishing notification event of type: {} for user ID: {}", event.getType(), event.getUserId());
        if (properties.isOutboxEnabled()) {
            outboxService.saveEvent(event);
        } else {
            eventBusFactory.getEventBus().publish(event);
        }
    }

    private void notifyAdmins(String title, String message, NotificationType type, NotificationPriority priority) {
        if (userRepository != null) {
            List<User> admins = userRepository.findByRole(Role.ROLE_ADMIN);
            List<User> superAdmins = userRepository.findByRole(Role.ROLE_SUPER_ADMIN);
            List<User> allAdmins = new ArrayList<>();
            allAdmins.addAll(admins);
            allAdmins.addAll(superAdmins);
            
            if (allAdmins.isEmpty()) {
                log.warn("No administrators found with ROLE_ADMIN or ROLE_SUPER_ADMIN to notify.");
                return;
            }
            
            for (User admin : allAdmins) {
                NotificationEvent event;
                if (type == NotificationType.SYSTEM_ALERT) {
                    event = new SystemAlertNotificationEvent(this, admin.getId(), title, message, NotificationChannel.EMAIL, priority);
                } else if (type == NotificationType.INCIDENT) {
                    event = new IncidentNotificationEvent(this, admin.getId(), title, message, NotificationChannel.EMAIL, priority);
                } else {
                    event = new OperationalNotificationEvent(this, admin.getId(), title, message, NotificationChannel.EMAIL, priority);
                }
                publishEvent(event);
            }
        } else {
            log.warn("UserRepository is not available, unable to notify admins.");
        }
    }

    public void publishCriticalAlert(String title, String message) {
        notifyAdmins(title, message, NotificationType.SYSTEM_ALERT, NotificationPriority.HIGH);
    }

    public void publishHighSeverityIncident(String title, String message) {
        notifyAdmins(title, message, NotificationType.INCIDENT, NotificationPriority.HIGH);
    }

    public void publishIncidentResolved(String title, String message) {
        notifyAdmins(title, message, NotificationType.INCIDENT, NotificationPriority.MEDIUM);
    }

    public void publishDatabaseDown() {
        notifyAdmins("CRITICAL: Database Connection Lost", "The application database is down", NotificationType.SYSTEM_ALERT, NotificationPriority.HIGH);
    }

    public void publishRedisUnavailable() {
        notifyAdmins("WARNING: Redis Cache Unavailable", "The Redis cache connection is unavailable", NotificationType.SYSTEM_ALERT, NotificationPriority.HIGH);
    }

    public void publishSchedulerFailure(String schedulerName, String error) {
        notifyAdmins("ALERT: Scheduler Failure - " + schedulerName, "Scheduler failed with error: " + error, NotificationType.SYSTEM_ALERT, NotificationPriority.HIGH);
    }

    public void publishWelcome(Long userId, String email) {
        publishEvent(new AuthenticationNotificationEvent(
                this,
                userId,
                "Welcome to E-Commerce!",
                "Thank you for registering, " + email + ". Your account is now active.",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        ));
    }

    public void publishPasswordReset(Long userId, String email, String resetUrl) {
        publishEvent(new SecurityNotificationEvent(
                this,
                userId,
                "Password Reset Request",
                "To reset your password, please use the following link: " + resetUrl,
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        ));
    }

    public void publishPasswordChanged(Long userId, String email) {
        publishEvent(new SecurityNotificationEvent(
                this,
                userId,
                "Password Changed Successfully",
                "Your account password has been updated. If you did not make this change, contact support immediately.",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
                ));
    }

    public void publishOrderCreated(Long userId, Long orderId, BigDecimal amount) {
        publishEvent(new OrderNotificationEvent(
                this,
                userId,
                "Order Placed Successfully",
                "Your order #" + orderId + " has been placed for amount: $" + amount,
                NotificationChannel.EMAIL,
                NotificationPriority.MEDIUM
        ));
    }

    public void publishOrderShipped(Long userId, Long orderId) {
        publishEvent(new OrderNotificationEvent(
                this,
                userId,
                "Order Shipped",
                "Your order #" + orderId + " has been shipped.",
                NotificationChannel.EMAIL,
                NotificationPriority.MEDIUM
        ));
    }

    public void publishOrderProcessing(Long userId, Long orderId) {
        publishEvent(new OrderNotificationEvent(
                this,
                userId,
                "Order Payment Received",
                "Your order #" + orderId + " payment was successful and is now being processed.",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        ));
    }

    public void publishOrderDelivered(Long userId, Long orderId) {
        publishEvent(new OrderNotificationEvent(
                this,
                userId,
                "Order Delivered",
                "Your order #" + orderId + " has been delivered.",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        ));
    }

    public void publishOrderCancelled(Long userId, Long orderId) {
        publishEvent(new OrderNotificationEvent(
                this,
                userId,
                "Order Cancelled",
                "Your order #" + orderId + " has been cancelled.",
                NotificationChannel.EMAIL,
                NotificationPriority.MEDIUM
        ));
    }

    public void publishOrderExpired(Long userId, Long orderId) {
        publishEvent(new OrderNotificationEvent(
                this,
                userId,
                "Order Expired",
                "Your order #" + orderId + " has expired due to non-payment.",
                NotificationChannel.EMAIL,
                NotificationPriority.MEDIUM
        ));
    }

    public void publishPaymentSuccess(Long userId, Long orderId, BigDecimal amount) {
        publishEvent(new PaymentNotificationEvent(
                this,
                userId,
                "Payment Successful",
                "Payment of $" + amount + " for order #" + orderId + " completed successfully.",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        ));
    }

    public void publishPaymentFailed(Long userId, Long orderId, BigDecimal amount) {
        publishEvent(new PaymentNotificationEvent(
                this,
                userId,
                "Payment Failed",
                "Payment of $" + amount + " for order #" + orderId + " failed.",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        ));
    }

    public void publishPaymentCreated(Long userId, Long orderId, BigDecimal amount) {
        publishEvent(new PaymentNotificationEvent(
                this,
                userId,
                "Payment Session Created",
                "Payment session for $" + amount + " for order #" + orderId + " has been initiated.",
                NotificationChannel.EMAIL,
                NotificationPriority.MEDIUM
        ));
    }

    public void publishPaymentCancelled(Long userId, Long orderId) {
        publishEvent(new PaymentNotificationEvent(
                this,
                userId,
                "Payment Session Cancelled",
                "Payment session for order #" + orderId + " has been cancelled.",
                NotificationChannel.EMAIL,
                NotificationPriority.MEDIUM
        ));
    }

    public void publishRetryPaymentInitiated(Long userId, Long orderId) {
        publishEvent(new PaymentNotificationEvent(
                this,
                userId,
                "Retry Payment Initiated",
                "A retry payment has been initiated for order #" + orderId,
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        ));
    }

    public void publishRefundCompleted(Long userId, Long paymentId, BigDecimal amount) {
        publishEvent(new PaymentNotificationEvent(
                this,
                userId,
                "Refund Successful",
                "Refund of $" + amount + " for payment #" + paymentId + " has been processed.",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        ));
    }

    public void publishLowStock(Long adminUserId, String productName, int currentStock) {
        publishEvent(new OrderNotificationEvent(
                this,
                adminUserId,
                "LOW STOCK WARNING: " + productName,
                "Stock level for '" + productName + "' has dropped to: " + currentStock,
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        ));
    }

    public void publishOutOfStock(Long adminUserId, String productName) {
        publishEvent(new OrderNotificationEvent(
                this,
                adminUserId,
                "OUT OF STOCK WARNING: " + productName,
                "Product '" + productName + "' is now completely out of stock.",
                NotificationChannel.EMAIL,
                NotificationPriority.HIGH
        ));
    }
}
