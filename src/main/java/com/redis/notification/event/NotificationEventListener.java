package com.redis.notification.event;

import com.redis.notification.entity.NotificationChannel;
import com.redis.product.entity.Product;
import com.redis.audit.entity.AuditStatus;
import com.redis.notification.service.UserNotificationPreferenceService;
import com.redis.notification.dto.response.NotificationPreferenceResponse;
import com.redis.notification.entity.NotificationType;
import com.redis.notification.service.NotificationRetryService;
import com.redis.notification.service.NotificationChannelService;
import com.redis.notification.service.NotificationRateLimitService;
import com.redis.notification.repository.NotificationTemplateRepository;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.notification.entity.NotificationChannelFactory;
import com.redis.infrastructure.config.NotificationProperties;
import com.redis.audit.entity.AuditActionType;
import com.redis.notification.service.NotificationQueueService;

import com.redis.notification.event.NotificationEvent;
import com.redis.notification.dto.response.NotificationResponse;
import com.redis.notification.entity.Notification;
import com.redis.notification.entity.NotificationTemplateEntity;
import com.redis.user.entity.User;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.repository.NotificationRepository;
import com.redis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationChannelFactory channelFactory;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRetryService retryService;
    private final com.redis.infrastructure.config.NotificationProperties properties;
    private final NotificationQueueService queueService;
    private final UserNotificationPreferenceService preferenceService;
    private final NotificationRateLimitService rateLimitService;
    private final com.redis.notification.repository.NotificationTemplateRepository templateRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    /**
     * Listens to published NotificationEvent subclasses asynchronously.
     * Persists in PENDING status, updates to DELIVERING, triggers channel strategy send,
     * and records delivery outcome (SENT/FAILED) without interrupting calling transaction scope.
     * Pushes real-time WebSocket updates immediately after initial persistence.
     * Logs observability metrics at the end of execution.
     */
    @Async("notificationAsyncExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNotificationEvent(NotificationEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("Received notification event in thread: {}. Processing for user ID: {}",
                Thread.currentThread().getName(), event.getUserId());

        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + event.getUserId()));

        boolean optedOut = false;
        if (properties.isPreferenceValidationEnabled() 
                && event.getType() != com.redis.notification.entity.NotificationType.SECURITY
                && event.getType() != com.redis.notification.entity.NotificationType.SYSTEM_ALERT
                && event.getType() != com.redis.notification.entity.NotificationType.INCIDENT
                && event.getType() != com.redis.notification.entity.NotificationType.OPERATIONAL) {
            com.redis.notification.dto.response.NotificationPreferenceResponse prefs = preferenceService.getPreferences(user.getId());
            optedOut = switch (event.getChannel()) {
                case EMAIL -> !prefs.isEmailEnabled();
                case SMS -> !prefs.isSmsEnabled();
                case PUSH -> !prefs.isPushEnabled();
                case IN_APP -> !prefs.isInAppEnabled();
                default -> false;
            };
        }

        boolean rateLimited = false;
        if (!optedOut && properties.isRateLimitingEnabled()) {
            rateLimited = !rateLimitService.shouldSend(user.getId(), event.getType(), event.getChannel());
        }

        NotificationStatus finalStatus = NotificationStatus.PENDING;
        String skipReason = null;
        if (optedOut) {
            finalStatus = NotificationStatus.SKIPPED;
            skipReason = "User opted out of " + event.getChannel() + " notifications";
            log.info("OBSERVABILITY - DELIVERY_SKIPPED: userId={}, channel={}, reason={}", user.getId(), event.getChannel(), skipReason);
        } else if (rateLimited) {
            finalStatus = NotificationStatus.SKIPPED;
            skipReason = "Rate limit exceeded for " + event.getChannel() + " channel";
            log.info("OBSERVABILITY - DELIVERY_SKIPPED: userId={}, channel={}, reason={}", user.getId(), event.getChannel(), skipReason);
        }

        String renderedTitle = event.getTitle();
        String renderedMessage = event.getMessage();

        if (finalStatus == NotificationStatus.PENDING && properties.isTemplateManagementEnabled()) {
            java.util.Optional<NotificationTemplateEntity> templateOpt = templateRepository
                    .findActiveByTypeAndChannel(event.getType(), event.getChannel());
            if (templateOpt.isPresent()) {
                NotificationTemplateEntity template = templateOpt.get();
                try {
                    org.thymeleaf.spring6.SpringTemplateEngine engine = new org.thymeleaf.spring6.SpringTemplateEngine();
                    engine.setDialect(new org.thymeleaf.spring6.dialect.SpringStandardDialect());
                    org.thymeleaf.templateresolver.StringTemplateResolver resolver = new org.thymeleaf.templateresolver.StringTemplateResolver();
                    engine.setTemplateResolver(resolver);

                    org.thymeleaf.context.Context context = new org.thymeleaf.context.Context();
                    context.setVariable("user", user);
                    context.setVariable("username", user.getUsername());
                    context.setVariable("customerName", user.getUsername());
                    context.setVariable("email", user.getEmail());
                    context.setVariable("title", event.getTitle());
                    context.setVariable("message", event.getMessage());
                    context.setVariable("orderId", 12345L);
                    context.setVariable("totalAmount", new java.math.BigDecimal("99.99"));
                    context.setVariable("paymentAmount", new java.math.BigDecimal("99.99"));
                    context.setVariable("paymentGateway", "Stripe");
                    context.setVariable("paymentId", 500L);
                    context.setVariable("refundAmount", new java.math.BigDecimal("99.99"));
                    context.setVariable("productList", java.util.List.of("Product A", "Product B"));
                    context.setVariable("companyName", "E-Commerce Corp");
                    context.setVariable("supportContact", "support@ecommerce.com");

                    renderedTitle = engine.process(template.getSubject(), context);
                    if (event.getChannel() == com.redis.notification.entity.NotificationChannel.EMAIL && template.getHtmlTemplate() != null) {
                        renderedMessage = engine.process(template.getHtmlTemplate(), context);
                    } else if (template.getTextTemplate() != null) {
                        renderedMessage = engine.process(template.getTextTemplate(), context);
                    }
                    log.info("OBSERVABILITY - TEMPLATE_RESOLVED: templateCode={}, userId={}", template.getTemplateCode(), user.getId());
                } catch (Exception e) {
                    log.error("Failed to render template {} for user {}: {}", template.getTemplateCode(), user.getId(), e.getMessage(), e);
                }
            }
        }

        // 1. Persist initial Notification record
        Notification notification = Notification.builder()
                .user(user)
                .title(renderedTitle)
                .message(renderedMessage)
                .type(event.getType())
                .channel(event.getChannel())
                .priority(event.getPriority())
                .status(finalStatus)
                .readStatus(false)
                .failureReason(skipReason)
                .build();

        notification = notificationRepository.saveAndFlush(notification);
        log.info("Notification successfully persisted to DB in {} status. ID: {}", notification.getStatus(), notification.getId());
        log.info("Notification stored: ID={}, status={}", notification.getId(), notification.getStatus());

        // 1b. Push real-time update over WebSocket channel (failsafe)
        try {
            NotificationResponse wsPayload = NotificationResponse.builder()
                    .id(notification.getId())
                    .userId(user.getId())
                    .title(notification.getTitle())
                    .message(notification.getMessage())
                    .type(notification.getType().name())
                    .channel(notification.getChannel().name())
                    .priority(notification.getPriority().name())
                    .status(notification.getStatus().name())
                    .readStatus(notification.isReadStatus())
                    .createdAt(notification.getCreatedAt())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/notifications",
                    wsPayload
            );
            log.info("Successfully pushed notification over WebSocket to user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("WebSocket push failed for user {}: {}", user.getUsername(), e.getMessage(), e);
        }

        if (notification.getStatus() == NotificationStatus.SKIPPED) {
            log.info("Notification ID {} is SKIPPED. Exiting event listener.", notification.getId());
            return;
        }

        if (properties.isQueueEnabled()) {
            queueService.enqueue(notification.getId());
            log.info("Enqueued notification ID {} to queue. Exiting event listener.", notification.getId());
            return;
        }

        // 2. Transition status to DELIVERING and flush immediately
        notification.setStatus(NotificationStatus.DELIVERING);
        notification = notificationRepository.saveAndFlush(notification);
        log.info("Notification transitioned to DELIVERING. ID: {}", notification.getId());

        // 3. Attempt delivery via selected channel strategy
        try {
            NotificationChannelService deliveryService = channelFactory.getService(event.getChannel());
            deliveryService.send(notification);

            // 4. Update status to SENT on successful delivery
            notification.setStatus(NotificationStatus.SENT);
            notification.setDeliveredAt(LocalDateTime.now());
            notification = notificationRepository.saveAndFlush(notification);
            log.info("Notification successfully sent and transitioned to SENT. ID: {}", notification.getId());

            if (auditEventPublisher != null) {
                auditEventPublisher.publish(user.getId(), user.getEmail(), com.redis.audit.entity.AuditActionType.NOTIFICATION_SENT, com.redis.audit.entity.AuditStatus.SUCCESS,
                        com.redis.common.entity.ResourceType.NOTIFICATION, String.valueOf(notification.getId()), "Notification sent successfully: " + notification.getTitle());
            }
        } catch (Exception e) {
            // 5. Delegate failure and retry scheduling to NotificationRetryService
            log.error("Failed to deliver notification ID {}: {}", notification.getId(), e.getMessage(), e);
            
            if (auditEventPublisher != null) {
                auditEventPublisher.publish(user.getId(), user.getEmail(), com.redis.audit.entity.AuditActionType.NOTIFICATION_FAILED, com.redis.audit.entity.AuditStatus.FAILED,
                        com.redis.common.entity.ResourceType.NOTIFICATION, String.valueOf(notification.getId()), "Failed to deliver notification: " + e.getMessage());
            }

            try {
                retryService.scheduleRetry(notification, e);
            } catch (Exception retryEx) {
                log.error("Failed to delegate retry scheduling for notification ID: {}", notification.getId(), retryEx);
            }
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            log.info("OBSERVABILITY - Notification processed: ID={}, EventType={}, Channel={}, Priority={}, ExecutionTime={}ms, DeliveryResult={}",
                    notification.getId(), event.getClass().getSimpleName(), notification.getChannel(), notification.getPriority(), executionTime, notification.getStatus());
        }
    }
}
