package com.redis.monitoring.dto.response;

import com.redis.monitoring.entity.AlertSeverity;
import com.redis.monitoring.entity.AlertSource;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleResponse {
    private Long id;
    private String ruleCode;
    private String ruleName;
    private AlertSource source;
    private AlertSeverity severity;
    private Double threshold;
    private boolean enabled;
    private int evaluationIntervalSeconds;
    private int cooldownSeconds;
    private boolean notificationEnabled;
    private Long version;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
