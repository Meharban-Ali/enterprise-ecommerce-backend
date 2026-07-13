package com.redis.monitoring.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class MonitoringEvent extends ApplicationEvent {
    private final String eventType; // e.g. "SCHEDULER_FAILURE", "HEALTH_CHECK_TIMEOUT", "DATABASE_UNAVAILABLE", "REDIS_UNAVAILABLE", "ALERT_TRIGGERED"
    private final String sourceComponent;
    private final String message;
    private final LocalDateTime eventTimestamp;

    public MonitoringEvent(Object source, String eventType, String sourceComponent, String message) {
        super(source);
        this.eventType = eventType;
        this.sourceComponent = sourceComponent;
        this.message = message;
        this.eventTimestamp = LocalDateTime.now();
    }
}
