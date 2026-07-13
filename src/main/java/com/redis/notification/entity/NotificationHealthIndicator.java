package com.redis.notification.entity;

import com.redis.monitoring.service.HealthIndicatorService;

import com.redis.reliability.dto.ModuleHealthResponse;
import com.redis.notification.entity.NotificationStatus;
import com.redis.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHealthIndicator implements HealthIndicatorService, org.springframework.boot.actuate.health.HealthIndicator {

    private final NotificationRepository notificationRepository;

    @Override
    public String getName() {
        return "Notifications";
    }

    @Override
    public ModuleHealthResponse checkHealth() {
        Map<String, Object> details = new HashMap<>();
        try {
            long pendingCount = notificationRepository.countByStatus(NotificationStatus.PENDING);
            long failedCount = notificationRepository.countByStatus(NotificationStatus.FAILED);
            long dlqCount = notificationRepository.countByStatusAndRetryCountGreaterThanEqual(NotificationStatus.FAILED, 3);
            long sentCount = notificationRepository.countByStatus(NotificationStatus.SENT);

            details.put("pendingRetryCount", pendingCount);
            details.put("failedCount", failedCount);
            details.put("dlqCount", dlqCount);
            details.put("sentCount", sentCount);

            String status = "UP";
            String message = "Notification service is healthy";

            // High retry queue rule: if pending retry queue size is > 50, transition to WARNING
            if (pendingCount > 50) {
                status = "WARNING";
                message = "High volume of pending notifications in retry queue: " + pendingCount;
            }

            return ModuleHealthResponse.builder()
                    .moduleName(getName())
                    .status(status)
                    .message(message)
                    .details(details)
                    .build();
        } catch (Exception e) {
            log.error("Notification health check failed: {}", e.getMessage());
            details.put("error", e.getMessage());
            return ModuleHealthResponse.builder()
                    .moduleName(getName())
                    .status("WARNING") // Internal module exception -> WARNING
                    .message("Notification health check degraded: " + e.getMessage())
                    .details(details)
                    .build();
        }
    }

    @Override
    public org.springframework.boot.actuate.health.Health health() {
        ModuleHealthResponse res = checkHealth();
        org.springframework.boot.actuate.health.Health.Builder builder;
        if ("UP".equalsIgnoreCase(res.getStatus())) {
            builder = org.springframework.boot.actuate.health.Health.up();
        } else if ("DEGRADED".equalsIgnoreCase(res.getStatus())) {
            builder = org.springframework.boot.actuate.health.Health.status("DEGRADED");
        } else if ("WARNING".equalsIgnoreCase(res.getStatus())) {
            builder = org.springframework.boot.actuate.health.Health.status("WARNING");
        } else {
            builder = org.springframework.boot.actuate.health.Health.down();
        }
        if (res.getMessage() != null) {
            builder.withDetail("message", res.getMessage());
        }
        if (res.getDetails() != null) {
            builder.withDetails(res.getDetails());
        }
        return builder.build();
    }
}
