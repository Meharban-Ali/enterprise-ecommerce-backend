package com.redis.incident.entity;

import com.redis.monitoring.entity.AlertSeverity;

import com.redis.monitoring.entity.AlertRule;
import com.redis.incident.entity.Incident;
import com.redis.incident.entity.IncidentTimeline;
import com.redis.monitoring.entity.AlertStatus;
import com.redis.common.entity.EscalationLevel;
import com.redis.monitoring.repository.AlertRuleRepository;
import com.redis.incident.repository.IncidentRepository;
import com.redis.incident.repository.IncidentTimelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PlatformIncidentHelper {

    @Autowired(required = false)
    private IncidentRepository incidentRepository;

    @Autowired(required = false)
    private IncidentTimelineRepository incidentTimelineRepository;

    @Autowired(required = false)
    private AlertRuleRepository alertRuleRepository;

    private final AtomicLong incidentSequence = new AtomicLong(System.currentTimeMillis() % 1000000);

    @Transactional
    public void triggerIncident(String ruleCode, String description) {
        if (incidentRepository == null || alertRuleRepository == null || incidentTimelineRepository == null) {
            return;
        }

        Optional<AlertRule> ruleOpt = alertRuleRepository.findByRuleCode(ruleCode);
        if (ruleOpt.isEmpty()) {
            return;
        }

        AlertRule rule = ruleOpt.get();

        Optional<Incident> openIncidentOpt = incidentRepository.findTopByAlertRuleIdOrderByCreatedAtDesc(rule.getId());
        if (openIncidentOpt.isPresent()) {
            Incident existing = openIncidentOpt.get();
            if (existing.getStatus() == AlertStatus.OPEN || existing.getStatus() == AlertStatus.ACKNOWLEDGED) {
                existing.setOccurrenceCount(existing.getOccurrenceCount() + 1);
                existing.setLastOccurredAt(LocalDateTime.now());
                existing.setDescription(description);
                incidentRepository.save(existing);
                return;
            }
        }

        String incNum = String.format("INC-%d-%06d", LocalDate.now().getYear(), incidentSequence.incrementAndGet());
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime slaDeadline = now.plusHours(rule.getSeverity() == com.redis.monitoring.entity.AlertSeverity.CRITICAL ? 2 : 4);
        LocalDateTime ackDeadline = now.plusMinutes(rule.getSeverity() == com.redis.monitoring.entity.AlertSeverity.CRITICAL ? 15 : 30);

        Incident incident = Incident.builder()
                .incidentNumber(incNum)
                .alertRule(rule)
                .severity(rule.getSeverity())
                .source(rule.getSource())
                .title("Platform Alert: " + rule.getRuleName())
                .description(description)
                .status(AlertStatus.OPEN)
                .firstOccurredAt(now)
                .lastOccurredAt(now)
                .occurrenceCount(1)
                .slaDeadline(slaDeadline)
                .slaBreached(false)
                .acknowledgementDeadline(ackDeadline)
                .escalationLevel(EscalationLevel.L1)
                .correlationId(UUID.randomUUID().toString())
                .build();

        incident = incidentRepository.save(incident);

        IncidentTimeline timeline = IncidentTimeline.builder()
                .incident(incident)
                .previousStatus(null)
                .newStatus(AlertStatus.OPEN)
                .previousSeverity(null)
                .newSeverity(incident.getSeverity())
                .actionPerformedBy("SYSTEM")
                .actionSource("SYSTEM")
                .remarks("Reliability Incident auto-created.")
                .build();
        incidentTimelineRepository.save(timeline);
    }
}
