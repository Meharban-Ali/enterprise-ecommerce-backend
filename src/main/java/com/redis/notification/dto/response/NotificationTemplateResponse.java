package com.redis.notification.dto.response;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateResponse {
    private Long id;
    private String templateCode;
    private String templateName;
    private NotificationType notificationType;
    private NotificationChannel notificationChannel;
    private String subject;
    private String htmlTemplate;
    private String textTemplate;
    private int version;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
