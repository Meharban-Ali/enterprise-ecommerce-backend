package com.redis.notification.service;

import com.redis.notification.event.SecurityNotificationEvent;
import com.redis.notification.event.OperationalNotificationEvent;
import com.redis.notification.event.NotificationEventBusFactory;
import com.redis.notification.event.SystemNotificationEvent;
import com.redis.notification.event.NotificationEventBus;
import com.redis.order.event.OrderNotificationEvent;
import com.redis.notification.event.SystemAlertNotificationEvent;
import com.redis.notification.event.IncidentNotificationEvent;
import com.redis.payment.event.PaymentNotificationEvent;
import com.redis.notification.event.NotificationEvent;
import com.redis.notification.event.AuthenticationNotificationEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.notification.entity.NotificationOutbox;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationType;
import com.redis.common.entity.OutboxStatus;
import com.redis.notification.repository.NotificationOutboxRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxServiceImpl implements NotificationOutboxService {

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationEventBusFactory eventBusFactory;
    private final ObjectMapper objectMapper;

    @Getter
    @Setter
    public static class EventPayloadDto {
        private Long userId;
        private String title;
        private String message;
        private NotificationChannel channel;
        private NotificationPriority priority;
        private NotificationType type;
    }

    @Override
    @Transactional
    public void saveEvent(NotificationEvent event) {
        try {
            EventPayloadDto dto = new EventPayloadDto();
            dto.setUserId(event.getUserId());
            dto.setTitle(event.getTitle());
            dto.setMessage(event.getMessage());
            dto.setChannel(event.getChannel());
            dto.setPriority(event.getPriority());
            dto.setType(event.getType());

            String payloadJson = objectMapper.writeValueAsString(dto);

            NotificationOutbox outbox = NotificationOutbox.builder()
                    .eventId(UUID.randomUUID())
                    .aggregateType(event.getType().name())
                    .aggregateId(String.valueOf(event.getUserId()))
                    .eventType(event.getClass().getSimpleName())
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .build();

            outboxRepository.save(outbox);
            log.info("OBSERVABILITY - OUTBOX_CREATED: outboxId={}, eventType={}, userId={}",
                    outbox.getId(), outbox.getEventType(), event.getUserId());
        } catch (Exception e) {
            log.error("Failed to save event to transactional outbox: {}", e.getMessage(), e);
            throw new RuntimeException("Outbox persistence failure", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationOutbox> getPendingEvents(int batchSize) {
        return outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.PENDING);
    }

    @Override
    @Transactional
    public void processOutboxBatch(List<NotificationOutbox> outboxRecords) {
        if (outboxRecords == null || outboxRecords.isEmpty()) {
            return;
        }

        NotificationEventBus eventBus = eventBusFactory.getEventBus();

        for (NotificationOutbox record : outboxRecords) {
            try {
                // Reconstruct the event
                EventPayloadDto dto = objectMapper.readValue(record.getPayload(), EventPayloadDto.class);
                NotificationEvent event = reconstructEvent(dto);

                // Publish
                eventBus.publish(event);

                record.setStatus(OutboxStatus.PUBLISHED);
                record.setProcessedAt(LocalDateTime.now());
                outboxRepository.saveAndFlush(record);
                log.info("OBSERVABILITY - OUTBOX_PUBLISHED: outboxId={}, eventId={}", record.getId(), record.getEventId());
            } catch (Exception e) {
                log.error("Failed to publish outbox record ID {}: {}", record.getId(), e.getMessage());
                record.setRetryCount(record.getRetryCount() + 1);
                if (record.getRetryCount() >= 5) {
                    record.setStatus(OutboxStatus.FAILED);
                } else {
                    record.setStatus(OutboxStatus.PENDING); // retry later
                }
                outboxRepository.saveAndFlush(record);
            }
        }
    }

    private NotificationEvent reconstructEvent(EventPayloadDto dto) {
        Object source = new Object();
        return switch (dto.getType()) {
            case AUTH -> new AuthenticationNotificationEvent(
                    source, dto.getUserId(), dto.getTitle(), dto.getMessage(), dto.getChannel(), dto.getPriority());
            case SECURITY -> new SecurityNotificationEvent(
                    source, dto.getUserId(), dto.getTitle(), dto.getMessage(), dto.getChannel(), dto.getPriority());
            case ORDER -> new OrderNotificationEvent(
                    source, dto.getUserId(), dto.getTitle(), dto.getMessage(), dto.getChannel(), dto.getPriority());
            case PAYMENT -> new PaymentNotificationEvent(
                    source, dto.getUserId(), dto.getTitle(), dto.getMessage(), dto.getChannel(), dto.getPriority());
            case SYSTEM -> new SystemNotificationEvent(
                    source, dto.getUserId(), dto.getTitle(), dto.getMessage(), dto.getChannel(), dto.getPriority());
            case SYSTEM_ALERT -> new SystemAlertNotificationEvent(
                    source, dto.getUserId(), dto.getTitle(), dto.getMessage(), dto.getChannel(), dto.getPriority());
            case INCIDENT -> new IncidentNotificationEvent(
                    source, dto.getUserId(), dto.getTitle(), dto.getMessage(), dto.getChannel(), dto.getPriority());
            case OPERATIONAL -> new OperationalNotificationEvent(
                    source, dto.getUserId(), dto.getTitle(), dto.getMessage(), dto.getChannel(), dto.getPriority());
        };
    }
}
