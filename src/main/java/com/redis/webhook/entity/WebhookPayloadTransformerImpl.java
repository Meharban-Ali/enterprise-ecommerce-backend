package com.redis.webhook.entity;

import com.redis.payment.entity.Payment;
import com.redis.product.entity.Product;
import com.redis.notification.entity.Notification;
import com.redis.order.entity.Order;
import com.redis.incident.entity.Incident;
import com.redis.notification.event.NotificationEvent;
import com.redis.user.entity.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.common.entity.IntegrationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookPayloadTransformerImpl implements WebhookPayloadTransformer {

    private final ObjectMapper objectMapper;

    @Override
    public String transform(IntegrationEventType eventType, Object payload, String version) {
        try {
            Object publicDto = convertToPublicDto(eventType, payload, version);
            return objectMapper.writeValueAsString(publicDto);
        } catch (Exception e) {
            log.error("Failed to transform payload for event type: {}", eventType, e);
            throw new RuntimeException("Payload transformation error", e);
        }
    }

    private Object convertToPublicDto(IntegrationEventType eventType, Object payload, String version) {
        if (payload == null) {
            return new GenericEventDto(eventType.name(), "No details", LocalDateTime.now());
        }

        if (payload instanceof com.redis.notification.event.NotificationEvent) {
            com.redis.notification.event.NotificationEvent ne = (com.redis.notification.event.NotificationEvent) payload;
            return new GenericEventDto(eventType.name(), ne.getTitle() + ": " + ne.getMessage(), LocalDateTime.now());
        }

        if (payload instanceof User) {
            User u = (User) payload;
            return new PublicUserDto(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(), u.isAccountEnabled());
        }

        if (payload instanceof Order) {
            Order o = (Order) payload;
            return new PublicOrderDto(o.getId(), o.getUser() != null ? o.getUser().getId() : null, o.getTotalAmount(), o.getStatus().name(), o.getOrderDate());
        }

        if (payload instanceof Payment) {
            Payment p = (Payment) payload;
            return new PublicPaymentDto(p.getId(), p.getOrder() != null ? p.getOrder().getId() : null, p.getAmount(), p.getStatus().name(), p.getPaymentGateway() != null ? p.getPaymentGateway().name() : null, p.getCreatedAt());
        }

        if (payload instanceof Product) {
            Product pr = (Product) payload;
            return new PublicProductDto(pr.getId(), pr.getName(), pr.getPrice(), pr.getRating(), pr.getStockQuantity());
        }

        if (payload instanceof Incident) {
            Incident inc = (Incident) payload;
            return new PublicIncidentDto(inc.getId(), inc.getIncidentNumber(), inc.getSeverity().name(), inc.getSource().name(), inc.getTitle(), inc.getDescription(), inc.getStatus().name(), inc.getCreatedAt());
        }

        if (payload instanceof Notification) {
            Notification n = (Notification) payload;
            return new PublicNotificationDto(n.getId(), n.getTitle(), n.getMessage(), n.getType().name(), n.getChannel().name(), n.getPriority().name(), n.getStatus().name(), n.getCreatedAt());
        }

        return payload;
    }

    // Static inner classes for public webhook event DTOs

    public record GenericEventDto(String eventType, String message, LocalDateTime timestamp) {}

    public record PublicUserDto(Long id, String username, String email, String role, boolean accountEnabled) {}

    public record PublicOrderDto(Long id, Long userId, BigDecimal totalAmount, String status, LocalDateTime createdAt) {}

    public record PublicPaymentDto(Long id, Long orderId, BigDecimal amount, String status, String paymentGateway, LocalDateTime createdAt) {}

    public record PublicProductDto(Long id, String name, BigDecimal price, BigDecimal rating, int stockQuantity) {}

    public record PublicIncidentDto(Long id, String incidentNumber, String severity, String source, String title, String description, String status, LocalDateTime createdAt) {}

    public record PublicNotificationDto(Long id, String title, String message, String type, String channel, String priority, String status, LocalDateTime createdAt) {}
}
