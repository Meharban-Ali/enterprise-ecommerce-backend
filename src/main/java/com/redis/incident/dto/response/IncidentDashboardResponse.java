package com.redis.incident.dto.response;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentDashboardResponse {
    // Active incidents
    private List<IncidentResponse> activeIncidentsList;

    // Operational KPIs
    private long activeIncidents;
    private long openCriticalIncidents;
    private long incidentsLastHour;
    private long incidentsToday;
    private long incidentsThisWeek;

    // Reliability KPIs
    private double mttdSeconds;             // Mean Time To Detect (in seconds)
    private double mttrSeconds;             // Mean Time To Resolve (in seconds)
    private double mtbfSeconds;             // Mean Time Between Failures (in seconds)
    private double incidentReopenRate;       // percentage (0-100)
    private long escalationCount;
    private long suppressedAlerts;
    private long autoResolvedCount;

    // SLA tracking
    private double slaCompliancePercentage;  // percentage (0-100)
    private long slaViolations;
    private long averageResolutionTimeMs;
    private long averageAcknowledgementTimeMs;
    private double systemAvailabilityPercentage; // availability (0-100)

    // Trend Metrics
    private Map<String, Long> topIncidentSources;
    private Map<String, Long> topAlertRules;
    private Map<String, Long> topFailedComponents;
}
