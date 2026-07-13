package com.redis.incident.entity;

import com.redis.category.entity.ResolutionCategory;
import com.redis.monitoring.entity.AlertStatus;
import com.redis.monitoring.entity.AlertSource;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.common.entity.EscalationLevel;
import com.redis.monitoring.entity.AlertRule;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "incidents",
    indexes = {
        @Index(name = "idx_incidents_status", columnList = "status"),
        @Index(name = "idx_incidents_severity", columnList = "severity"),
        @Index(name = "idx_incidents_source", columnList = "source"),
        @Index(name = "idx_incidents_sla_deadline", columnList = "sla_deadline"),
        @Index(name = "idx_incidents_created_at", columnList = "created_at"),
        @Index(name = "idx_incidents_incident_number", columnList = "incident_number"),
        @Index(name = "idx_incidents_escalation_level", columnList = "escalation_level")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Incident extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    @Column(name = "incident_number", nullable = false, unique = true, length = 50)
    private String incidentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 50)
    private AlertSource source;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AlertStatus status;

    @Column(name = "first_occurred_at", nullable = false)
    private LocalDateTime firstOccurredAt;

    @Column(name = "last_occurred_at", nullable = false)
    private LocalDateTime lastOccurredAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "closed_by", length = 100)
    private String closedBy;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    // SLA tracking
    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Column(name = "sla_breached", nullable = false)
    private boolean slaBreached;

    @Column(name = "resolved_within_sla")
    private Boolean resolvedWithinSla;

    @Column(name = "acknowledgement_deadline")
    private LocalDateTime acknowledgementDeadline;

    // Escalation tracking
    @Enumerated(EnumType.STRING)
    @Column(name = "escalation_level", nullable = false, length = 30)
    private EscalationLevel escalationLevel;

    // Root Cause Analysis
    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "resolution_summary", columnDefinition = "TEXT")
    private String resolutionSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_category", length = 50)
    private ResolutionCategory resolutionCategory;

    

    
}