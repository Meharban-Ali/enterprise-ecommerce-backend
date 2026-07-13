package com.redis.notification.event;

import com.redis.infrastructure.config.NotificationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationEventBusFactory {

    private final List<NotificationEventBus> eventBuses;
    private final NotificationProperties properties;

    public NotificationEventBus getEventBus() {
        String type = properties.getEventBusType();
        return eventBuses.stream()
                .filter(bus -> bus.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported event bus type: " + type));
    }
}
