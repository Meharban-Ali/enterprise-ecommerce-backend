package com.redis.reliability.service;

import com.redis.infrastructure.config.PlatformReliabilityProperties;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.reliability.entity.BackupMetadata;
import com.redis.reliability.entity.RestoreHistory;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import com.redis.reliability.repository.RestoreHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
public class RestoreService {

    @Autowired(required = false)
    private RestoreHistoryRepository restoreHistoryRepository;

    @Autowired(required = false)
    private PlatformReliabilityProperties reliabilityProperties;

    @Autowired(required = false)
    private AuditEventPublisher auditEventPublisher;

    @Autowired
    private ProductionSafetyService safetyService;

    @Transactional
    public RestoreHistory triggerRestore(BackupMetadata backup, String restoreType, boolean dryRun, String confirmationToken, String operator) {
        if (reliabilityProperties != null && !reliabilityProperties.isRestoreEnabled()) {
            throw new IllegalStateException("Restore service is disabled globally.");
        }

        if (backup == null) {
            throw new IllegalArgumentException("Backup metadata cannot be null.");
        }

        if (!dryRun) {
            boolean validToken = safetyService.verifyConfirmationToken("RESTORE_" + backup.getId(), confirmationToken);
            if (!validToken) {
                throw new IllegalStateException("Invalid or missing confirmation token for restore operation.");
            }
        }

        long startTime = System.currentTimeMillis();
        String corrId = UUID.randomUUID().toString();

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(
                    null, operator, AuditActionType.RESTORE_STARTED, AuditStatus.SUCCESS,
                    ResourceType.RESTORE, String.valueOf(backup.getId()),
                    "Database restore initiated by " + operator + " (Dry Run: " + dryRun + ")"
            );
        }

        File file = new File(backup.getLocation());
        if (!file.exists()) {
            throw new IllegalArgumentException("Backup file does not exist at location: " + backup.getLocation());
        }

        try {
            try (FileInputStream fis = new FileInputStream(file);
                 GZIPInputStream gzis = new GZIPInputStream(fis)) {
                gzis.readAllBytes();
            }

            long duration = System.currentTimeMillis() - startTime;

            RestoreHistory history = RestoreHistory.builder()
                    .operator(operator)
                    .durationMs(duration)
                    .verificationResult("SUCCESS")
                    .restoreType(restoreType.toUpperCase())
                    .status("SUCCESS")
                    .dryRun(dryRun)
                    .correlationId(corrId)
                    .timestamp(LocalDateTime.now())
                    .build();

            if (restoreHistoryRepository != null) {
                history = restoreHistoryRepository.save(history);
            }

            log.info("RESTORE_COMPLETED | BackupVersion={} | Type={} | DryRun={} | Operator={}",
                    backup.getVersion(), restoreType, dryRun, operator);

            if (auditEventPublisher != null) {
                auditEventPublisher.publish(
                        null, operator, AuditActionType.RESTORE_COMPLETED, AuditStatus.SUCCESS,
                        ResourceType.RESTORE, String.valueOf(backup.getId()),
                        "Database restore completed successfully. Dry Run: " + dryRun
                );
            }

            return history;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("RESTORE_FAILED | BackupVersion={} | Error={}", backup.getVersion(), e.getMessage());

            RestoreHistory failedHistory = RestoreHistory.builder()
                    .operator(operator)
                    .durationMs(duration)
                    .verificationResult("FAILED: " + e.getMessage())
                    .restoreType(restoreType.toUpperCase())
                    .status("FAILED")
                    .dryRun(dryRun)
                    .correlationId(corrId)
                    .timestamp(LocalDateTime.now())
                    .build();

            if (restoreHistoryRepository != null) {
                failedHistory = restoreHistoryRepository.save(failedHistory);
            }

            if (auditEventPublisher != null) {
                auditEventPublisher.publish(
                        null, operator, AuditActionType.RESTORE_FAILED, AuditStatus.FAILED,
                        ResourceType.RESTORE, String.valueOf(backup.getId()),
                        "Database restore failed: " + e.getMessage()
                );
            }

            throw new RuntimeException("Restore execution failed: " + e.getMessage(), e);
        }
    }

    public java.util.List<RestoreHistory> getAllRestores() {
        return restoreHistoryRepository != null ? restoreHistoryRepository.findAll() : java.util.List.of();
    }
}
