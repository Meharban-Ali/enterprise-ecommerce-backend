package com.redis.reliability.entity;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "restore_history")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RestoreHistory extends AuditableEntity {

    

    @Column(nullable = false)
    private String operator;

    @Column(name = "duration_ms")
    private long durationMs;

    @Column(name = "verification_result")
    private String verificationResult;

    @Column(name = "restore_type", nullable = false)
    private String restoreType;

    @Column(nullable = false)
    private String status;

    @Column(name = "dry_run", nullable = false)
    private boolean dryRun;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(nullable = false)
    private LocalDateTime timestamp;
}