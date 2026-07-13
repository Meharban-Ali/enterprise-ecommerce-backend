package com.redis.notification.dto.request;

import com.redis.notification.entity.Notification;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateRequest {

    @NotBlank(message = "Template code is required")
    private String templateCode;

    @NotBlank(message = "Template name is required")
    private String templateName;

    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    @NotNull(message = "Notification channel is required")
    private NotificationChannel notificationChannel;

    @NotBlank(message = "Subject is required")
    private String subject;

    private String htmlTemplate;

    private String textTemplate;
}
