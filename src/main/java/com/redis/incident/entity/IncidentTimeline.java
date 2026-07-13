package com.redis.incident.entity;

import com.redis.common.base.AuditableEntity;

import com.redis.monitoring.entity.AlertSeverity;
import com.redis.monitoring.entity.AlertStatus;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "incident_timelines",
    indexes = {
        @Index(name = "idx_incident_timelines_incident_id", columnList = "incident_id")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentTimeline extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private AlertStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private AlertStatus newStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_severity", length = 30)
    private AlertSeverity previousSeverity;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_severity", nullable = false, length = 30)
    private AlertSeverity newSeverity;

    @Column(name = "action_performed_by", nullable = false, length = 100)
    private String actionPerformedBy;

    @Column(name = "action_source", nullable = false, length = 50)
    private String actionSource; // e.g. "SYSTEM", "USER", "SCHEDULER"

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    
}