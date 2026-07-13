package com.redis.user.entity;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_sessions",
    indexes = {
        @Index(name = "idx_user_sessions_user_id", columnList = "user_id", unique = true),
        @Index(name = "idx_user_sessions_email", columnList = "email", unique = true)
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession extends AuditableEntity implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // ONLINE, OFFLINE
}