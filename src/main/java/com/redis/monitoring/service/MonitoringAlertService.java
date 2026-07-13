package com.redis.monitoring.service;

import com.redis.monitoring.dto.response.AlertResponse;
import com.redis.reliability.dto.SystemHealthResponse;
import com.redis.observability.dto.response.SystemMetricsResponse;
import com.redis.reliability.dto.SchedulerStatusResponse;
import com.redis.observability.dto.response.JvmMetricsResponse;

import java.util.List;

public interface MonitoringAlertService {
    List<AlertResponse> evaluateAlertRules(
            SystemHealthResponse health,
            SystemMetricsResponse metrics,
            List<SchedulerStatusResponse> schedulers,
            JvmMetricsResponse jvmMetrics
    );
}
