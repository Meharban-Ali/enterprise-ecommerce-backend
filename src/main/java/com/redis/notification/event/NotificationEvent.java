package com.redis.notification.event;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public abstract class NotificationEvent extends ApplicationEvent {
    private final Long userId;
    private final String title;
    private final String message;
    private final NotificationChannel channel;
    private final NotificationPriority priority;
    private final NotificationType type;

    protected NotificationEvent(Object source, Long userId, String title, String message,
                                NotificationChannel channel, NotificationPriority priority, NotificationType type) {
        super(source);
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.channel = channel;
        this.priority = priority;
        this.type = type;
    }
}
