package com.redis.incident.dto.response;

import com.redis.monitoring.entity.AlertSeverity;
import com.redis.category.entity.ResolutionCategory;
import com.redis.monitoring.entity.AlertStatus;
import com.redis.common.entity.EscalationLevel;
import com.redis.monitoring.entity.AlertSource;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentResponse {
    private Long id;
    private String incidentNumber;
    private Long alertRuleId;
    private String alertRuleCode;
    private AlertSeverity severity;
    private AlertSource source;
    private String title;
    private String description;
    private AlertStatus status;
    private LocalDateTime firstOccurredAt;
    private LocalDateTime lastOccurredAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private String acknowledgedBy;
    private String resolvedBy;
    private String closedBy;
    private int occurrenceCount;
    private String correlationId;

    // SLA details
    private LocalDateTime slaDeadline;
    private boolean slaBreached;
    private Boolean resolvedWithinSla;
    private LocalDateTime acknowledgementDeadline;

    // Escalation details
    private EscalationLevel escalationLevel;

    // RCA details
    private String rootCause;
    private String resolutionSummary;
    private ResolutionCategory resolutionCategory;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
