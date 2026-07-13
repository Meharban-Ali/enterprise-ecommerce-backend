package com.redis.security.entity;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_keys", indexes = {
    @Index(name = "idx_idempotency_key_lookup", columnList = "key_value"),
    @Index(name = "idx_idempotency_key_expiry", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class IdempotencyKey extends AuditableEntity {

    

    @Column(name = "key_value", nullable = false, unique = true)
    private String key;

    @Column(nullable = false)
    private String status; // IN_PROGRESS, COMPLETED

    private int responseStatus;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String responseHeaders;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private String requestFingerprint;

    
}