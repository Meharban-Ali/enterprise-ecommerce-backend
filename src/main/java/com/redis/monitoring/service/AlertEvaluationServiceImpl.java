package com.redis.monitoring.service;

import com.redis.product.repository.ProductRepository;
import com.redis.payment.entity.PaymentExpirationScheduler;
import com.redis.payment.repository.PaymentRepository;
import com.redis.monitoring.entity.AlertStatus;
import com.redis.notification.repository.NotificationRepository;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.event.NotificationEventPublisher;
import com.redis.notification.entity.NotificationOutboxScheduler;
import com.redis.incident.entity.Incident;
import com.redis.payment.entity.PaymentStatus;
import com.redis.incident.entity.IncidentTimeline;
import com.redis.monitoring.repository.AlertRuleRepository;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.notification.entity.NotificationRetryScheduler;
import com.redis.common.entity.EscalationLevel;
import com.redis.user.entity.User;
import com.redis.monitoring.entity.AlertRule;
import com.redis.incident.repository.IncidentTimelineRepository;
import com.redis.incident.repository.IncidentRepository;

import com.redis.monitoring.entity.DatabaseHealthIndicator;
import com.redis.monitoring.entity.RedisHealthIndicator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEvaluationServiceImpl implements AlertEvaluationService {

    private final AlertRuleRepository alertRuleRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    private final DatabaseHealthIndicator databaseHealthIndicator;
    private final RedisHealthIndicator redisHealthIndicator;
    private final NotificationRepository notificationRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    private final PaymentExpirationScheduler paymentExpirationScheduler;
    private final NotificationRetryScheduler notificationRetryScheduler;
    private final NotificationOutboxScheduler notificationOutboxScheduler;

    private final AtomicLong incidentSequence = new AtomicLong(System.currentTimeMillis() % 1000000);

    @Override
    @Transactional
    public void evaluateRules() {
        log.debug("Starting alert rules evaluation...");
        long startTime = System.currentTimeMillis();

        List<AlertRule> enabledRules = alertRuleRepository.findEnabledRules();
        for (AlertRule rule : enabledRules) {
            try {
                boolean triggered = checkThreshold(rule);
                processRuleOutcome(rule, triggered);
            } catch (Exception e) {
                log.error("Failed to evaluate alert rule: {}", rule.getRuleCode(), e);
            }
        }

        log.debug("Completed alert rules evaluation in {} ms", System.currentTimeMillis() - startTime);
    }

    private boolean checkThreshold(AlertRule rule) {
        switch (rule.getRuleCode()) {
            case "DB_DOWN":
                return !"UP".equals(databaseHealthIndicator.checkHealth().getStatus());

            case "REDIS_DOWN":
                String redisStatus = redisHealthIndicator.checkHealth().getStatus();
                return "DOWN".equals(redisStatus) || "DEGRADED".equals(redisStatus);

            case "SCHEDULER_FAIL":
                long schedulerFails = paymentExpirationScheduler.getStatusDetails().getFailuresCount() +
                        notificationRetryScheduler.getStatusDetails().getFailuresCount() +
                        notificationOutboxScheduler.getStatusDetails().getFailuresCount();
                return schedulerFails > rule.getThreshold();

            case "NOTIF_FAIL_RATE":
                long totalNotif = notificationRepository.count();
                if (totalNotif == 0) return false;
                long failedNotif = notificationRepository.countByStatus(com.redis.notification.entity.NotificationStatus.FAILED);
                double notifRate = (double) failedNotif / totalNotif;
                return notifRate > rule.getThreshold();

            case "PAYMENT_FAIL_RATE":
                long totalPayments = paymentRepository.count();
                if (totalPayments == 0) return false;
                long failedPayments = paymentRepository.countByStatus(com.redis.payment.entity.PaymentStatus.FAILED);
                double paymentRate = (double) failedPayments / totalPayments;
                return paymentRate > rule.getThreshold();

            case "DISK_USAGE_HIGH":
                File file = new File(".");
                long totalSpace = file.getTotalSpace();
                long freeSpace = file.getFreeSpace();
                double usedPercent = totalSpace > 0 ? ((double) (totalSpace - freeSpace) / totalSpace) * 100.0 : 0.0;
                return usedPercent > rule.getThreshold();

            case "MEMORY_USAGE_HIGH":
                long heapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
                long heapMax = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
                double memoryUtil = heapMax > 0 ? ((double) heapUsed / heapMax) * 100.0 : 0.0;
                return memoryUtil > rule.getThreshold();

            default:
                log.warn("Unknown alert rule code: {}", rule.getRuleCode());
                return false;
        }
    }

    private void processRuleOutcome(AlertRule rule, boolean triggered) {
        Optional<Incident> activeIncidentOpt = incidentRepository.findTopByAlertRuleIdOrderByCreatedAtDesc(rule.getId());

        if (triggered) {
            if (activeIncidentOpt.isPresent()) {
                Incident activeIncident = activeIncidentOpt.get();
                if (activeIncident.getStatus() == AlertStatus.OPEN || activeIncident.getStatus() == AlertStatus.ACKNOWLEDGED) {
                    // Duplicate suppression: update count and timestamp
                    activeIncident.setOccurrenceCount(activeIncident.getOccurrenceCount() + 1);
                    activeIncident.setLastOccurredAt(LocalDateTime.now());
                    incidentRepository.save(activeIncident);
                    log.debug("Suppressed duplicate alert for rule {}. Occurrence count: {}", rule.getRuleCode(), activeIncident.getOccurrenceCount());
                } else if (activeIncident.getStatus() == AlertStatus.RESOLVED) {
                    // Automatic incident reopening
                    activeIncident.setStatus(AlertStatus.OPEN);
                    activeIncident.setLastOccurredAt(LocalDateTime.now());
                    activeIncident.setOccurrenceCount(activeIncident.getOccurrenceCount() + 1);
                    incidentRepository.save(activeIncident);

                    IncidentTimeline timeline = IncidentTimeline.builder()
                            .incident(activeIncident)
                            .previousStatus(AlertStatus.RESOLVED)
                            .newStatus(AlertStatus.OPEN)
                            .previousSeverity(activeIncident.getSeverity())
                            .newSeverity(activeIncident.getSeverity())
                            .actionPerformedBy("SYSTEM")
                            .actionSource("SYSTEM")
                            .remarks("Incident auto-reopened by rules evaluator.")
                            .build();
                    incidentTimelineRepository.save(timeline);

                    if (rule.isNotificationEnabled()) {
                        notificationEventPublisher.publishHighSeverityIncident(
                                "REOPENED: " + activeIncident.getTitle(),
                                "Incident " + activeIncident.getIncidentNumber() + " has been auto-reopened due to recurring threshold breaches."
                        );
                    }
                } else {
                    // Closed or other: create new incident
                    createNewIncident(rule);
                }
            } else {
                // First occurrence: create new incident
                createNewIncident(rule);
            }
        } else {
            // Evaluated as healthy: auto-resolve open/acknowledged incidents
            if (activeIncidentOpt.isPresent()) {
                Incident activeIncident = activeIncidentOpt.get();
                if (activeIncident.getStatus() == AlertStatus.OPEN || activeIncident.getStatus() == AlertStatus.ACKNOWLEDGED) {
                    AlertStatus prevStatus = activeIncident.getStatus();
                    activeIncident.setStatus(AlertStatus.RESOLVED);
                    activeIncident.setResolvedAt(LocalDateTime.now());
                    activeIncident.setResolvedBy("SYSTEM");
                    activeIncident.setResolvedWithinSla(LocalDateTime.now().isBefore(activeIncident.getSlaDeadline()));
                    incidentRepository.save(activeIncident);

                    IncidentTimeline timeline = IncidentTimeline.builder()
                            .incident(activeIncident)
                            .previousStatus(prevStatus)
                            .newStatus(AlertStatus.RESOLVED)
                            .previousSeverity(activeIncident.getSeverity())
                            .newSeverity(activeIncident.getSeverity())
                            .actionPerformedBy("SYSTEM")
                            .actionSource("SYSTEM")
                            .remarks("Incident auto-resolved as systems returned to normal.")
                            .build();
                    incidentTimelineRepository.save(timeline);

                    if (rule.isNotificationEnabled()) {
                        notificationEventPublisher.publishIncidentResolved(
                                "RESOLVED: " + activeIncident.getTitle(),
                                "Incident " + activeIncident.getIncidentNumber() + " has been auto-resolved by system evaluation."
                        );
                    }
                }
            }
        }
    }

    private void createNewIncident(AlertRule rule) {
        String incNum = String.format("INC-%d-%06d", LocalDate.now().getYear(), incidentSequence.incrementAndGet());
        LocalDateTime now = LocalDateTime.now();

        // Calculate deadlines
        LocalDateTime slaDeadline;
        LocalDateTime ackDeadline;
        if (rule.getSeverity() == AlertSeverity.CRITICAL) {
            slaDeadline = now.plusHours(2);
            ackDeadline = now.plusMinutes(15);
        } else if (rule.getSeverity() == AlertSeverity.HIGH) {
            slaDeadline = now.plusHours(4);
            ackDeadline = now.plusMinutes(30);
        } else {
            slaDeadline = now.plusHours(24);
            ackDeadline = now.plusHours(2);
        }

        Incident incident = Incident.builder()
                .incidentNumber(incNum)
                .alertRule(rule)
                .severity(rule.getSeverity())
                .source(rule.getSource())
                .title("Threshold breached: " + rule.getRuleName())
                .description("Threshold value " + rule.getThreshold() + " exceeded during rules evaluation.")
                .status(AlertStatus.OPEN)
                .firstOccurredAt(now)
                .lastOccurredAt(now)
                .occurrenceCount(1)
                .slaDeadline(slaDeadline)
                .slaBreached(false)
                .acknowledgementDeadline(ackDeadline)
                .escalationLevel(EscalationLevel.L1)
                .build();

        incident = incidentRepository.save(incident);

        IncidentTimeline timeline = IncidentTimeline.builder()
                .incident(incident)
                .previousStatus(null)
                .newStatus(AlertStatus.OPEN)
                .previousSeverity(null)
                .newSeverity(incident.getSeverity())
                .actionPerformedBy("SYSTEM")
                .actionSource("SYSTEM")
                .remarks("Incident created by rules evaluator.")
                .build();
        incidentTimelineRepository.save(timeline);

        if (rule.isNotificationEnabled()) {
            if (incident.getSeverity() == AlertSeverity.CRITICAL) {
                notificationEventPublisher.publishCriticalAlert(
                        "CRITICAL ALERT: " + incident.getTitle(),
                        "Incident " + incident.getIncidentNumber() + " has been opened with CRITICAL severity. Immediate attention required."
                );
            } else {
                notificationEventPublisher.publishHighSeverityIncident(
                        "ALERT: " + incident.getTitle(),
                        "Incident " + incident.getIncidentNumber() + " has been opened with severity " + incident.getSeverity()
                );
            }
        }
        
        log.info("INCIDENT_EVENT | IncidentId={} | Severity={} | Status=OPEN | Source={} | User=SYSTEM | Timestamp={}",
                incident.getIncidentNumber(), incident.getSeverity(), incident.getSource(), now);
    }
}
