package com.redis.reliability.dto;

import com.redis.incident.dto.response.IncidentDashboardResponse;
import com.redis.observability.dto.response.JvmMetricsResponse;
import com.redis.webhook.dto.response.WebhookDashboardResponse;
import com.redis.monitoring.dto.MonitoringMetadata;
import com.redis.observability.dto.response.SystemMetricsResponse;
import com.redis.monitoring.dto.response.AlertResponse;
import com.redis.incident.dto.response.IncidentResponse;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private LocalDateTime generatedAt;
    private long generationTimeMs;
    private SystemHealthResponse health;
    private SystemMetricsResponse metrics;
    private List<SchedulerStatusResponse> schedulers;
    private List<RecentSystemErrorResponse> recentErrors;
    private SystemInfoResponse systemInfo;
    private JvmMetricsResponse jvmMetrics;
    private List<AlertResponse> alerts;
    private MonitoringMetadata metadata;

    // Sprint 9.2 Dashboard Enhancements
    private List<AlertResponse> activeAlerts;
    private long activeIncidents;
    private long criticalIncidents;
    private long averageResolutionTime;
    private double systemAvailability;
    private Map<String, Long> alertTrend;

    private List<IncidentResponse> activeIncidentsList;
    private IncidentDashboardResponse incidentDashboard;

    // Sprint 9.3 Webhook Dashboard Extensions
    private WebhookDashboardResponse webhookDashboard;
    private double webhookSuccessRate;
    private long webhookFailures;
    private long webhookRetryQueue;
    private long webhookDlq;
    private double webhookAverageResponseTime;
}
