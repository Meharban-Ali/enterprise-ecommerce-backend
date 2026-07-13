package com.redis.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.reliability")
public class PlatformReliabilityProperties {

    private boolean backupEnabled = true;
    private boolean restoreEnabled = true;
    private boolean maintenanceMode = false;
    private boolean featureFlagsEnabled = true;
    private boolean resilienceEnabled = true;
    private int backupRetentionDays = 7;
    private int healthValidationInterval = 60;
    private int configurationVerificationInterval = 300;

    private Map<String, Boolean> featureFlags = new ConcurrentHashMap<>();

    public PlatformReliabilityProperties() {
        // Initialize default feature flags
        featureFlags.put("NOTIFICATIONS", true);
        featureFlags.put("WEBHOOKS", true);
        featureFlags.put("MONITORING", true);
        featureFlags.put("AUDIT", true);
        featureFlags.put("ALERTING", true);
        featureFlags.put("API_KEYS", true);
        featureFlags.put("RATE_LIMITING", true);
        featureFlags.put("IDEMPOTENCY", true);
        featureFlags.put("SECURITY", true);
    }
}
