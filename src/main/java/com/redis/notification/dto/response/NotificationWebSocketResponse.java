package com.redis.notification.dto.response;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationPriority;
import com.redis.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationWebSocketResponse {
    private final Long notificationId;
    private final String title;
    private final String message;
    private final NotificationType type;
    private final NotificationPriority priority;
    private final NotificationChannel channel;
    private final LocalDateTime createdAt;
}
