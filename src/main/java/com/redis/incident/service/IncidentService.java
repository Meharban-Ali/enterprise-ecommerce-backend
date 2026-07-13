package com.redis.incident.service;

import com.redis.incident.dto.response.IncidentDashboardResponse;
import com.redis.category.entity.ResolutionCategory;
import com.redis.incident.dto.response.IncidentTimelineResponse;
import com.redis.monitoring.entity.AlertStatus;
import com.redis.incident.dto.request.IncidentCommentRequest;
import com.redis.monitoring.entity.AlertSource;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.incident.dto.response.IncidentSummaryResponse;
import com.redis.incident.dto.response.IncidentResponse;
import com.redis.common.entity.EscalationLevel;
import com.redis.incident.dto.response.IncidentCommentResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentService {
    IncidentResponse acknowledge(Long id, String username);
    IncidentResponse resolve(Long id, String username, String rootCause, String resolutionSummary, ResolutionCategory category);
    IncidentResponse close(Long id, String username);
    IncidentResponse reopen(Long id, String username);
    
    IncidentCommentResponse addComment(Long incidentId, IncidentCommentRequest request, String username);
    List<IncidentCommentResponse> getIncidentComments(String incidentNumber);
    List<IncidentTimelineResponse> getIncidentTimeline(String incidentNumber);
    
    IncidentResponse getIncidentByNumber(String incidentNumber);
    IncidentDashboardResponse getIncidentDashboard();
    
    Page<IncidentSummaryResponse> searchIncidents(
            AlertSeverity severity,
            AlertStatus status,
            AlertSource source,
            Boolean slaBreached,
            EscalationLevel escalationLevel,
            String incidentNumber,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );
}
