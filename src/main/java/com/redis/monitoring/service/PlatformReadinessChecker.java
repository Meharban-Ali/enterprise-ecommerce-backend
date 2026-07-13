package com.redis.monitoring.service;

import com.redis.infrastructure.config.JwtProperties;
import com.redis.infrastructure.config.PlatformReliabilityProperties;
import com.redis.monitoring.entity.DatabaseHealthIndicator;
import com.redis.monitoring.entity.RedisHealthIndicator;
import com.redis.monitoring.entity.StorageHealthIndicator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Slf4j
@Component
public class PlatformReadinessChecker implements CommandLineRunner {

    @Autowired
    private DatabaseHealthIndicator databaseHealthIndicator;

    @Autowired
    private RedisHealthIndicator redisHealthIndicator;

    @Autowired
    private StorageHealthIndicator storageHealthIndicator;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired(required = false)
    private PlatformReliabilityProperties reliabilityProperties;

    @Autowired
    private Environment environment;

    @Override
    public void run(String... args) throws Exception {
        validatePlatform();
    }

    public void validatePlatform() {
        log.info("Performing startup Platform Readiness Checks...");
        boolean success = true;

        try {
            String db = databaseHealthIndicator.checkHealth().getStatus();
            if (!"UP".equalsIgnoreCase(db)) {
                log.error("[Readiness Check] Database is not ready: {}", db);
                success = false;
            }
        } catch (Exception e) {
            log.error("[Readiness Check] Database verification failed", e);
            success = false;
        }

        try {
            String storage = storageHealthIndicator.checkHealth().getStatus();
            if ("DOWN".equalsIgnoreCase(storage)) {
                log.error("[Readiness Check] Storage disk space critical!");
                success = false;
            }
        } catch (Exception e) {
            log.error("[Readiness Check] Storage verification failed", e);
            success = false;
        }

        if (jwtProperties == null || jwtProperties.getSecret() == null || jwtProperties.getSecret().trim().length() < 32) {
            log.error("[Readiness Check] JWT Secret Key is missing or weak!");
            success = false;
        }

        boolean isTestProfile = Arrays.asList(environment.getActiveProfiles()).contains("test");

        if (!success) {
            log.error("Platform Readiness validation failed!");
            if (!isTestProfile) {
                log.error("FAIL_FAST: Stopping application context due to critical platform validation failures.");
                throw new RuntimeException("Platform Readiness Checks Failed.");
            } else {
                log.warn("Test profile active: suppressing fail-fast context termination.");
            }
        } else {
            log.info("All Platform Readiness Checks passed successfully.");
        }
    }
}
