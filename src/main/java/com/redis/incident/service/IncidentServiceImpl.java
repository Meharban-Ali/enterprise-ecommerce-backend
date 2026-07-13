package com.redis.incident.service;

import com.redis.category.entity.ResolutionCategory;
import com.redis.incident.dto.response.IncidentDashboardResponse;
import com.redis.incident.dto.response.IncidentTimelineResponse;
import com.redis.category.entity.Category;
import com.redis.monitoring.entity.AlertStatus;
import com.redis.incident.entity.Incident;
import com.redis.incident.dto.request.IncidentCommentRequest;
import com.redis.monitoring.entity.AlertSource;
import com.redis.incident.entity.IncidentTimeline;
import com.redis.monitoring.repository.AlertRuleRepository;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.incident.dto.response.IncidentSummaryResponse;
import com.redis.incident.entity.IncidentComment;
import com.redis.incident.repository.IncidentCommentRepository;
import com.redis.incident.dto.response.IncidentResponse;
import com.redis.common.entity.EscalationLevel;
import com.redis.user.entity.User;
import com.redis.incident.dto.response.IncidentCommentResponse;
import com.redis.incident.repository.IncidentTimelineRepository;
import com.redis.incident.repository.IncidentRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final IncidentCommentRepository incidentCommentRepository;
    private final AlertRuleRepository alertRuleRepository;

    @Override
    @Transactional
    public IncidentResponse acknowledge(Long id, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + id));

        if (incident.getStatus() == AlertStatus.RESOLVED || incident.getStatus() == AlertStatus.CLOSED) {
            throw new IllegalStateException("Cannot acknowledge a resolved or closed incident.");
        }

        AlertStatus prevStatus = incident.getStatus();
        incident.setStatus(AlertStatus.ACKNOWLEDGED);
        incident.setAcknowledgedAt(LocalDateTime.now());
        incident.setAcknowledgedBy(username);
        incidentRepository.save(incident);

        IncidentTimeline timeline = IncidentTimeline.builder()
                .incident(incident)
                .previousStatus(prevStatus)
                .newStatus(AlertStatus.ACKNOWLEDGED)
                .previousSeverity(incident.getSeverity())
                .newSeverity(incident.getSeverity())
                .actionPerformedBy(username)
                .actionSource("USER")
                .remarks("Incident acknowledged by " + username)
                .build();
        incidentTimelineRepository.save(timeline);

        log.info("INCIDENT_EVENT | IncidentId={} | Severity={} | Status=ACKNOWLEDGED | Source={} | User={} | Timestamp={}",
                incident.getIncidentNumber(), incident.getSeverity(), incident.getSource(), username, LocalDateTime.now());

        return mapToResponse(incident);
    }

    @Override
    @Transactional
    public IncidentResponse resolve(Long id, String username, String rootCause, String resolutionSummary, ResolutionCategory category) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + id));

        if (incident.getStatus() == AlertStatus.CLOSED) {
            throw new IllegalStateException("Cannot resolve a closed incident.");
        }

        AlertStatus prevStatus = incident.getStatus();
        LocalDateTime now = LocalDateTime.now();
        incident.setStatus(AlertStatus.RESOLVED);
        incident.setResolvedAt(now);
        incident.setResolvedBy(username);
        incident.setRootCause(rootCause);
        incident.setResolutionSummary(resolutionSummary);
        incident.setResolutionCategory(category);

        boolean withinSla = now.isBefore(incident.getSlaDeadline());
        incident.setResolvedWithinSla(withinSla);
        if (!withinSla) {
            incident.setSlaBreached(true);
        }
        incidentRepository.save(incident);

        IncidentTimeline timeline = IncidentTimeline.builder()
                .incident(incident)
                .previousStatus(prevStatus)
                .newStatus(AlertStatus.RESOLVED)
                .previousSeverity(incident.getSeverity())
                .newSeverity(incident.getSeverity())
                .actionPerformedBy(username)
                .actionSource("USER")
                .remarks("Incident resolved by " + username + ". Category: " + category)
                .build();
        incidentTimelineRepository.save(timeline);

        log.info("INCIDENT_EVENT | IncidentId={} | Severity={} | Status=RESOLVED | Source={} | User={} | Timestamp={}",
                incident.getIncidentNumber(), incident.getSeverity(), incident.getSource(), username, now);

        return mapToResponse(incident);
    }

    @Override
    @Transactional
    public IncidentResponse close(Long id, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + id));

        if (incident.getStatus() != AlertStatus.RESOLVED) {
            throw new IllegalStateException("Incident must be RESOLVED before closing.");
        }

        AlertStatus prevStatus = incident.getStatus();
        incident.setStatus(AlertStatus.CLOSED);
        incident.setClosedBy(username);
        incidentRepository.save(incident);

        IncidentTimeline timeline = IncidentTimeline.builder()
                .incident(incident)
                .previousStatus(prevStatus)
                .newStatus(AlertStatus.CLOSED)
                .previousSeverity(incident.getSeverity())
                .newSeverity(incident.getSeverity())
                .actionPerformedBy(username)
                .actionSource("USER")
                .remarks("Incident closed by " + username)
                .build();
        incidentTimelineRepository.save(timeline);

        log.info("INCIDENT_EVENT | IncidentId={} | Severity={} | Status=CLOSED | Source={} | User={} | Timestamp={}",
                incident.getIncidentNumber(), incident.getSeverity(), incident.getSource(), username, LocalDateTime.now());

        return mapToResponse(incident);
    }

    @Override
    @Transactional
    public IncidentResponse reopen(Long id, String username) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + id));

        if (incident.getStatus() != AlertStatus.RESOLVED && incident.getStatus() != AlertStatus.CLOSED) {
            throw new IllegalStateException("Incident must be RESOLVED or CLOSED to reopen.");
        }

        AlertStatus prevStatus = incident.getStatus();
        incident.setStatus(AlertStatus.REOPENED);
        incidentRepository.save(incident);

        IncidentTimeline timeline = IncidentTimeline.builder()
                .incident(incident)
                .previousStatus(prevStatus)
                .newStatus(AlertStatus.REOPENED)
                .previousSeverity(incident.getSeverity())
                .newSeverity(incident.getSeverity())
                .actionPerformedBy(username)
                .actionSource("USER")
                .remarks("Incident reopened by " + username)
                .build();
        incidentTimelineRepository.save(timeline);

        log.info("INCIDENT_EVENT | IncidentId={} | Severity={} | Status=REOPENED | Source={} | User={} | Timestamp={}",
                incident.getIncidentNumber(), incident.getSeverity(), incident.getSource(), username, LocalDateTime.now());

        return mapToResponse(incident);
    }

    @Override
    @Transactional
    public IncidentCommentResponse addComment(Long incidentId, IncidentCommentRequest request, String username) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with ID: " + incidentId));

        IncidentComment comment = IncidentComment.builder()
                .incident(incident)
                .comment(request.getComment())
                .build();
        comment.setCreatedBy(username);
        comment = incidentCommentRepository.save(comment);

        return IncidentCommentResponse.builder()
                .id(comment.getId())
                .incidentId(incident.getId())
                .comment(comment.getComment())
                .createdBy(comment.getCreatedBy())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentCommentResponse> getIncidentComments(String incidentNumber) {
        return incidentCommentRepository.findByIncidentIncidentNumberOrderByCreatedAtDesc(incidentNumber)
                .stream()
                .map(comment -> IncidentCommentResponse.builder()
                        .id(comment.getId())
                        .incidentId(comment.getIncident().getId())
                        .comment(comment.getComment())
                        .createdBy(comment.getCreatedBy())
                        .createdAt(comment.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentTimelineResponse> getIncidentTimeline(String incidentNumber) {
        return incidentTimelineRepository.findByIncidentIncidentNumberOrderByCreatedAtAsc(incidentNumber)
                .stream()
                .map(timeline -> IncidentTimelineResponse.builder()
                        .id(timeline.getId())
                        .incidentId(timeline.getIncident().getId())
                        .previousStatus(timeline.getPreviousStatus())
                        .newStatus(timeline.getNewStatus())
                        .previousSeverity(timeline.getPreviousSeverity())
                        .newSeverity(timeline.getNewSeverity())
                        .actionPerformedBy(timeline.getActionPerformedBy())
                        .actionSource(timeline.getActionSource())
                        .remarks(timeline.getRemarks())
                        .createdAt(timeline.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getIncidentByNumber(String incidentNumber) {
        Incident incident = incidentRepository.findByIncidentNumber(incidentNumber)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with number: " + incidentNumber));
        return mapToResponse(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentDashboardResponse getIncidentDashboard() {
        LocalDateTime now = LocalDateTime.now();
        List<Incident> allIncidents = incidentRepository.findAll();

        long active = allIncidents.stream()
                .filter(i -> i.getStatus() == AlertStatus.OPEN || i.getStatus() == AlertStatus.ACKNOWLEDGED || i.getStatus() == AlertStatus.REOPENED || i.getStatus() == AlertStatus.ESCALATED)
                .count();

        long openCritical = allIncidents.stream()
                .filter(i -> (i.getStatus() == AlertStatus.OPEN || i.getStatus() == AlertStatus.REOPENED || i.getStatus() == AlertStatus.ESCALATED) && i.getSeverity() == AlertSeverity.CRITICAL)
                .count();

        long lastHour = allIncidents.stream()
                .filter(i -> i.getCreatedAt().isAfter(now.minusHours(1)))
                .count();

        long today = allIncidents.stream()
                .filter(i -> i.getCreatedAt().toLocalDate().isEqual(LocalDate.now()))
                .count();

        long thisWeek = allIncidents.stream()
                .filter(i -> i.getCreatedAt().isAfter(now.minusDays(7)))
                .count();

        // Calculate MTTD (Mean Time To Detect) & MTTR (Mean Time To Resolve)
        double totalMttdSeconds = 0.0;
        double totalMttrSeconds = 0.0;
        long resolvedCount = 0;

        for (Incident i : allIncidents) {
            totalMttdSeconds += Duration.between(i.getFirstOccurredAt(), i.getCreatedAt()).toSeconds();
            if (i.getResolvedAt() != null) {
                totalMttrSeconds += Duration.between(i.getFirstOccurredAt(), i.getResolvedAt()).toSeconds();
                resolvedCount++;
            }
        }

        double mttd = allIncidents.isEmpty() ? 0.0 : totalMttdSeconds / allIncidents.size();
        double mttr = resolvedCount == 0 ? 0.0 : totalMttrSeconds / resolvedCount;

        // Calculate MTBF (Mean Time Between Failures)
        double mtbf = 86400.0; // fallback 1 day
        if (allIncidents.size() > 1) {
            List<Incident> sorted = allIncidents.stream()
                    .sorted(Comparator.comparing(Incident::getCreatedAt))
                    .collect(Collectors.toList());
            double totalIntervals = 0.0;
            for (int k = 1; k < sorted.size(); k++) {
                totalIntervals += Duration.between(sorted.get(k - 1).getCreatedAt(), sorted.get(k).getCreatedAt()).toSeconds();
            }
            mtbf = totalIntervals / (sorted.size() - 1);
        }

        // Calculate Incident Reopen Rate
        long totalReopens = 0;
        List<IncidentTimeline> allTimelines = incidentTimelineRepository.findAll();
        for (IncidentTimeline t : allTimelines) {
            if (t.getNewStatus() == AlertStatus.REOPENED) {
                totalReopens++;
            }
        }
        double reopenRate = allIncidents.isEmpty() ? 0.0 : ((double) totalReopens / allIncidents.size()) * 100.0;

        // Escalation count
        long escalations = allTimelines.stream()
                .filter(t -> t.getNewStatus() == AlertStatus.ESCALATED)
                .count();

        // Suppressed Alerts
        long suppressed = allIncidents.stream()
                .mapToLong(i -> Math.max(0, i.getOccurrenceCount() - 1))
                .sum();

        // Auto Resolved count
        long autoResolved = allIncidents.stream()
                .filter(i -> "SYSTEM".equals(i.getResolvedBy()))
                .count();

        // SLA KPIs
        long slaViolations = allIncidents.stream()
                .filter(Incident::isSlaBreached)
                .count();

        long resolvedWithinSlaCount = allIncidents.stream()
                .filter(i -> Boolean.TRUE.equals(i.getResolvedWithinSla()))
                .count();

        double slaCompliance = resolvedCount == 0 ? 100.0 : ((double) resolvedWithinSlaCount / resolvedCount) * 100.0;

        long totalResolutionTimeMs = 0;
        long totalAcknowledgementTimeMs = 0;
        long ackCount = 0;

        for (Incident i : allIncidents) {
            if (i.getResolvedAt() != null) {
                totalResolutionTimeMs += Duration.between(i.getCreatedAt(), i.getResolvedAt()).toMillis();
            }
            if (i.getAcknowledgedAt() != null) {
                totalAcknowledgementTimeMs += Duration.between(i.getCreatedAt(), i.getAcknowledgedAt()).toMillis();
                ackCount++;
            }
        }

        long avgResolution = resolvedCount == 0 ? 0L : totalResolutionTimeMs / resolvedCount;
        long avgAck = ackCount == 0 ? 0L : totalAcknowledgementTimeMs / ackCount;

        // System Availability
        double availability = 100.0;
        long activeCritical = openCritical;
        if (activeCritical > 0) {
            availability = Math.max(90.0, 100.0 - (activeCritical * 0.05));
        }

        // Trends
        Map<String, Long> topSources = allIncidents.stream()
                .collect(Collectors.groupingBy(i -> i.getSource().name(), Collectors.counting()));
        Map<String, Long> topRules = allIncidents.stream()
                .collect(Collectors.groupingBy(i -> i.getAlertRule().getRuleCode(), Collectors.counting()));
        Map<String, Long> topFailedComponents = topSources;

        List<IncidentResponse> activeList = allIncidents.stream()
                .filter(i -> i.getStatus() != AlertStatus.CLOSED && i.getStatus() != AlertStatus.RESOLVED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return IncidentDashboardResponse.builder()
                .activeIncidentsList(activeList)
                .activeIncidents(active)
                .openCriticalIncidents(openCritical)
                .incidentsLastHour(lastHour)
                .incidentsToday(today)
                .incidentsThisWeek(thisWeek)
                .mttdSeconds(mttd)
                .mttrSeconds(mttr)
                .mtbfSeconds(mtbf)
                .incidentReopenRate(reopenRate)
                .escalationCount(escalations)
                .suppressedAlerts(suppressed)
                .autoResolvedCount(autoResolved)
                .slaCompliancePercentage(slaCompliance)
                .slaViolations(slaViolations)
                .averageResolutionTimeMs(avgResolution)
                .averageAcknowledgementTimeMs(avgAck)
                .systemAvailabilityPercentage(availability)
                .topIncidentSources(topSources)
                .topAlertRules(topRules)
                .topFailedComponents(topFailedComponents)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentSummaryResponse> searchIncidents(
            AlertSeverity severity,
            AlertStatus status,
            AlertSource source,
            Boolean slaBreached,
            EscalationLevel escalationLevel,
            String incidentNumber,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    ) {
        Specification<Incident> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (severity != null) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (slaBreached != null) {
                predicates.add(cb.equal(root.get("slaBreached"), slaBreached));
            }
            if (escalationLevel != null) {
                predicates.add(cb.equal(root.get("escalationLevel"), escalationLevel));
            }
            if (incidentNumber != null && !incidentNumber.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("incidentNumber")), "%" + incidentNumber.toLowerCase() + "%"));
            }
            if (start != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
            }
            if (end != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), end));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return incidentRepository.findAll(spec, pageable)
                .map(i -> IncidentSummaryResponse.builder()
                        .id(i.getId())
                        .incidentNumber(i.getIncidentNumber())
                        .alertRuleCode(i.getAlertRule().getRuleCode())
                        .severity(i.getSeverity())
                        .source(i.getSource())
                        .title(i.getTitle())
                        .status(i.getStatus())
                        .occurrenceCount(i.getOccurrenceCount())
                        .firstOccurredAt(i.getFirstOccurredAt())
                        .lastOccurredAt(i.getLastOccurredAt())
                        .slaBreached(i.isSlaBreached())
                        .escalationLevel(i.getEscalationLevel())
                        .createdAt(i.getCreatedAt())
                        .build());
    }

    private IncidentResponse mapToResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .incidentNumber(incident.getIncidentNumber())
                .alertRuleId(incident.getAlertRule().getId())
                .alertRuleCode(incident.getAlertRule().getRuleCode())
                .severity(incident.getSeverity())
                .source(incident.getSource())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .status(incident.getStatus())
                .firstOccurredAt(incident.getFirstOccurredAt())
                .lastOccurredAt(incident.getLastOccurredAt())
                .acknowledgedAt(incident.getAcknowledgedAt())
                .resolvedAt(incident.getResolvedAt())
                .acknowledgedBy(incident.getAcknowledgedBy())
                .resolvedBy(incident.getResolvedBy())
                .closedBy(incident.getClosedBy())
                .occurrenceCount(incident.getOccurrenceCount())
                .correlationId(incident.getCorrelationId())
                .slaDeadline(incident.getSlaDeadline())
                .slaBreached(incident.isSlaBreached())
                .resolvedWithinSla(incident.getResolvedWithinSla())
                .acknowledgementDeadline(incident.getAcknowledgementDeadline())
                .escalationLevel(incident.getEscalationLevel())
                .rootCause(incident.getRootCause())
                .resolutionSummary(incident.getResolutionSummary())
                .resolutionCategory(incident.getResolutionCategory())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}
