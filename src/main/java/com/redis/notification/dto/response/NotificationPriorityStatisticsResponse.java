package com.redis.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationPriorityStatisticsResponse {
    private String priority;
    private long count;
}
