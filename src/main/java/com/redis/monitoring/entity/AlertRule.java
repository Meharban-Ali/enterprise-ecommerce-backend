package com.redis.monitoring.entity;

import com.redis.common.base.AuditableEntity;

import com.redis.monitoring.entity.AlertSeverity;
import com.redis.monitoring.entity.AlertSource;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "alert_rules",
    uniqueConstraints = {
        @UniqueConstraint(name = "uc_alert_rules_code", columnNames = {"rule_code"})
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    @NotBlank(message = "Rule code is required")
    @Size(max = 100, message = "Rule code cannot exceed 100 characters")
    @Column(name = "rule_code", nullable = false, length = 100)
    private String ruleCode;

    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name cannot exceed 255 characters")
    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @NotNull(message = "Alert source is required")
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "source", nullable = false, length = 50)
    private AlertSource source;

    @NotNull(message = "Alert severity is required")
    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "severity", nullable = false, length = 50)
    private AlertSeverity severity;

    @NotNull(message = "Threshold is required")
    @DecimalMin(value = "0.0", message = "Threshold must be greater than or equal to 0")
    @Column(name = "threshold", nullable = false)
    private Double threshold;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Min(value = 1, message = "Evaluation interval must be greater than 0")
    @Column(name = "evaluation_interval_seconds", nullable = false)
    private int evaluationIntervalSeconds;

    @Min(value = 1, message = "Cooldown seconds must be greater than 0")
    @Column(name = "cooldown_seconds", nullable = false)
    private int cooldownSeconds;

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    

    

    

    
}