package com.redis.notification.entity;

import com.redis.webhook.service.WebhookService;
import com.redis.notification.service.NotificationRetryService;
import com.redis.notification.service.NotificationChannelService;
import com.redis.notification.service.NotificationQueueService;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.notification.entity.Notification;
import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.repository.NotificationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationQueueWorker {

    private final NotificationQueueService queueService;
    private final NotificationRepository notificationRepository;
    private final NotificationChannelFactory channelFactory;
    private final NotificationRetryService retryService;
    private final NotificationProperties properties;
    private final ObjectProvider<WebhookService> webhookServiceProvider;

    private ExecutorService executorService;
    private volatile boolean running = true;
    private final String workerId = UUID.randomUUID().toString();

    @PostConstruct
    public void start() {
        if (!properties.isQueueEnabled()) {
            log.info("Notification Queue Worker is disabled.");
            return;
        }

        int threads = properties.getWorkerThreads();
        log.info("Starting Notification Queue Worker with {} threads. WorkerId: {}", threads, workerId);
        executorService = Executors.newFixedThreadPool(threads);
        running = true;

        for (int i = 0; i < threads; i++) {
            executorService.submit(this::runWorker);
        }
    }

    private void runWorker() {
        while (running) {
            try {
                Long notificationId = queueService.dequeue();
                if (notificationId == null) {
                    Thread.sleep(200);
                    continue;
                }

                log.info("OBSERVABILITY - QUEUE_CONSUMED: workerId={}, notificationId={}", workerId, notificationId);
                processNotification(notificationId);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Error in queue worker execution loop: {}", e.getMessage(), e);
            }
        }
    }

    private void processNotification(Long notificationId) {
        long startTime = System.currentTimeMillis();
        try {
            Notification notification = notificationRepository.findByIdWithUser(notificationId).orElse(null);
            if (notification == null) {
                log.warn("Notification entity not found for ID: {}", notificationId);
                queueService.acknowledge(notificationId);
                return;
            }

            int updated = notificationRepository.transitionToDelivering(notificationId);
            if (updated == 0) {
                log.warn("Notification ID {} already claimed or not in valid state for delivery", notificationId);
                queueService.acknowledge(notificationId);
                return;
            }

            notification = notificationRepository.findByIdWithUser(notificationId).orElse(notification);

            // Webhook delivery interception routing
            if (notification.getChannel() == NotificationChannel.WEBHOOK) {
                try {
                    WebhookService ws = webhookServiceProvider.getIfAvailable();
                    if (ws != null) {
                        ws.executeDelivery(notificationId);
                    }
                    notification.setStatus(NotificationStatus.SENT);
                    notificationRepository.saveAndFlush(notification);
                    queueService.acknowledge(notificationId);
                    log.info("Webhook Notification ID {} sent successfully via worker. ExecutionTime: {}ms",
                            notificationId, System.currentTimeMillis() - startTime);
                } catch (Exception e) {
                    log.error("Failed to deliver webhook notification ID {}: {}", notificationId, e.getMessage(), e);
                    queueService.acknowledge(notificationId);
                }
                return;
            }

            try {
                NotificationChannelService deliveryService = channelFactory.getService(notification.getChannel());
                deliveryService.send(notification);

                notification.setStatus(NotificationStatus.SENT);
                notification.setDeliveredAt(LocalDateTime.now());
                notificationRepository.saveAndFlush(notification);

                queueService.acknowledge(notificationId);
                log.info("Notification ID {} sent successfully via worker. ExecutionTime: {}ms",
                        notificationId, System.currentTimeMillis() - startTime);

            } catch (Exception e) {
                log.error("Worker failed to deliver notification ID {}: {}", notificationId, e.getMessage());
                retryService.scheduleRetry(notification, e);
                queueService.acknowledge(notificationId);
            }

        } catch (Exception e) {
            log.error("Fatal error processing notification ID {}: {}", notificationId, e.getMessage(), e);
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }
}
