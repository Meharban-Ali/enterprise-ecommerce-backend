package com.redis.observability.dto.response;

import com.redis.monitoring.dto.MonitoringMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricsResponse {
    // Flat structures for backward compatibility
    private long totalUsers;
    private long totalOrders;
    private long totalPayments;
    private long totalNotifications;
    private long totalAuditLogs;
    private long activeProducts;
    private long lowStockProducts;
    private long failedNotificationsCount;
    private long retryQueueSize;
    private long dlqSize;

    // Structured categories
    private Map<String, Object> infrastructureMetrics;
    private Map<String, Object> businessMetrics;
    private Map<String, Object> runtimeMetrics;
    
    private MonitoringMetadata metadata;
}
