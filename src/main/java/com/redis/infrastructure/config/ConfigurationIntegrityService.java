package com.redis.infrastructure.config;

import com.redis.incident.entity.PlatformIncidentHelper;

import com.redis.audit.event.AuditEventPublisher;
import com.redis.infrastructure.config.ConfigurationSnapshot;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import com.redis.infrastructure.config.ConfigurationSnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class ConfigurationIntegrityService {

    @Autowired(required = false)
    private ConfigurationSnapshotRepository snapshotRepository;

    @Autowired(required = false)
    private AuditEventPublisher auditEventPublisher;

    @Autowired
    private PlatformIncidentHelper platformIncidentHelper;

    private String currentChecksum;

    @Transactional
    public void initialize() {
        currentChecksum = calculateConfigChecksum();
        log.info("CONFIGURATION_CHANGED | Initial checksum calculated: {}", currentChecksum);

        if (snapshotRepository != null) {
            Optional<ConfigurationSnapshot> latestOpt = snapshotRepository.findLatestSnapshot();
            if (latestOpt.isEmpty()) {
                createSnapshot("SYSTEM", 1);
            } else {
                ConfigurationSnapshot latest = latestOpt.get();
                if (!latest.getChecksum().equals(currentChecksum)) {
                    log.warn("CONFIGURATION_CHANGED | Config mismatch detected from latest snapshot!");
                    createSnapshot("SYSTEM", latest.getVersion() + 1);
                }
            }
        }
    }

    @Transactional
    public void verifyIntegrity() {
        String newChecksum = calculateConfigChecksum();
        if (currentChecksum != null && !currentChecksum.equals(newChecksum)) {
            log.error("CONFIGURATION_CHANGED | Corruption detected! Expected: {} | Got: {}", currentChecksum, newChecksum);
            
            if (auditEventPublisher != null) {
                auditEventPublisher.publish(
                        null, "SYSTEM", AuditActionType.CONFIGURATION_CHANGED, AuditStatus.FAILED,
                        ResourceType.CONFIGURATION, "CONFIG_FILE",
                        "Configuration corruption detected! Checksum mismatch."
                );
            }

            platformIncidentHelper.triggerIncident("CONFIG_CORRUPT", "Configuration file modified unexpectedly. Expected: " + currentChecksum + " Got: " + newChecksum);
        }
    }

    @Transactional
    public void updateConfiguration(String changedBy) {
        currentChecksum = calculateConfigChecksum();
        if (snapshotRepository != null) {
            Optional<ConfigurationSnapshot> latestOpt = snapshotRepository.findLatestSnapshot();
            int nextVersion = latestOpt.map(s -> s.getVersion() + 1).orElse(1);
            createSnapshot(changedBy, nextVersion);
        }
    }

    private void createSnapshot(String operator, int version) {
        ConfigurationSnapshot snapshot = ConfigurationSnapshot.builder()
                .checksum(currentChecksum)
                .timestamp(LocalDateTime.now())
                .changedBy(operator)
                .correlationId(UUID.randomUUID().toString())
                .version(version)
                .build();
        snapshotRepository.save(snapshot);

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(
                    null, operator, AuditActionType.CONFIGURATION_CHANGED, AuditStatus.SUCCESS,
                    ResourceType.CONFIGURATION, "CONFIG_FILE",
                    "Configuration snapshot created. Version: " + version
            );
        }
    }

    public String calculateConfigChecksum() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is == null) {
                return "fallback-checksum-empty";
            }
            byte[] bytes = is.readAllBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "error-calculating-checksum";
        }
    }

    public String getCurrentChecksum() {
        return currentChecksum;
    }
}
