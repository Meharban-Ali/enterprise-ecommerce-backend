package com.redis.infrastructure.events;

import com.redis.notification.event.NotificationEventBus;

import com.redis.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalEventBus implements NotificationEventBus {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void publish(NotificationEvent event) {
        log.info("Publishing event to Local Spring Event Bus: {}", event.getClass().getSimpleName());
        eventPublisher.publishEvent(event);
    }

    @Override
    public void publishBatch(List<NotificationEvent> events) {
        if (events != null) {
            events.forEach(this::publish);
        }
    }

    @Override
    public boolean supports(String type) {
        return "LOCAL".equalsIgnoreCase(type);
    }
}
