package com.redis.notification.service;

import com.redis.notification.dto.response.NotificationAnalyticsResponse;
import com.redis.notification.dto.response.NotificationPriorityStatisticsResponse;
import com.redis.notification.dto.response.NotificationResponse;
import com.redis.notification.dto.response.NotificationTypeStatisticsResponse;
import com.redis.notification.dto.response.NotificationDashboardResponse;
import com.redis.notification.dto.response.NotificationChannelStatisticsResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationAnalyticsService {
    NotificationDashboardResponse getDashboard(LocalDateTime start, LocalDateTime end);
    NotificationAnalyticsResponse getOverallAnalytics(LocalDateTime start, LocalDateTime end);
    List<NotificationChannelStatisticsResponse> getChannelStatistics(LocalDateTime start, LocalDateTime end);
    List<NotificationTypeStatisticsResponse> getTypeStatistics(LocalDateTime start, LocalDateTime end);
    List<NotificationPriorityStatisticsResponse> getPriorityStatistics(LocalDateTime start, LocalDateTime end);
    NotificationAnalyticsResponse getRetryStatistics(LocalDateTime start, LocalDateTime end);
    List<NotificationResponse> getDeadLetterSummary(LocalDateTime start, LocalDateTime end, int page, int size);
    List<NotificationResponse> getRecentFailures(LocalDateTime start, LocalDateTime end, int page, int size);
}
