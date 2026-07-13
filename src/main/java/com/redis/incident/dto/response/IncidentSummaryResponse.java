package com.redis.incident.dto.response;

import com.redis.monitoring.entity.AlertSeverity;
import com.redis.monitoring.entity.AlertSource;
import com.redis.monitoring.entity.AlertStatus;
import com.redis.common.entity.EscalationLevel;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentSummaryResponse {
    private Long id;
    private String incidentNumber;
    private String alertRuleCode;
    private AlertSeverity severity;
    private AlertSource source;
    private String title;
    private AlertStatus status;
    private int occurrenceCount;
    private LocalDateTime firstOccurredAt;
    private LocalDateTime lastOccurredAt;
    private boolean slaBreached;
    private EscalationLevel escalationLevel;
    private LocalDateTime createdAt;
}
