package com.redis.notification.entity;

import com.redis.common.base.BaseEntity;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_templates", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"template_code", "version"})
}, indexes = {
        @Index(name = "idx_template_code", columnList = "template_code"),
        @Index(name = "idx_template_type", columnList = "notification_type"),
        @Index(name = "idx_template_channel", columnList = "notification_channel"),
        @Index(name = "idx_template_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationTemplateEntity extends BaseEntity {

    @Column(name = "template_code", nullable = false, length = 50)
    private String templateCode;

    @Column(name = "template_name", nullable = false, length = 100)
    private String templateName;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30, columnDefinition = "varchar(30)")
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_channel", nullable = false, length = 30, columnDefinition = "varchar(30)")
    private NotificationChannel notificationChannel;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "html_template", columnDefinition = "TEXT")
    private String htmlTemplate;

    @Column(name = "text_template", columnDefinition = "TEXT")
    private String textTemplate;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    @lombok.Builder.Default
    private int version = 1;

    @Column(name = "created_at", updatable = false)
    @lombok.Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @lombok.Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "created_by", updatable = false)
    @lombok.Builder.Default
    private String createdBy = "system";

    @Column(name = "updated_by")
    @lombok.Builder.Default
    private String updatedBy = "system";
}