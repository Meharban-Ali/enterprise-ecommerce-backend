package com.redis.user.entity;

import com.redis.notification.entity.UserNotificationPreference;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_email", columnList = "email", unique = true)
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class User extends AuditableEntity implements UserDetails, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    // ─── Primary Key ────────────────────────────────────────────────────────────
    

    // ─── Core Credentials ───────────────────────────────────────────────────────
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 255, message = "Password must be at least 6 characters")
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    // ─── Role & Status Flags ────────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Builder.Default
    @Column(name = "account_enabled", nullable = false)
    private boolean accountEnabled = true;

    @Builder.Default
    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked = true;

    @Builder.Default
    @Column(name = "password_change_required", nullable = false)
    private boolean passwordChangeRequired = false;

    @Column(name = "phone", length = 20)
    private String phone;

    // ─── Security Question Fields ────────────────────────────────────────────────
    @Column(name = "security_question", length = 255)
    private String securityQuestion;

    @Column(name = "security_answer", length = 255)
    private String securityAnswer;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserNotificationPreference notificationPreference;

    // ─── Audit Fields ───────────────────────────────────────────────────────────
    

    


    // ═══════════════════════════════════════════════════════════════════════════
    //  UserDetails Interface Implementations
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // SimpleGrantedAuthority must match "ROLE_USER" / "ROLE_ADMIN" prefix exactly
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    public String getActualUsername() {
        return this.username;
    }

    @Override
    public String getUsername() {
        return this.email; // We use email as the unique identifier for logging in
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // We don't implement credentials/account expiration logic
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Credentials do not expire automatically
    }

    @Override
    public boolean isEnabled() {
        return this.accountEnabled;
    }
}