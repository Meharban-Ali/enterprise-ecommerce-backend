package com.redis.user.service;

import com.redis.audit.entity.AuditActionType;
import com.redis.notification.event.NotificationEventPublisher;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.audit.entity.AuditStatus;

import com.redis.user.exception.UserAlreadyExistsException;
import com.redis.common.dto.ProfileUpdateRequest;
import com.redis.auth.dto.request.RegisterRequest;
import com.redis.auth.dto.response.RegisterResponse;
import com.redis.user.dto.response.UserResponse;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.auth.service.RefreshTokenService;
import com.redis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationEventPublisher notificationEventPublisher;
    private final RefreshTokenService refreshTokenService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    @Override
    @Transactional
    public RegisterResponse registerUser(RegisterRequest request) {
        log.info("Registration started for username: {}, email: {}", request.getUsername(), request.getEmail());
        log.info("Processing user registration");

        // 1. Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already exists");
            throw new UserAlreadyExistsException(request.getEmail());
        }

        // 2. Map DTO to User entity and encrypt password
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hashing
                .role(Role.ROLE_USER) // Default role assignment
                .accountEnabled(true) // Default enabled
                .accountNonLocked(true) // Default unlocked
                .build();

        // 4. Persist entity
        User savedUser = userRepository.save(user);
        log.info("User registered successfully — ID: {}", savedUser.getId());
        log.info("User saved: ID={}, email={}", savedUser.getId(), savedUser.getEmail());

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(savedUser.getId(), savedUser.getEmail(), com.redis.audit.entity.AuditActionType.REGISTER, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.USER, String.valueOf(savedUser.getId()), "User registered successfully");
        }

        // Publish welcome event
        try {
            log.info("Welcome email triggered for user ID: {}, email: {}", savedUser.getId(), savedUser.getEmail());
            notificationEventPublisher.publishWelcome(savedUser.getId(), savedUser.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish welcome event for user ID: {}", savedUser.getId(), e);
        }

        log.info("Registration completed: ID={}, email={}", savedUser.getId(), savedUser.getEmail());

        // 5. Return formatted response DTO
        return RegisterResponse.builder()
                .message("User registered successfully")
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .build();
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        log.info("Updating profile details for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        // Validate email uniqueness if changed
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Profile update failed — email already in use: {}", request.getEmail());
                throw new UserAlreadyExistsException(request.getEmail());
            }
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        User savedUser = userRepository.save(user);

        log.info("Profile updated successfully for user ID: {}", savedUser.getId());

        return UserResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getActualUsername())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .accountEnabled(savedUser.isAccountEnabled())
                .accountNonLocked(savedUser.isAccountNonLocked())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .username(user.getActualUsername())
                        .email(user.getEmail())
                        .role(user.getRole())
                        .accountEnabled(user.isAccountEnabled())
                        .accountNonLocked(user.isAccountNonLocked())
                        .createdAt(user.getCreatedAt())
                        .updatedAt(user.getUpdatedAt())
                        .build())
                .orElse(null);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, com.redis.user.dto.request.ChangePasswordRequest request) {
        log.info("Processing password change for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));

        // Validation a: oldPassword must match current stored password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("Password change failed for user ID: {} — current password mismatch", userId);
            throw new IllegalArgumentException("Current password does not match");
        }

        // Validation b: newPassword must match confirmPassword
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Password change failed for user ID: {} — confirm password mismatch", userId);
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        // Validation d: newPassword must not be identical to oldPassword
        if (request.getNewPassword().equals(request.getOldPassword())) {
            log.warn("Password change failed for user ID: {} — new password identical to current password", userId);
            throw new IllegalArgumentException("New password cannot be the same as the current password");
        }

        // Encode and save new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all active refresh tokens (invalidates active sessions atomically within transaction)
        refreshTokenService.deleteByUserId(user.getId());
        log.info("Revoked all active refresh tokens for user ID: {}", userId);

        // Publish confirmation email event
        try {
            notificationEventPublisher.publishPasswordChanged(user.getId(), user.getEmail());
            log.info("Password changed confirmation email event published for user ID: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish password change notification event for user ID: {}", userId, e);
        }

        log.info("Password changed successfully for user ID: {}", userId);

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(user.getId(), user.getEmail(), com.redis.audit.entity.AuditActionType.PASSWORD_CHANGED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.USER, String.valueOf(user.getId()), "User changed password successfully");
        }
    }
}
