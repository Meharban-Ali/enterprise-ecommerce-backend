package com.redis.monitoring.entity;

import com.redis.monitoring.service.HealthIndicatorService;

import com.redis.reliability.dto.ModuleHealthResponse;
import com.redis.monitoring.event.MonitoringEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicatorService, org.springframework.boot.actuate.health.HealthIndicator {

    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public String getName() {
        return "Redis";
    }

    @Override
    public ModuleHealthResponse checkHealth() {
        Map<String, Object> details = new HashMap<>();
        RedisConnectionFactory factory = redisConnectionFactoryProvider.getIfAvailable();
        if (factory == null) {
            details.put("cacheType", "SIMPLE/IN_MEMORY");
            details.put("message", "Redis auto-configuration is disabled; using in-memory cache");
            return ModuleHealthResponse.builder()
                    .moduleName(getName())
                    .status("UP")
                    .message("In-memory cache is active (Redis disabled)")
                    .details(details)
                    .build();
        }

        try (RedisConnection connection = factory.getConnection()) {
            if (connection == null) {
                throw new IllegalStateException("RedisConnection is null");
            }
            String pingResult = connection.ping();
            details.put("ping", pingResult);
            details.put("connection", "ESTABLISHED");
            return ModuleHealthResponse.builder()
                    .moduleName(getName())
                    .status("UP")
                    .message("Redis cache is available and healthy")
                    .details(details)
                    .build();
        } catch (Exception e) {
            log.error("Redis health check failed: {}", e.getMessage());
            details.put("error", e.getMessage());
            eventPublisher.publishEvent(new MonitoringEvent(this, "REDIS_UNAVAILABLE", getName(), "Redis is unavailable: " + e.getMessage()));
            return ModuleHealthResponse.builder()
                    .moduleName(getName())
                    .status("DEGRADED")
                    .message("Redis cache is unavailable: " + e.getMessage())
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
