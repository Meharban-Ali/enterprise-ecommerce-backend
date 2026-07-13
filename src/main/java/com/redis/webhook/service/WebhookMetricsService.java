package com.redis.webhook.service;

import com.redis.webhook.dto.response.WebhookDashboardResponse;
import com.redis.webhook.dto.response.WebhookStatisticsResponse;

import java.util.List;

public interface WebhookMetricsService {
    WebhookDashboardResponse getDashboard();
    WebhookStatisticsResponse getEndpointStats(Long endpointId);
}
