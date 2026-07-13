package com.redis.incident.controller;

import com.redis.incident.dto.response.IncidentDashboardResponse;
import com.redis.category.entity.ResolutionCategory;
import com.redis.incident.dto.response.IncidentTimelineResponse;
import com.redis.category.entity.Category;
import com.redis.monitoring.entity.AlertStatus;
import com.redis.audit.entity.AuditActionType;
import com.redis.common.entity.ResourceType;
import com.redis.incident.entity.Incident;
import com.redis.incident.dto.request.IncidentCommentRequest;
import com.redis.monitoring.entity.AlertSource;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.incident.dto.response.IncidentCommentResponse;
import com.redis.incident.dto.response.IncidentSummaryResponse;
import com.redis.incident.dto.response.IncidentResponse;
import com.redis.common.entity.EscalationLevel;
import com.redis.audit.entity.AuditStatus;

import com.redis.common.dto.ApiResponse;
import com.redis.user.dto.response.UserResponse;
import com.redis.user.service.UserService;
import com.redis.incident.service.IncidentService;
import com.redis.audit.event.AuditEventPublisher;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/incidents")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class IncidentController {

    private final IncidentService incidentService;
    private final UserService userService;
    private final AuditEventPublisher auditEventPublisher;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<IncidentSummaryResponse>>> searchIncidents(
            @RequestParam(required = false) AlertSeverity severity,
            @RequestParam(required = false) AlertStatus status,
            @RequestParam(required = false) AlertSource source,
            @RequestParam(required = false) Boolean slaBreached,
            @RequestParam(required = false) EscalationLevel escalationLevel,
            @RequestParam(required = false) String incidentNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Pageable pageable
    ) {
        Page<IncidentSummaryResponse> page = incidentService.searchIncidents(
                severity, status, source, slaBreached, escalationLevel, incidentNumber, start, end, pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Incidents search results retrieved successfully", page));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<IncidentDashboardResponse>> getDashboard() {
        IncidentDashboardResponse dashboard = incidentService.getIncidentDashboard();
        return ResponseEntity.ok(ApiResponse.success("Incident dashboard stats retrieved successfully", dashboard));
    }

    @GetMapping("/{incidentNumber}")
    public ResponseEntity<ApiResponse<IncidentResponse>> getByIncidentNumber(@PathVariable String incidentNumber) {
        IncidentResponse response = incidentService.getIncidentByNumber(incidentNumber);
        return ResponseEntity.ok(ApiResponse.success("Incident details retrieved successfully", response));
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<ApiResponse<IncidentResponse>> acknowledgeIncident(@PathVariable Long id) {
        String username = getUsername();
        IncidentResponse response = incidentService.acknowledge(id, username);
        audit(AuditActionType.INCIDENT_ACKNOWLEDGED, AuditStatus.SUCCESS, String.valueOf(id), "Acknowledged incident: " + response.getIncidentNumber());
        return ResponseEntity.ok(ApiResponse.success("Incident acknowledged successfully", response));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<IncidentResponse>> resolveIncident(
            @PathVariable Long id,
            @RequestParam String rootCause,
            @RequestParam String resolutionSummary,
            @RequestParam ResolutionCategory category
    ) {
        String username = getUsername();
        IncidentResponse response = incidentService.resolve(id, username, rootCause, resolutionSummary, category);
        audit(AuditActionType.INCIDENT_RESOLVED, AuditStatus.SUCCESS, String.valueOf(id), "Resolved incident: " + response.getIncidentNumber() + ". Category: " + category);
        return ResponseEntity.ok(ApiResponse.success("Incident resolved successfully", response));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<IncidentResponse>> closeIncident(@PathVariable Long id) {
        String username = getUsername();
        IncidentResponse response = incidentService.close(id, username);
        audit(AuditActionType.INCIDENT_CLOSED, AuditStatus.SUCCESS, String.valueOf(id), "Closed incident: " + response.getIncidentNumber());
        return ResponseEntity.ok(ApiResponse.success("Incident closed successfully", response));
    }

    @GetMapping("/{incidentNumber}/timeline")
    public ResponseEntity<ApiResponse<List<IncidentTimelineResponse>>> getTimeline(@PathVariable String incidentNumber) {
        List<IncidentTimelineResponse> list = incidentService.getIncidentTimeline(incidentNumber);
        return ResponseEntity.ok(ApiResponse.success("Incident timeline transitions retrieved successfully", list));
    }

    @GetMapping("/{incidentNumber}/comments")
    public ResponseEntity<ApiResponse<List<IncidentCommentResponse>>> getComments(@PathVariable String incidentNumber) {
        List<IncidentCommentResponse> list = incidentService.getIncidentComments(incidentNumber);
        return ResponseEntity.ok(ApiResponse.success("Incident comments retrieved successfully", list));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<IncidentCommentResponse>> addComment(
            @PathVariable Long id,
            @Valid @RequestBody IncidentCommentRequest request
    ) {
        String username = getUsername();
        IncidentCommentResponse comment = incidentService.addComment(id, request, username);
        audit(AuditActionType.ADMIN_OPERATION, AuditStatus.SUCCESS, String.valueOf(id), "Added comment to incident ID " + id);
        return ResponseEntity.ok(ApiResponse.success("Comment added successfully", comment));
    }

    private String getUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private void audit(AuditActionType action, AuditStatus status, String resourceId, String desc) {
        try {
            String email = getUsername();
            UserResponse user = userService.getUserByEmail(email);
            Long userId = user != null ? user.getId() : null;
            auditEventPublisher.publish(userId, email, action, status, ResourceType.INCIDENT, resourceId, desc);
        } catch (Exception e) {
            log.error("Failed to publish audit event for incident lifecycle operation", e);
        }
    }
}
