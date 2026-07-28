package com.redis.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.security")
public class ApiSecurityProperties {

    private boolean rateLimitEnabled = false;
    private int defaultRateLimitUser = 60;
    private int defaultRateLimitIp = 100;
    private int defaultRateLimitAnonymous = 20;

    private boolean idempotencyEnabled = true;
    private int idempotencyTtlHours = 24;

    private boolean apiKeyEnabled = true;
    private int apiKeyRotationGracePeriodDays = 1;

    private boolean correlationIdEnabled = true;

    private boolean replayProtectionEnabled = true;
    private int replayWindowSeconds = 300; // 5 minutes

    private boolean fingerprintEnabled = true;
    private boolean apiAnalyticsEnabled = true;
    private boolean apiGovernanceEnabled = true;
    private boolean distributedLockEnabled = false;
    private boolean sensitiveDataMaskingEnabled = true;

    private boolean adaptiveRateLimitEnabled = false;
    private boolean endpointRateLimitEnabled = false;
    private boolean payloadProtectionEnabled = false;
    private boolean jwtStrictValidationEnabled = false;
    private boolean apiVersioningEnabled = false;
    private boolean consumerAnalyticsEnabled = true;
    private long maxRequestBodySize = 1048576; // 1MB
    private int maxFailedApiKeyAttempts = 5;
    private int apiKeyLockDurationMinutes = 30;
    private String endpointRateLimitsRaw = "";
    private java.util.Map<String, Integer> endpointRateLimits = new java.util.HashMap<>();

    public java.util.Map<String, Integer> getParsedEndpointRateLimits() {
        java.util.Map<String, Integer> map = new java.util.HashMap<>();
        if (endpointRateLimitsRaw != null && !endpointRateLimitsRaw.isBlank()) {
            String[] entries = endpointRateLimitsRaw.split(",");
            for (String entry : entries) {
                String[] parts = entry.trim().split(":");
                if (parts.length == 2) {
                    try {
                        map.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (endpointRateLimits != null) {
            map.putAll(endpointRateLimits);
        }
        return map;
    }
}
