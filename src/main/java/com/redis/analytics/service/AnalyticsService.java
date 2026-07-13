package com.redis.analytics.service;

import com.redis.analytics.dto.AnalyticsResponse;

public interface AnalyticsService {
    /** Admin/SuperAdmin: Get platform business analytics. */
    AnalyticsResponse getAnalytics();
}
