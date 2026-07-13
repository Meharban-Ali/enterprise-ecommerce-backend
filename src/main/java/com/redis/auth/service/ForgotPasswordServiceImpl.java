package com.redis.auth.service;

import com.redis.audit.entity.AuditActionType;
import com.redis.user.service.UserSessionService;
import com.redis.notification.event.NotificationEventPublisher;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.auth.service.RefreshTokenService;
import com.redis.audit.entity.AuditStatus;

import com.redis.common.exception.InvalidSecurityAnswerException;
import com.redis.common.exception.PasswordMismatchException;
import com.redis.common.exception.SecurityQuestionNotSetException;
import com.redis.user.exception.UserNotFoundException;
import com.redis.auth.dto.request.ForgotPasswordRequest;
import com.redis.auth.dto.request.ForgotPasswordResetRequest;
import com.redis.auth.dto.request.ForgotPasswordVerifyRequest;
import com.redis.common.dto.ApiResponse;
import com.redis.auth.dto.response.ForgotPasswordResponse;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import com.redis.infrastructure.cache.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserSessionService userSessionService;
    private final ObjectProvider<RedisUtil> redisUtilProvider;
    private final NotificationEventPublisher notificationEventPublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    // Thread-safe local cache as fallback if Redis is down
    private final ConcurrentHashMap<String, LocalDateTime> localVerificationCache = new ConcurrentHashMap<>();

    private static final String REDIS_VERIFY_PREFIX = "password-reset-verified::";
    private static final long VERIFY_TTL_MINUTES = 5;

    @Override
    @Transactional(readOnly = true)
    public ForgotPasswordResponse retrieveSecurityQuestion(ForgotPasswordRequest request) {
        log.info("Processing forgot password request (security question lookup) for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Forgot password lookup failed — email not found in database: {}", request.getEmail());
                    return new UserNotFoundException(request.getEmail());
                });

        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()) {
            log.warn("Forgot password lookup failed — security question not configured for user ID: {}", user.getId());
            throw new SecurityQuestionNotSetException(request.getEmail());
        }

        log.info("Security question retrieved successfully for user ID: {}", user.getId());
        return ForgotPasswordResponse.builder()
                .success(true)
                .securityQuestion(user.getSecurityQuestion())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<String> verifySecurityAnswer(ForgotPasswordVerifyRequest request) {
        log.info("Verifying security answer for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        if (user.getSecurityQuestion() == null || user.getSecurityQuestion().isBlank()
                || user.getSecurityAnswer() == null || user.getSecurityAnswer().isBlank()) {
            throw new SecurityQuestionNotSetException(request.getEmail());
        }

        // Verify matches
        if (!passwordEncoder.matches(request.getSecurityAnswer(), user.getSecurityAnswer())) {
            log.warn("Security verification failed — invalid answer for user ID: {}", user.getId());
            throw new InvalidSecurityAnswerException();
        }

        log.info("Security verification successful for user ID: {}", user.getId());

        // Cache verification state for 5 minutes
        String cacheKey = REDIS_VERIFY_PREFIX + request.getEmail();
        RedisUtil redisUtil = redisUtilProvider.getIfAvailable();
        if (redisUtil != null) {
            redisUtil.set(cacheKey, "true", VERIFY_TTL_MINUTES);
        }
        // Always store in local fallback cache too to ensure seamless functionality
        localVerificationCache.put(request.getEmail(), LocalDateTime.now().plusMinutes(VERIFY_TTL_MINUTES));

        return ApiResponse.success("Verification successful. Please provide your new password.", null);
    }

    @Override
    @Transactional
    public ApiResponse<Void> resetForgotPassword(ForgotPasswordResetRequest request) {
        log.info("Processing secure forgot-password reset for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException(request.getEmail()));

        // Check if verified
        String cacheKey = REDIS_VERIFY_PREFIX + request.getEmail();
        boolean verified = false;

        RedisUtil redisUtil = redisUtilProvider.getIfAvailable();
        if (redisUtil != null) {
            Object val = redisUtil.get(cacheKey);
            if (val != null && "true".equals(val.toString())) {
                verified = true;
            }
        }

        if (!verified) {
            LocalDateTime expiry = localVerificationCache.get(request.getEmail());
            if (expiry != null && expiry.isAfter(LocalDateTime.now())) {
                verified = true;
            }
        }

        if (!verified) {
            log.warn("Password reset rejected — security verification required or expired for email: {}", request.getEmail());
            throw new IllegalArgumentException("Security verification has expired or was not completed");
        }

        // Check confirm password if provided
        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new PasswordMismatchException("Confirm password does not match new password");
            }
        }

        // Update password using BCrypt
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Evict verification cache
        if (redisUtil != null) {
            redisUtil.delete(cacheKey);
        }
        localVerificationCache.remove(request.getEmail());

        // Invalidate old refresh tokens
        refreshTokenService.deleteByUserId(user.getId());

        // Logout active session
        userSessionService.logoutSession(user.getEmail());

        log.info("Password updated successfully via forgot-password flow for user ID: {}", user.getId());

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(user.getId(), user.getEmail(), com.redis.audit.entity.AuditActionType.PASSWORD_RESET, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.USER, String.valueOf(user.getId()), "Password reset via forgot password flow");
        }

        // Publish password reset notification
        try {
            notificationEventPublisher.publishPasswordChanged(user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish password changed event for user ID: {}", user.getId(), e);
        }

        return ApiResponse.success("Password reset successfully.");
    }
}
