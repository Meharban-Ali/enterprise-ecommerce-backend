package com.redis.monitoring.service;

import com.redis.reliability.dto.DashboardResponse;
import com.redis.reliability.dto.SchedulerStatusResponse;
import com.redis.observability.dto.response.SystemMetricsResponse;
import com.redis.reliability.dto.ModulesHealthResponse;
import com.redis.reliability.dto.RecentErrorsResponse;
import com.redis.reliability.dto.RecentSystemErrorResponse;
import com.redis.observability.dto.response.JvmMetricsResponse;
import com.redis.reliability.dto.SystemInfoResponse;
import com.redis.reliability.dto.SystemHealthResponse;

import java.util.List;

public interface SystemMonitoringService {
    DashboardResponse getDashboard();
    SystemHealthResponse getSystemHealth();
    SystemMetricsResponse getSystemMetrics();
    SystemInfoResponse getSystemInfo();
    JvmMetricsResponse getJvmMetrics();
    List<RecentSystemErrorResponse> getRecentErrors();
    List<SchedulerStatusResponse> getSchedulerStatuses();
    void registerError(String component, Throwable throwable);

    // Sprint 9.1 enhancements
    RecentErrorsResponse getRecentErrorsWrapped();
    ModulesHealthResponse getModulesHealthWrapped();
}
