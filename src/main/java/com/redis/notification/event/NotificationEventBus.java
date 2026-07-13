package com.redis.notification.event;

import com.redis.notification.event.NotificationEvent;
import java.util.List;

public interface NotificationEventBus {
    void publish(NotificationEvent event);
    void publishBatch(List<NotificationEvent> events);
    boolean supports(String type);
}
