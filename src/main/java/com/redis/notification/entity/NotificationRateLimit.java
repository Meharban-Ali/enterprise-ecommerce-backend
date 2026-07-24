package com.redis.notification.entity;

import com.redis.common.base.AuditableEntity;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.NotificationType;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_rate_limits", indexes = {
        @Index(name = "idx_rate_limit_user", columnList = "user_id"),
        @Index(name = "idx_rate_limit_composite", columnList = "user_id, notification_type, channel, window_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class NotificationRateLimit extends AuditableEntity {

    

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30, columnDefinition = "varchar(30)")
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, columnDefinition = "varchar(30)")
    private NotificationChannel channel;

    @Column(name = "window_type", nullable = false, length = 20)
    private String windowType; // HOURLY, DAILY

    @Column(nullable = false)
    private int counter;

    @Column(name = "window_start", nullable = false)
    private LocalDateTime windowStart;

    
}