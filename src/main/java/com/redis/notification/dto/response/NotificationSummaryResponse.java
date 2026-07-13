package com.redis.notification.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSummaryResponse {
    private long unreadCount;
    private List<NotificationResponse> recentNotifications;
    private LocalDateTime lastNotificationTime;
}
