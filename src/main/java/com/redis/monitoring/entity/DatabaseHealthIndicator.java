package com.redis.monitoring.entity;

import com.redis.monitoring.service.HealthIndicatorService;

import com.redis.reliability.dto.ModuleHealthResponse;
import com.redis.monitoring.event.MonitoringEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseHealthIndicator implements HealthIndicatorService, org.springframework.boot.actuate.health.HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String getName() {
        return "Database";
    }

    @Override
    public ModuleHealthResponse checkHealth() {
        Map<String, Object> details = new HashMap<>();
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (result != null && result == 1) {
                details.put("connection", "ESTABLISHED");
                details.put("ping", "SUCCESS");
                return ModuleHealthResponse.builder()
                        .moduleName(getName())
                        .status("UP")
                        .message("Database is connected and healthy")
                        .details(details)
                        .build();
            } else {
                eventPublisher.publishEvent(new MonitoringEvent(this, "DATABASE_UNAVAILABLE", getName(), "Database returned invalid ping result"));
                return ModuleHealthResponse.builder()
                        .moduleName(getName())
                        .status("DOWN")
                        .message("Database returned invalid ping result")
                        .details(details)
                        .build();
            }
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            details.put("error", e.getMessage());
            eventPublisher.publishEvent(new MonitoringEvent(this, "DATABASE_UNAVAILABLE", getName(), "Database is unavailable: " + e.getMessage()));
            return ModuleHealthResponse.builder()
                    .moduleName(getName())
                    .status("DOWN")
                    .message("Database is unavailable: " + e.getMessage())
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
