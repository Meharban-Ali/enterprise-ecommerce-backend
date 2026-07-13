package com.redis.notification.service;

import com.redis.notification.entity.NotificationChannelFactory;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.notification.entity.Notification;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRetryServiceImpl implements NotificationRetryService {

    private final NotificationRepository notificationRepository;
    private final NotificationChannelFactory channelFactory;
    private final NotificationProperties notificationProperties;
    private final NotificationQueueService queueService;

    @Override
    @Transactional
    public void executeRetry(Long notificationId) {
        long startTime = System.currentTimeMillis();
        log.info("Executing scheduled retry for notification ID: {}", notificationId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        if (!shouldRetry(notification)) {
            log.warn("Notification ID {} is not eligible for retry. State: {}", notificationId, notification.getStatus());
            return;
        }

        try {
            notification.setStatus(NotificationStatus.RETRYING);
            notification.setLastRetryAt(LocalDateTime.now());
            notification = notificationRepository.saveAndFlush(notification);

            if (notificationProperties.isQueueEnabled()) {
                queueService.requeue(notificationId);
                log.info("Re-enqueued notification ID {} for worker retry processing.", notificationId);
                return;
            }

            try {
                NotificationChannelService deliveryService = channelFactory.getService(notification.getChannel());
                deliveryService.send(notification);

                notification.setStatus(NotificationStatus.SENT);
                notification.setDeliveredAt(LocalDateTime.now());
                notification.setResolvedAt(LocalDateTime.now());
                notification = notificationRepository.saveAndFlush(notification);
                log.info("Retry successful. Notification ID {} transitioned to SENT.", notificationId);
            } catch (Exception e) {
                log.error("Retry failed for notification ID {}: {}", notificationId, e.getMessage());
                scheduleRetry(notification, e);
            }
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("OBSERVABILITY - Notification retry processed: ID={}, Channel={}, RetryCount={}, ExecutionTime={}ms, FinalStatus={}, FailureReason={}",
                    notificationId, notification.getChannel(), notification.getRetryCount(), elapsed, notification.getStatus(), notification.getFailureReason());
        }
    }

    @Override
    @Transactional
    public void scheduleRetry(Notification notification, Throwable error) {
        int maxAttempts = notificationProperties.getRetry().getMaxAttempts();
        int currentRetries = notification.getRetryCount();

        // Save failure reason and stack trace safely
        notification.setFailureReason(error != null ? error.getMessage() : "Unknown delivery error");
        if (error != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            error.printStackTrace(pw);
            notification.setLastErrorStack(sw.toString());
        }

        if (currentRetries >= maxAttempts) {
            log.warn("Max retry attempts ({}) reached for notification ID: {}. Moving to DLQ.", maxAttempts, notification.getId());
            moveToDeadLetter(notification);
        } else {
            notification.setRetryCount(currentRetries + 1);
            notification.setStatus(NotificationStatus.FAILED);
            LocalDateTime nextRetry = calculateNextRetry(notification.getRetryCount());
            notification.setNextRetryAt(nextRetry);
            notificationRepository.saveAndFlush(notification);
            log.info("Scheduled retry #{} for notification ID {} at {}", notification.getRetryCount(), notification.getId(), nextRetry);
        }
    }

    @Override
    @Transactional
    public void moveToDeadLetter(Notification notification) {
        if (notificationProperties.getRetry().isDeadLetterEnabled()) {
            notification.setStatus(NotificationStatus.DEAD_LETTER);
            notificationRepository.saveAndFlush(notification);
            log.warn("Notification ID {} has been moved to DEAD_LETTER Queue (DLQ).", notification.getId());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.saveAndFlush(notification);
            log.info("DLQ is disabled. Notification ID {} remains FAILED.", notification.getId());
        }
    }

    @Override
    public LocalDateTime calculateNextRetry(int retryCount) {
        int initialDelayMinutes = notificationProperties.getRetry().getInitialDelayMinutes();
        int multiplier = notificationProperties.getRetry().getBackoffMultiplier();

        // Exponential backoff: initialDelay * (multiplier ^ (retryCount - 1))
        long delayMinutes = (long) (initialDelayMinutes * Math.pow(multiplier, retryCount - 1));
        return LocalDateTime.now().plusMinutes(delayMinutes);
    }

    @Override
    public boolean shouldRetry(Notification notification) {
        if (notification == null) return false;
        NotificationStatus status = notification.getStatus();
        // Eligible if FAILED or RETRYING (if a previous retry got interrupted)
        return status == NotificationStatus.FAILED || status == NotificationStatus.RETRYING;
    }
}
