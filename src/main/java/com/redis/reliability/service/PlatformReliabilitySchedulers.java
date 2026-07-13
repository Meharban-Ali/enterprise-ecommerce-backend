package com.redis.reliability.service;

import com.redis.reliability.service.BackupService;
import com.redis.infrastructure.config.ConfigurationIntegrityService;

import com.redis.infrastructure.config.PlatformReliabilityProperties;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class PlatformReliabilitySchedulers {

    @Autowired
    private BackupService backupService;

    @Autowired
    private ConfigurationIntegrityService configurationIntegrityService;

    @Autowired
    private PlatformReliabilityProperties properties;

    @Getter
    private final SchedulerStats backupStats = new SchedulerStats();
    @Getter
    private final SchedulerStats cleanupStats = new SchedulerStats();
    @Getter
    private final SchedulerStats healthStats = new SchedulerStats();
    @Getter
    private final SchedulerStats configStats = new SchedulerStats();

    @Scheduled(fixedDelayString = "${app.reliability.backup-delay-ms:86400000}")
    public void runBackup() {
        if (properties != null && !properties.isBackupEnabled()) {
            return;
        }
        long start = System.currentTimeMillis();
        backupStats.executionCount.incrementAndGet();
        try {
            backupService.triggerBackup("SYSTEM_SCHEDULER", "FULL", "DAILY");
            backupStats.successCount.incrementAndGet();
        } catch (Exception e) {
            backupStats.failureCount.incrementAndGet();
            log.error("Scheduled backup failed: {}", e.getMessage());
        } finally {
            backupStats.durationMs.set(System.currentTimeMillis() - start);
            backupStats.lastRun = LocalDateTime.now();
            backupStats.nextRun = LocalDateTime.now().plusDays(1);
        }
    }

    @Scheduled(fixedDelayString = "${app.reliability.cleanup-delay-ms:86400000}")
    public void runCleanup() {
        if (properties != null && !properties.isBackupEnabled()) {
            return;
        }
        long start = System.currentTimeMillis();
        cleanupStats.executionCount.incrementAndGet();
        try {
            backupService.retentionCleanup();
            cleanupStats.successCount.incrementAndGet();
        } catch (Exception e) {
            cleanupStats.failureCount.incrementAndGet();
            log.error("Scheduled cleanup failed: {}", e.getMessage());
        } finally {
            cleanupStats.durationMs.set(System.currentTimeMillis() - start);
            cleanupStats.lastRun = LocalDateTime.now();
            cleanupStats.nextRun = LocalDateTime.now().plusDays(1);
        }
    }

    @Scheduled(fixedDelayString = "${app.reliability.health-delay-ms:60000}")
    public void runHealthValidation() {
        long start = System.currentTimeMillis();
        healthStats.executionCount.incrementAndGet();
        try {
            healthStats.successCount.incrementAndGet();
        } catch (Exception e) {
            healthStats.failureCount.incrementAndGet();
            log.error("Scheduled health check failed: {}", e.getMessage());
        } finally {
            healthStats.durationMs.set(System.currentTimeMillis() - start);
            healthStats.lastRun = LocalDateTime.now();
            healthStats.nextRun = LocalDateTime.now().plusSeconds(60);
        }
    }

    @Scheduled(fixedDelayString = "${app.reliability.config-delay-ms:300000}")
    public void runConfigurationVerification() {
        long start = System.currentTimeMillis();
        configStats.executionCount.incrementAndGet();
        try {
            configurationIntegrityService.verifyIntegrity();
            configStats.successCount.incrementAndGet();
        } catch (Exception e) {
            configStats.failureCount.incrementAndGet();
            log.error("Scheduled configuration integrity check failed: {}", e.getMessage());
        } finally {
            configStats.durationMs.set(System.currentTimeMillis() - start);
            configStats.lastRun = LocalDateTime.now();
            configStats.nextRun = LocalDateTime.now().plusMinutes(5);
        }
    }

    public Map<String, Object> getStatsMap(String name, SchedulerStats stats) {
        Map<String, Object> map = new HashMap<>();
        long exec = stats.executionCount.get();
        long succ = stats.successCount.get();
        double rate = exec == 0 ? 100.0 : ((double) succ / exec) * 100.0;

        map.put("name", name);
        map.put("executions", exec);
        map.put("failures", stats.failureCount.get());
        map.put("durationMs", stats.durationMs.get());
        map.put("lastRun", stats.lastRun);
        map.put("nextRun", stats.nextRun);
        map.put("successRate", rate);
        return map;
    }

    public static class SchedulerStats {
        public final AtomicLong executionCount = new AtomicLong(0);
        public final AtomicLong successCount = new AtomicLong(0);
        public final AtomicLong failureCount = new AtomicLong(0);
        public final AtomicLong durationMs = new AtomicLong(0);
        public LocalDateTime lastRun;
        public LocalDateTime nextRun;
    }
}
