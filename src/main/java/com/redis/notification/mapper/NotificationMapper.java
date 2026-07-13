package com.redis.notification.mapper;

import com.redis.notification.dto.response.NotificationResponse;
import com.redis.notification.dto.response.NotificationSummaryResponse;
import com.redis.notification.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUser() != null ? notification.getUser().getId() : null)
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType().name())
                .channel(notification.getChannel().name())
                .priority(notification.getPriority().name())
                .status(notification.getStatus().name())
                .readStatus(notification.isReadStatus())
                .createdAt(notification.getCreatedAt())
                .deliveredAt(notification.getDeliveredAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
