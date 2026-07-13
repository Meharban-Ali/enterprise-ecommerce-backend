package com.redis.notification.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationDashboardResponse {
    private NotificationAnalyticsResponse overallSummary;
    private List<NotificationChannelStatisticsResponse> channelStatistics;
    private List<NotificationTypeStatisticsResponse> typeStatistics;
    private List<NotificationPriorityStatisticsResponse> priorityStatistics;
    private NotificationAnalyticsResponse retryStatistics;
    private NotificationAnalyticsResponse dlqStatistics;
    private List<NotificationResponse> recentFailures;
}
