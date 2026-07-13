package com.redis.monitoring.dto.request;

import com.redis.monitoring.entity.AlertSeverity;
import com.redis.monitoring.entity.AlertSource;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleRequest {

    @NotBlank(message = "Rule code is required")
    @Size(max = 100, message = "Rule code cannot exceed 100 characters")
    private String ruleCode;

    @NotBlank(message = "Rule name is required")
    @Size(max = 255, message = "Rule name cannot exceed 255 characters")
    private String ruleName;

    @NotNull(message = "Alert source is required")
    private AlertSource source;

    @NotNull(message = "Alert severity is required")
    private AlertSeverity severity;

    @NotNull(message = "Threshold is required")
    @DecimalMin(value = "0.0", message = "Threshold must be greater than or equal to 0")
    private Double threshold;

    private boolean enabled;

    @Min(value = 1, message = "Evaluation interval must be greater than 0")
    private int evaluationIntervalSeconds;

    @Min(value = 1, message = "Cooldown seconds must be greater than 0")
    private int cooldownSeconds;

    private boolean notificationEnabled;
}
