package com.redis.notification.entity;

import com.redis.user.entity.User;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notifications_user_id", columnList = "user_id"),
        @Index(name = "idx_notifications_status", columnList = "status"),
        @Index(name = "idx_notifications_created_at", columnList = "created_at"),
        @Index(name = "idx_notifications_user_read", columnList = "user_id, read_status"),
        @Index(name = "idx_notifications_status_created", columnList = "status, created_at"),
        @Index(name = "idx_notifications_status_next_retry", columnList = "status, next_retry_at")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "user")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Notification extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User association cannot be null")
    private User user;

    @Column(name = "title", nullable = false, length = 255)
    @NotBlank(message = "Title cannot be blank")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Message content cannot be blank")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50, columnDefinition = "varchar(50)")
    @NotNull(message = "Notification type cannot be null")
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 50, columnDefinition = "varchar(50)")
    @NotNull(message = "Notification channel cannot be null")
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20, columnDefinition = "varchar(20)")
    @NotNull(message = "Notification priority cannot be null")
    private NotificationPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30, columnDefinition = "varchar(30)")
    @NotNull(message = "Notification status cannot be null")
    private NotificationStatus status;

    @Column(name = "read_status", nullable = false)
    @Builder.Default
    private boolean readStatus = false;

    

    

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "last_retry_at")
    private LocalDateTime lastRetryAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "last_error_stack", columnDefinition = "TEXT")
    private String lastErrorStack;
}