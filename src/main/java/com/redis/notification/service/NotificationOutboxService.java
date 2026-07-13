package com.redis.notification.service;

import com.redis.notification.event.NotificationEvent;
import com.redis.notification.entity.NotificationOutbox;
import java.util.List;

public interface NotificationOutboxService {
    void saveEvent(NotificationEvent event);
    List<NotificationOutbox> getPendingEvents(int batchSize);
    void processOutboxBatch(List<NotificationOutbox> outboxRecords);
}
