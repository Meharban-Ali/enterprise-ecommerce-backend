package com.redis.auth.entity;

import com.redis.user.entity.User;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_token", columnList = "token", unique = true)
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken extends AuditableEntity {

    // ─── Primary Key ────────────────────────────────────────────────────────────
    

    // ─── Token Properties ───────────────────────────────────────────────────────
    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    // LAZY fetch ensures we don't automatically load the User record from the DB
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    // ─── Audit Field ───────────────────────────────────────────────────────────
    
}