package com.redis.notification.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String type;
    private String channel;
    private String priority;
    private String status;
    private boolean readStatus;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
    private LocalDateTime resolvedAt;
    private int retryCount;
    private String failureReason;
}
