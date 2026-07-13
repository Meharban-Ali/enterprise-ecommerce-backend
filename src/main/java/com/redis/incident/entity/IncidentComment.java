package com.redis.incident.entity;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "incident_comments",
    indexes = {
        @Index(name = "idx_incident_comments_incident_id", columnList = "incident_id")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentComment extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    

    
}