package com.redis.monitoring.service;

import com.redis.reliability.dto.SchedulerStatusResponse;
import com.redis.payment.entity.Payment;
import com.redis.observability.dto.response.JvmMetricsResponse;
import com.redis.notification.entity.Notification;
import com.redis.observability.dto.response.SystemMetricsResponse;
import com.redis.monitoring.dto.response.AlertResponse;
import com.redis.reliability.dto.ModuleHealthResponse;
import com.redis.reliability.dto.SystemHealthResponse;

import com.redis.infrastructure.config.MonitoringProperties;
import com.redis.monitoring.event.MonitoringEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringAlertServiceImpl implements MonitoringAlertService {

    private final MonitoringProperties monitoringProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public List<AlertResponse> evaluateAlertRules(
            SystemHealthResponse health,
            SystemMetricsResponse metrics,
            List<SchedulerStatusResponse> schedulers,
            JvmMetricsResponse jvmMetrics
    ) {
        List<AlertResponse> alerts = new ArrayList<>();

        // 1. Database Check
        if ("DOWN".equalsIgnoreCase(health.getDatabaseStatus())) {
            AlertResponse alert = AlertResponse.builder()
                    .ruleName("DATABASE_UNAVAILABLE")
                    .severity("CRITICAL")
                    .message("Database connectivity is lost!")
                    .timestamp(LocalDateTime.now())
                    .build();
            alerts.add(alert);
            eventPublisher.publishEvent(new MonitoringEvent(this, "ALERT_TRIGGERED", "Database", alert.getMessage()));
        }

        // 2. Redis Check
        if ("DOWN".equalsIgnoreCase(health.getRedisStatus()) || "DEGRADED".equalsIgnoreCase(health.getRedisStatus())) {
            AlertResponse alert = AlertResponse.builder()
                    .ruleName("REDIS_UNAVAILABLE")
                    .severity("WARNING")
                    .message("Redis cache connection is down or degraded.")
                    .timestamp(LocalDateTime.now())
                    .build();
            alerts.add(alert);
            eventPublisher.publishEvent(new MonitoringEvent(this, "ALERT_TRIGGERED", "Redis", alert.getMessage()));
        }

        // 3. Notification Failure Rate Check
        long totalNotif = metrics.getTotalNotifications();
        long failedNotif = metrics.getFailedNotificationsCount();
        if (totalNotif > 0) {
            double rate = (double) failedNotif / totalNotif;
            if (rate > monitoringProperties.getNotificationFailureWarningThreshold()) {
                AlertResponse alert = AlertResponse.builder()
                        .ruleName("NOTIFICATION_FAILURE_RATE_EXCEEDED")
                        .severity("WARNING")
                        .message(String.format("Notification failure rate of %.2f%% exceeds threshold of %.2f%%",
                                rate * 100, monitoringProperties.getNotificationFailureWarningThreshold() * 100))
                        .timestamp(LocalDateTime.now())
                        .build();
                alerts.add(alert);
                eventPublisher.publishEvent(new MonitoringEvent(this, "ALERT_TRIGGERED", "Notifications", alert.getMessage()));
            }
        }

        // 4. Payment Failure Rate Check
        long totalPayments = metrics.getTotalPayments();
        long failedPayments = metrics.getFailedNotificationsCount(); // default fallback
        
        // Try to get payment details from modules
        for (ModuleHealthResponse module : health.getModules()) {
            if ("Payments".equals(module.getModuleName()) && module.getDetails() != null) {
                Map<String, Object> details = module.getDetails();
                if (details.containsKey("failedPaymentsCount") && details.containsKey("totalPaymentsCount")) {
                    try {
                        failedPayments = ((Number) details.get("failedPaymentsCount")).longValue();
                        totalPayments = ((Number) details.get("totalPaymentsCount")).longValue();
                    } catch (Exception ignored) {}
                }
            }
        }

        if (totalPayments > 0) {
            double rate = (double) failedPayments / totalPayments;
            if (rate > monitoringProperties.getPaymentFailureWarningThreshold()) {
                AlertResponse alert = AlertResponse.builder()
                        .ruleName("PAYMENT_FAILURE_RATE_EXCEEDED")
                        .severity("CRITICAL")
                        .message(String.format("Payment failure rate of %.2f%% exceeds threshold of %.2f%%",
                                rate * 100, monitoringProperties.getPaymentFailureWarningThreshold() * 100))
                        .timestamp(LocalDateTime.now())
                        .build();
                alerts.add(alert);
                eventPublisher.publishEvent(new MonitoringEvent(this, "ALERT_TRIGGERED", "Payments", alert.getMessage()));
            }
        }

        // 5. Scheduler Warning Threshold
        for (SchedulerStatusResponse scheduler : schedulers) {
            if (scheduler.getLastExecutionDurationMs() > monitoringProperties.getSchedulerWarningThresholdMs()) {
                AlertResponse alert = AlertResponse.builder()
                        .ruleName("SCHEDULER_EXECUTION_EXCEEDED")
                        .severity("WARNING")
                        .message(String.format("Scheduler %s execution duration %d ms exceeds threshold of %d ms",
                                scheduler.getSchedulerName(), scheduler.getLastExecutionDurationMs(), monitoringProperties.getSchedulerWarningThresholdMs()))
                        .timestamp(LocalDateTime.now())
                        .build();
                alerts.add(alert);
                eventPublisher.publishEvent(new MonitoringEvent(this, "ALERT_TRIGGERED", scheduler.getSchedulerName(), alert.getMessage()));
            }
        }

        // 6. Disk Warning
        try {
            File file = new File(".");
            long totalSpace = file.getTotalSpace();
            long freeSpace = file.getFreeSpace();
            double freePercent = totalSpace > 0 ? ((double) freeSpace / totalSpace) * 100.0 : 100.0;
            if (freePercent < monitoringProperties.getLowDiskWarningPercentage()) {
                AlertResponse alert = AlertResponse.builder()
                        .ruleName("DISK_USAGE_WARNING")
                        .severity("WARNING")
                        .message(String.format("Free disk space of %.2f%% is below warning threshold of %.2f%%",
                                freePercent, monitoringProperties.getLowDiskWarningPercentage()))
                        .timestamp(LocalDateTime.now())
                        .build();
                alerts.add(alert);
                eventPublisher.publishEvent(new MonitoringEvent(this, "ALERT_TRIGGERED", "SystemDisk", alert.getMessage()));
            }
        } catch (Exception e) {
            log.warn("Failed to check disk usage alert rules: {}", e.getMessage());
        }

        // 7. Memory Warning
        if (jvmMetrics.getHeapUtilizationPercentage() > 90.0) {
            AlertResponse alert = AlertResponse.builder()
                    .ruleName("MEMORY_USAGE_WARNING")
                    .severity("WARNING")
                    .message(String.format("JVM Heap memory utilization is at %.2f%% (exceeds 90%% warning threshold)",
                            jvmMetrics.getHeapUtilizationPercentage()))
                    .timestamp(LocalDateTime.now())
                    .build();
            alerts.add(alert);
            eventPublisher.publishEvent(new MonitoringEvent(this, "ALERT_TRIGGERED", "Memory", alert.getMessage()));
        }

        return alerts;
    }
}
