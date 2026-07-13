package com.redis.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationTypeStatisticsResponse {
    private String notificationType;
    private long count;
    private double successRate;
}
