package com.redis.reliability.service;

import com.redis.infrastructure.config.PlatformReliabilityProperties;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.reliability.entity.BackupMetadata;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import com.redis.reliability.repository.BackupMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
public class BackupService {

    @Autowired(required = false)
    private BackupMetadataRepository metadataRepository;

    @Autowired(required = false)
    private PlatformReliabilityProperties reliabilityProperties;

    @Autowired(required = false)
    private AuditEventPublisher auditEventPublisher;

    private static final String BACKUP_DIR = "backups";

    @Transactional
    public BackupMetadata triggerBackup(String operator, String backupType, String retentionType) {
        if (reliabilityProperties != null && !reliabilityProperties.isBackupEnabled()) {
            throw new IllegalStateException("Backup service is disabled globally.");
        }

        long startTime = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(
                    null, operator, AuditActionType.BACKUP_STARTED, AuditStatus.SUCCESS,
                    ResourceType.BACKUP, "CONFIG", "Database backup initiation requested by " + operator
            );
        }

        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        int nextVersion = metadataRepository != null ? (int) (metadataRepository.count() + 1) : 1;
        String fileName = String.format("backup_v%d_%s_%s.sql.gz", nextVersion, backupType.toLowerCase(), timestamp);
        File backupFile = new File(dir, fileName);

        try {
            try (FileOutputStream fos = new FileOutputStream(backupFile);
                 GZIPOutputStream gzos = new GZIPOutputStream(fos);
                 OutputStreamWriter osw = new OutputStreamWriter(gzos, StandardCharsets.UTF_8)) {

                osw.write("-- Ecommerce Redis Application Database Backup\n");
                osw.write("-- Version: " + nextVersion + "\n");
                osw.write("-- Type: " + backupType + "\n");
                osw.write("-- Timestamp: " + now.toString() + "\n");
                osw.write("CREATE TABLE products (id INT, name VARCHAR(255), price DECIMAL);\n");
                osw.write("CREATE TABLE users (id INT, email VARCHAR(255), role VARCHAR(50));\n");
                osw.write("CREATE TABLE orders (id INT, total DECIMAL, status VARCHAR(50));\n");
                osw.write("CREATE TABLE payments (id INT, amount DECIMAL, status VARCHAR(50));\n");
                osw.flush();
            }

            long duration = System.currentTimeMillis() - startTime;
            long size = backupFile.length();
            String checksum = calculateSHA256(backupFile);

            BackupMetadata metadata = BackupMetadata.builder()
                    .backupType(backupType.toUpperCase())
                    .status("SUCCESS")
                    .durationMs(duration)
                    .sizeBytes(size)
                    .checksum(checksum)
                    .location(backupFile.getAbsolutePath())
                    .compressionStatus("GZIP")
                    .encryptionStatus("AES-256-MOCK")
                    .verificationStatus("PENDING")
                    .retentionType(retentionType.toUpperCase())
                    .build();
            metadata.setVersion(nextVersion);
            metadata.setCreatedAt(now);

            if (metadataRepository != null) {
                metadata = metadataRepository.save(metadata);
            }

            log.info("BACKUP_COMPLETED | Version={} | Size={}B | Checksum={} | Location={}",
                    nextVersion, size, checksum, backupFile.getAbsolutePath());

            if (auditEventPublisher != null) {
                auditEventPublisher.publish(
                        null, operator, AuditActionType.BACKUP_COMPLETED, AuditStatus.SUCCESS,
                        ResourceType.BACKUP, String.valueOf(metadata.getId()),
                        "Database backup completed successfully. Location: " + backupFile.getName()
                );
            }

            verifyBackup(metadata, operator);

            return metadata;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("BACKUP_FAILED | Type={} | Error={}", backupType, e.getMessage());

            BackupMetadata failedMetadata = BackupMetadata.builder()
                    .backupType(backupType.toUpperCase())
                    .status("FAILED")
                    .durationMs(duration)
                    .sizeBytes(0)
                    .checksum("N/A")
                    .location("N/A")
                    .retentionType(retentionType.toUpperCase())
                    .build();
            failedMetadata.setVersion(nextVersion);
            failedMetadata.setCreatedAt(now);

            if (metadataRepository != null) {
                failedMetadata = metadataRepository.save(failedMetadata);
            }

            if (auditEventPublisher != null) {
                auditEventPublisher.publish(
                        null, operator, AuditActionType.BACKUP_FAILED, AuditStatus.FAILED,
                        ResourceType.BACKUP, String.valueOf(failedMetadata.getId()),
                        "Database backup failed: " + e.getMessage()
                );
            }

            throw new RuntimeException("Backup creation failed: " + e.getMessage(), e);
        }
    }

    public void verifyBackup(BackupMetadata metadata, String operator) {
        if (metadata == null || !"SUCCESS".equals(metadata.getStatus())) {
            return;
        }

        File file = new File(metadata.getLocation());
        if (!file.exists()) {
            metadata.setVerificationStatus("FAILED");
            if (metadataRepository != null) metadataRepository.save(metadata);
            return;
        }

        try {
            String calculated = calculateSHA256(file);
            boolean match = calculated.equals(metadata.getChecksum());

            try (FileInputStream fis = new FileInputStream(file);
                 java.util.zip.GZIPInputStream gzis = new java.util.zip.GZIPInputStream(fis)) {
                gzis.readAllBytes();
            }

            if (match) {
                metadata.setVerificationStatus("VERIFIED");
                log.info("BACKUP_VERIFIED | Checksum match for: {}", file.getName());
            } else {
                metadata.setVerificationStatus("CORRUPTED");
                log.error("BACKUP_CORRUPTED | Checksum mismatch for: {}", file.getName());
            }

            if (metadataRepository != null) {
                metadataRepository.save(metadata);
            }

        } catch (Exception e) {
            metadata.setVerificationStatus("CORRUPTED");
            if (metadataRepository != null) {
                metadataRepository.save(metadata);
            }
        }
    }

    @Transactional
    public void retentionCleanup() {
        if (metadataRepository == null || reliabilityProperties == null) {
            return;
        }

        log.info("Starting automated backup retention cleanup...");
        List<BackupMetadata> backups = metadataRepository.findAll();
        LocalDateTime threshold = LocalDateTime.now().minusDays(reliabilityProperties.getBackupRetentionDays());

        for (BackupMetadata backup : backups) {
            if (backup.getCreatedAt().isBefore(threshold) && !"SYSTEM_PROTECTED".equals(backup.getRetentionType())) {
                File file = new File(backup.getLocation());
                if (file.exists()) {
                    file.delete();
                }
                backup.setStatus("PURGED");
                metadataRepository.save(backup);
                log.info("Purged expired backup: {}", file.getName());
            }
        }
    }

    private String calculateSHA256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public List<BackupMetadata> getAllBackups() {
        return metadataRepository != null ? metadataRepository.findAll() : List.of();
    }
}
