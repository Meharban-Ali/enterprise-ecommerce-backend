package com.redis.user.service;

import com.redis.audit.entity.AuditActionType;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.auth.service.RefreshTokenService;
import com.redis.audit.entity.AuditStatus;

import com.redis.user.exception.UserAlreadyExistsException;
import com.redis.auth.dto.request.RegisterRequest;
import com.redis.product.dto.response.ProductResponse;
import com.redis.product.dto.response.ProductStatsResponse;
import com.redis.user.dto.response.UserResponse;
import com.redis.user.dto.response.UserSessionResponse;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.user.entity.UserSession;
import com.redis.product.mapper.ProductMapper;
import com.redis.product.repository.ProductRepository;
import com.redis.user.repository.UserRepository;
import com.redis.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserSessionService userSessionService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    private static final int INACTIVE_TIMEOUT_MINUTES = 15;
    private static final int LOW_STOCK_THRESHOLD = 10;

    @Override
    @Transactional(readOnly = true)
    public Page<UserSessionResponse> getOnlineUsers(Pageable pageable) {
        log.info("Super Admin: Fetching online users");
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(INACTIVE_TIMEOUT_MINUTES);
        Page<UserSession> sessions = userSessionRepository.findOnlineSessions(threshold, pageable);
        return sessions.map(this::toSessionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserSessionResponse> getOfflineUsers(Pageable pageable) {
        log.info("Super Admin: Fetching offline users");
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(INACTIVE_TIMEOUT_MINUTES);
        Page<UserSession> sessions = userSessionRepository.findOfflineSessions(threshold, pageable);
        return sessions.map(this::toSessionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String roleStr, String search, Boolean enabled, Boolean nonLocked, Pageable pageable) {
        log.info("Super Admin: Filtering and searching users");
        Role role = null;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                role = Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role passed: {}", roleStr);
            }
        }
        Page<User> users = userRepository.searchAndFilterUsers(role, search, enabled, nonLocked, pageable);
        return users.map(this::toUserResponse);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserResponse request) {
        log.info("Super Admin: Updating user ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));

        // Security Guard: Prevent demotion of the last Super Admin
        if (user.getRole() == Role.ROLE_SUPER_ADMIN && request.getRole() != Role.ROLE_SUPER_ADMIN) {
            long superAdminCount = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.ROLE_SUPER_ADMIN)
                    .count();
            if (superAdminCount <= 1) {
                log.warn("Update rejected — cannot demote the only remaining Super Admin account");
                throw new IllegalArgumentException("Cannot demote the last remaining Super Admin");
            }
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setAccountEnabled(request.isAccountEnabled());
        user.setAccountNonLocked(request.isAccountNonLocked());

        User savedUser = userRepository.save(user);

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(savedUser.getId(), savedUser.getEmail(), com.redis.audit.entity.AuditActionType.ROLE_CHANGED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.ROLE, String.valueOf(savedUser.getId()), "Admin updated user details/roles for email: " + savedUser.getEmail());
        }

        return toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Super Admin: Deleting user ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));

        // Security Guard: Super Admin accounts cannot be deleted
        if (user.getRole() == Role.ROLE_SUPER_ADMIN) {
            log.warn("Delete rejected — Super Admin accounts cannot be deleted: {}", user.getEmail());
            throw new IllegalArgumentException("Super Admin accounts cannot be deleted");
        }

        userRepository.delete(user);
        
        // Clean up session if exists
        userSessionRepository.findByUserId(id).ifPresent(userSessionRepository::delete);
    }

    @Override
    @Transactional
    public UserResponse changeStatus(Long id, String action) {
        log.info("Super Admin: Altering status flags of user ID: {} with action: {}", id, action);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));

        switch (action.toLowerCase()) {
            case "activate":
            case "enable":
                user.setAccountEnabled(true);
                break;
            case "deactivate":
            case "disable":
                // Security Guard: Last Super Admin cannot be disabled
                if (user.getRole() == Role.ROLE_SUPER_ADMIN) {
                    throw new IllegalArgumentException("Super Admin account status cannot be altered directly");
                }
                user.setAccountEnabled(false);
                break;
            case "lock":
                if (user.getRole() == Role.ROLE_SUPER_ADMIN) {
                    throw new IllegalArgumentException("Super Admin account status cannot be altered directly");
                }
                user.setAccountNonLocked(false);
                break;
            case "unlock":
                user.setAccountNonLocked(true);
                break;
            default:
                throw new IllegalArgumentException("Invalid status action: " + action);
        }

        User savedUser = userRepository.save(user);

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(savedUser.getId(), savedUser.getEmail(), com.redis.audit.entity.AuditActionType.ROLE_CHANGED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.ROLE, String.valueOf(savedUser.getId()), "Admin updated user details/roles for email: " + savedUser.getEmail());
        }

        return toUserResponse(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductStatsResponse getProductStats() {
        log.info("Super Admin: Generating product statistics");
        long totalProducts = productRepository.count();
        long outOfStock = productRepository.countOutOfStock();
        long lowStock = productRepository.countLowStock(LOW_STOCK_THRESHOLD);
        Double avgRatingVal = productRepository.getAverageRating();
        BigDecimal avgRating = avgRatingVal != null 
                ? BigDecimal.valueOf(avgRatingVal).setScale(2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        return ProductStatsResponse.builder()
                .totalProducts(totalProducts)
                .averageRating(avgRating)
                .outOfStockCount(outOfStock)
                .lowStockCount(lowStock)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductInventory(Pageable pageable) {
        log.info("Super Admin: Retrieving product inventory overview");
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public UserResponse createAdmin(RegisterRequest request) {
        log.info("Super Admin: Creating new business admin account: {}", request.getEmail());
        
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User admin = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_ADMIN)
                .accountEnabled(true)
                .accountNonLocked(true)
                .securityQuestion(request.getSecurityQuestion())
                .securityAnswer(request.getSecurityAnswer() != null && !request.getSecurityAnswer().isBlank()
                        ? passwordEncoder.encode(request.getSecurityAnswer())
                        : null)
                .build();

        User savedAdmin = userRepository.save(admin);
        log.info("Business admin account created successfully with ID: {}", savedAdmin.getId());

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(savedAdmin.getId(), savedAdmin.getEmail(), com.redis.audit.entity.AuditActionType.ADMIN_OPERATION, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.USER, String.valueOf(savedAdmin.getId()), "Admin created new sub-admin account with email: " + savedAdmin.getEmail());
        }

        return toUserResponse(savedAdmin);
    }

    @Override
    @Transactional
    public void resetUserPassword(Long id, String newPassword) {
        log.info("Super Admin: Resetting password for user ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate active refresh tokens and active user session
        refreshTokenService.deleteByUserId(user.getId());
        userSessionService.logoutSession(user.getEmail());
        
        log.info("Password reset and active sessions invalidated for user ID: {}", user.getId());

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(user.getId(), user.getEmail(), com.redis.audit.entity.AuditActionType.PASSWORD_RESET, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.USER, String.valueOf(user.getId()), "Admin reset password for user ID: " + user.getId());
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getActualUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .accountEnabled(user.isAccountEnabled())
                .accountNonLocked(user.isAccountNonLocked())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserSessionResponse toSessionResponse(UserSession session) {
        return UserSessionResponse.builder()
                .userId(session.getUserId())
                .username(session.getUsername())
                .email(session.getEmail())
                .loginTime(session.getLoginTime())
                .logoutTime(session.getLogoutTime())
                .lastActivity(session.getLastActivity())
                .status(session.getStatus())
                .build();
    }
}
