package com.redis.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@Component
@Validated
@ConfigurationProperties(prefix = "app.monitoring")
public class MonitoringProperties {

    @Min(0)
    private int dashboardCacheSeconds = 30;

    @Min(0)
    private int healthCacheSeconds = 30;

    @Min(0)
    private int metricsCacheSeconds = 30;

    @Min(1)
    private long monitoringTimeoutMs = 2000;

    @Min(1)
    @Max(1000)
    private int recentErrorLimit = 100;

    @Min(1)
    private long schedulerWarningThresholdMs = 5000;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double notificationFailureWarningThreshold = 0.10;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double paymentFailureWarningThreshold = 0.05;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private double lowDiskWarningPercentage = 15.0;

    private boolean schedulerMetricsEnabled = true;
    private boolean jvmMetricsEnabled = true;
    private boolean systemInfoEnabled = true;
}
