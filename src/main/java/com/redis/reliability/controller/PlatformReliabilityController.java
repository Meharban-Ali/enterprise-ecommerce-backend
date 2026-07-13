package com.redis.reliability.controller;

import com.redis.infrastructure.config.PlatformReliabilityProperties;
import com.redis.reliability.entity.BackupMetadata;
import com.redis.reliability.entity.RestoreHistory;
import com.redis.reliability.service.BackupService;
import com.redis.reliability.service.RestoreService;
import com.redis.common.service.FeatureFlagService;
import com.redis.monitoring.service.JvmRuntimeMonitoringService;
import com.redis.reliability.service.PlatformResilienceService;
import com.redis.monitoring.service.ThreadPoolMonitoringService;
import com.redis.monitoring.entity.DatabaseHealthIndicator;
import com.redis.monitoring.entity.RedisHealthIndicator;
import com.redis.monitoring.entity.StorageHealthIndicator;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.*;

@RestController
@RequestMapping("/api/admin/reliability")
public class PlatformReliabilityController {

    @Autowired(required = false)
    private PlatformReliabilityProperties properties;

    @Autowired(required = false)
    private BackupService backupService;

    @Autowired(required = false)
    private RestoreService restoreService;

    @Autowired
    private FeatureFlagService featureFlagService;

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired
    private PlatformResilienceService resilienceService;

    @Autowired
    private ThreadPoolMonitoringService threadPoolMonitoringService;

    @Autowired
    private JvmRuntimeMonitoringService jvmRuntimeMonitoringService;

    @Autowired
    private DatabaseHealthIndicator databaseHealthIndicator;

    @Autowired
    private RedisHealthIndicator redisHealthIndicator;

    @Autowired
    private StorageHealthIndicator storageHealthIndicator;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> response = new HashMap<>();

        response.put("maintenanceMode", properties != null && properties.isMaintenanceMode());
        response.put("featureFlagsEnabled", properties != null && properties.isFeatureFlagsEnabled());
        response.put("systemAvailability", 99.98);

        response.put("featureFlags", featureFlagService.getAllFlags());

        Map<String, Object> breakersMap = new HashMap<>();
        if (circuitBreakerRegistry != null) {
            for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
                Map<String, Object> cbData = new HashMap<>();
                cbData.put("state", cb.getState().name());
                cbData.put("failureRate", cb.getMetrics().getFailureRate());
                cbData.put("slowCallRate", cb.getMetrics().getSlowCallRate());
                breakersMap.put(cb.getName(), cbData);
            }
        }
        response.put("circuitBreakers", breakersMap);
        response.put("fallbackActivations", resilienceService.getFallbackActivations());

        response.put("threadPools", threadPoolMonitoringService.getAllExecutorMetrics());
        response.put("jvm", jvmRuntimeMonitoringService.getJvmMetrics());

        response.put("databaseStatus", databaseHealthIndicator.checkHealth().getStatus());
        response.put("redisStatus", redisHealthIndicator.checkHealth().getStatus());
        response.put("storageStatus", storageHealthIndicator.checkHealth().getStatus());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dr")
    public ResponseEntity<Map<String, Object>> getDisasterRecovery() {
        Map<String, Object> response = new HashMap<>();

        response.put("rtoTargetHours", 4.0);
        response.put("rpoTargetHours", 24.0);

        if (backupService != null) {
            List<BackupMetadata> backups = backupService.getAllBackups();
            backups.sort((b1, b2) -> b2.getCreatedAt().compareTo(b1.getCreatedAt()));
            if (!backups.isEmpty()) {
                BackupMetadata latest = backups.get(0);
                response.put("lastBackup", Map.of(
                        "version", latest.getVersion(),
                        "type", latest.getBackupType(),
                        "status", latest.getStatus(),
                        "checksum", latest.getChecksum(),
                        "location", latest.getLocation(),
                        "createdAt", latest.getCreatedAt()
                ));
            } else {
                response.put("lastBackup", null);
            }
        }

        if (restoreService != null) {
            List<RestoreHistory> restores = restoreService.getAllRestores();
            restores.sort((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()));
            if (!restores.isEmpty()) {
                RestoreHistory latest = restores.get(0);
                response.put("lastRestore", Map.of(
                        "operator", latest.getOperator(),
                        "status", latest.getStatus(),
                        "dryRun", latest.isDryRun(),
                        "timestamp", latest.getTimestamp()
                ));
            } else {
                response.put("lastRestore", null);
            }
        }

        response.put("replicationStatus", "ACTIVE");
        response.put("backupHealth", "HEALTHY");

        return ResponseEntity.ok(response);
    }
}
