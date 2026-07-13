package com.redis.notification.event;

import com.redis.notification.event.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class KafkaNotificationEventBus implements NotificationEventBus {

    @Override
    public void publish(NotificationEvent event) {
        log.info("OBSERVABILITY - KAFKA_READY: Mock publishing event to Kafka. Type={}, UserId={}, Title={}",
                event.getType(), event.getUserId(), event.getTitle());
    }

    @Override
    public void publishBatch(List<NotificationEvent> events) {
        if (events != null) {
            events.forEach(this::publish);
        }
    }

    @Override
    public boolean supports(String type) {
        return "KAFKA".equalsIgnoreCase(type);
    }
}
