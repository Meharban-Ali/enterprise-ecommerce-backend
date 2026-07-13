package com.redis.incident.dto.response;

import com.redis.monitoring.entity.AlertSeverity;
import com.redis.monitoring.entity.AlertStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentTimelineResponse {
    private Long id;
    private Long incidentId;
    private AlertStatus previousStatus;
    private AlertStatus newStatus;
    private AlertSeverity previousSeverity;
    private AlertSeverity newSeverity;
    private String actionPerformedBy;
    private String actionSource;
    private String remarks;
    private LocalDateTime createdAt;
}
