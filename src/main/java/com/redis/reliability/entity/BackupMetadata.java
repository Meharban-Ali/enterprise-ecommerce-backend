package com.redis.reliability.entity;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "backup_metadata")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BackupMetadata extends AuditableEntity {

    

    

    @Column(name = "backup_type", nullable = false)
    private String backupType;

    @Column(nullable = false)
    private String status;

    @Column(name = "duration_ms")
    private long durationMs;

    @Column(name = "size_bytes")
    private long sizeBytes;

    @Column(nullable = false)
    private String checksum;

    @Column(nullable = false)
    private String location;

    @Column(name = "encryption_status")
    private String encryptionStatus;

    @Column(name = "compression_status")
    private String compressionStatus;

    @Column(name = "verification_status")
    private String verificationStatus;

    @Column(name = "retention_type")
    private String retentionType;

    
}