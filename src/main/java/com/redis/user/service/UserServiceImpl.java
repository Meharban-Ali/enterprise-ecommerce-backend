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

        // 2. Validate security question and answer consistency
        if ((request.getSecurityQuestion() != null && !request.getSecurityQuestion().isBlank()) 
                || (request.getSecurityAnswer() != null && !request.getSecurityAnswer().isBlank())) {
            if (request.getSecurityQuestion() == null || request.getSecurityQuestion().isBlank() 
                    || request.getSecurityAnswer() == null || request.getSecurityAnswer().isBlank()) {
                throw new IllegalArgumentException("Both security question and security answer must be provided if either is set");
            }
        }

        // 3. Map DTO to User entity and encrypt password
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hashing
                .role(Role.ROLE_USER) // Default role assignment
                .accountEnabled(true) // Default enabled
                .accountNonLocked(true) // Default unlocked
                .securityQuestion(request.getSecurityQuestion())
                .securityAnswer(request.getSecurityAnswer() != null && !request.getSecurityAnswer().isBlank() 
                        ? passwordEncoder.encode(request.getSecurityAnswer()) 
                        : null)
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
}
