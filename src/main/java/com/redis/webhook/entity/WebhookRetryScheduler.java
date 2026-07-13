package com.redis.webhook.entity;

import com.redis.notification.service.NotificationQueueService;

import com.redis.notification.entity.Notification;
import com.redis.user.entity.User;
import com.redis.webhook.entity.WebhookDelivery;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.repository.NotificationRepository;
import com.redis.user.repository.UserRepository;
import com.redis.webhook.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryScheduler {

    private final WebhookDeliveryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationQueueService queueService;
    private final UserRepository userRepository;

    @Scheduled(fixedDelayString = "${app.webhook.retry-interval-ms:30000}")
    @Transactional
    public void processRetries() {
        List<WebhookDelivery> retryCandidates = deliveryRepository.findRetryCandidates();
        if (retryCandidates.isEmpty()) {
            return;
        }

        log.info("Found {} webhook deliveries to retry...", retryCandidates.size());

        User systemUser = userRepository.findByEmail("superadmin@ecommerce.local").orElse(null);
        if (systemUser == null) {
            systemUser = userRepository.findAll().stream().findFirst().orElse(null);
        }

        if (systemUser == null) {
            return;
        }

        for (WebhookDelivery delivery : retryCandidates) {
            try {
                // Re-enqueue retry candidate
                Notification n = Notification.builder()
                        .user(systemUser)
                        .title("Webhook Retry: " + delivery.getEventType().name())
                        .message(String.valueOf(delivery.getId()))
                        .type(NotificationType.SYSTEM)
                        .channel(NotificationChannel.WEBHOOK)
                        .priority(NotificationPriority.MEDIUM)
                        .status(NotificationStatus.PENDING)
                        .build();

                n = notificationRepository.save(n);
                queueService.enqueue(n.getId());
                
                log.info("Re-enqueued webhook delivery ID {} for retry attempt", delivery.getId());
            } catch (Exception e) {
                log.error("Failed to enqueue retry for delivery: {}", delivery.getId(), e);
            }
        }
    }
}
