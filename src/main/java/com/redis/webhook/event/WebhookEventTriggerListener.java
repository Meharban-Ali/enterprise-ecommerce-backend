package com.redis.webhook.event;

import com.redis.order.event.OrderNotificationEvent;
import com.redis.webhook.service.WebhookService;
import com.redis.notification.event.SystemAlertNotificationEvent;
import com.redis.notification.event.IncidentNotificationEvent;
import com.redis.payment.event.PaymentNotificationEvent;
import com.redis.notification.event.NotificationEvent;
import com.redis.notification.event.SecurityNotificationEvent;
import com.redis.notification.event.AuthenticationNotificationEvent;

import com.redis.common.entity.IntegrationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookEventTriggerListener {

    private final WebhookService webhookService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Transactional Event Listener triggered for Webhook publishing: {}", event.getClass().getSimpleName());
        
        IntegrationEventType type = mapToIntegrationEventType(event);
        if (type != null) {
            try {
                webhookService.publish(type, event);
            } catch (Exception e) {
                log.error("Failed to publish webhook event for type: {}", type, e);
            }
        }
    }

    private IntegrationEventType mapToIntegrationEventType(NotificationEvent event) {
        String title = event.getTitle() != null ? event.getTitle().toLowerCase() : "";

        if (event instanceof AuthenticationNotificationEvent) {
            if (title.contains("welcome") || title.contains("register")) {
                return IntegrationEventType.USER_REGISTERED;
            } else {
                return IntegrationEventType.USER_LOGIN;
            }
        } else if (event instanceof SecurityNotificationEvent) {
            if (title.contains("reset")) {
                return IntegrationEventType.PASSWORD_RESET;
            } else {
                return IntegrationEventType.SYSTEM_EVENT;
            }
        } else if (event instanceof OrderNotificationEvent) {
            if (title.contains("placed") || title.contains("created")) {
                return IntegrationEventType.ORDER_CREATED;
            } else if (title.contains("shipped")) {
                return IntegrationEventType.ORDER_SHIPPED;
            } else if (title.contains("delivered")) {
                return IntegrationEventType.ORDER_DELIVERED;
            } else if (title.contains("cancelled")) {
                return IntegrationEventType.ORDER_CANCELLED;
            } else if (title.contains("low stock")) {
                return IntegrationEventType.INVENTORY_LOW;
            } else if (title.contains("out of stock")) {
                return IntegrationEventType.INVENTORY_OUT_OF_STOCK;
            } else {
                return IntegrationEventType.ORDER_UPDATED;
            }
        } else if (event instanceof PaymentNotificationEvent) {
            if (title.contains("success")) {
                return IntegrationEventType.PAYMENT_SUCCESS;
            } else if (title.contains("fail")) {
                return IntegrationEventType.PAYMENT_FAILED;
            } else if (title.contains("refund")) {
                return IntegrationEventType.PAYMENT_REFUND;
            } else {
                return IntegrationEventType.PAYMENT_CREATED;
            }
        } else if (event instanceof SystemAlertNotificationEvent) {
            return IntegrationEventType.ALERT_TRIGGERED;
        } else if (event instanceof IncidentNotificationEvent) {
            if (title.contains("resolved")) {
                return IntegrationEventType.INCIDENT_RESOLVED;
            } else {
                return IntegrationEventType.INCIDENT_OPENED;
            }
        }
        
        return IntegrationEventType.SYSTEM_EVENT;
    }
}
