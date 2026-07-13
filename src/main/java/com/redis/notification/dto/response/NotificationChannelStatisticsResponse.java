package com.redis.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationChannelStatisticsResponse {
    private String channel;
    private long total;
    private long sent;
    private long failed;
    private long retrying;
    private long deadLetter;
    private double successRate;
}
