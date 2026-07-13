package com.redis.monitoring.entity;

import com.redis.monitoring.service.AlertEvaluationService;
import com.redis.notification.event.NotificationEventPublisher;
import com.redis.common.entity.EscalationLevel;
import com.redis.user.entity.User;

import com.redis.incident.entity.Incident;
import com.redis.incident.entity.IncidentTimeline;
import com.redis.incident.repository.IncidentRepository;
import com.redis.incident.repository.IncidentTimelineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEvaluationScheduler {

    private final AlertEvaluationService alertEvaluationService;
    private final IncidentRepository incidentRepository;
    private final IncidentTimelineRepository incidentTimelineRepository;
    private final NotificationEventPublisher notificationEventPublisher;

    @Scheduled(fixedDelayString = "${app.monitoring.evaluation-delay-ms:10000}")
    public void runEvaluation() {
        try {
            alertEvaluationService.evaluateRules();
        } catch (Exception e) {
            log.error("Error occurred during alert rules evaluation", e);
        }
    }

    @Scheduled(fixedDelayString = "${app.monitoring.escalation-delay-ms:20000}")
    @Transactional
    public void checkEscalations() {
        log.debug("Checking active incidents for escalation...");
        
        List<Incident> openIncidents = incidentRepository.findOpenIncidents();
        LocalDateTime now = LocalDateTime.now();

        for (Incident incident : openIncidents) {
            try {
                // Only escalate HIGH and CRITICAL severity incidents
                if (incident.getSeverity() != AlertSeverity.HIGH && incident.getSeverity() != AlertSeverity.CRITICAL) {
                    continue;
                }

                // Check SLA or acknowledgement breach
                if (incident.getStatus() == AlertStatus.OPEN && incident.getAcknowledgementDeadline() != null) {
                    if (now.isAfter(incident.getAcknowledgementDeadline()) && !incident.isSlaBreached()) {
                        incident.setSlaBreached(true);
                        incidentRepository.save(incident);
                        log.warn("Incident {} breached acknowledgement deadline!", incident.getIncidentNumber());
                    }
                }

                if (incident.getResolvedAt() == null && incident.getSlaDeadline() != null) {
                    if (now.isAfter(incident.getSlaDeadline()) && !incident.isSlaBreached()) {
                        incident.setSlaBreached(true);
                        incidentRepository.save(incident);
                        log.warn("Incident {} breached resolution SLA deadline!", incident.getIncidentNumber());
                    }
                }

                // Automatic escalation timing: e.g. every 1 minute of inaction escalates to next level for L1->L2->L3->EXECUTIVE
                long ageSeconds = Duration.between(incident.getCreatedAt(), now).toSeconds();
                EscalationLevel currentLevel = incident.getEscalationLevel();
                EscalationLevel nextLevel = null;

                if (currentLevel == EscalationLevel.L1 && ageSeconds > 60) {
                    nextLevel = EscalationLevel.L2;
                } else if (currentLevel == EscalationLevel.L2 && ageSeconds > 120) {
                    nextLevel = EscalationLevel.L3;
                } else if (currentLevel == EscalationLevel.L3 && ageSeconds > 180) {
                    nextLevel = EscalationLevel.EXECUTIVE;
                }

                if (nextLevel != null) {
                    AlertStatus prevStatus = incident.getStatus();
                    incident.setEscalationLevel(nextLevel);
                    incident.setStatus(AlertStatus.ESCALATED);
                    incidentRepository.save(incident);

                    IncidentTimeline timeline = IncidentTimeline.builder()
                            .incident(incident)
                            .previousStatus(prevStatus)
                            .newStatus(AlertStatus.ESCALATED)
                            .previousSeverity(incident.getSeverity())
                            .newSeverity(incident.getSeverity())
                            .actionPerformedBy("SYSTEM")
                            .actionSource("SCHEDULER")
                            .remarks("Incident automatically escalated to " + nextLevel + " due to SLA timeout.")
                            .build();
                    incidentTimelineRepository.save(timeline);

                    notificationEventPublisher.publishHighSeverityIncident(
                            "ESCALATED: " + incident.getTitle() + " (" + nextLevel + ")",
                            "Incident " + incident.getIncidentNumber() + " has been escalated to " + nextLevel + " level."
                    );

                    log.warn("INCIDENT_EVENT | IncidentId={} | Status=ESCALATED | Level={} | Source=SCHEDULER | User=SYSTEM | Timestamp={}",
                            incident.getIncidentNumber(), nextLevel, now);
                }
            } catch (Exception e) {
                log.error("Failed to check escalation for incident ID: {}", incident.getId(), e);
            }
        }
    }
}
