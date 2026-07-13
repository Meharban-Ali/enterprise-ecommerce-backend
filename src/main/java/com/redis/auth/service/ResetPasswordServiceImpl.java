package com.redis.auth.service;

import com.redis.audit.entity.AuditActionType;
import com.redis.notification.event.NotificationEventPublisher;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.audit.entity.AuditStatus;

import com.redis.infrastructure.config.RedisCacheConfig;
import com.redis.common.exception.InvalidSecurityAnswerException;
import com.redis.common.exception.PasswordMismatchException;
import com.redis.common.exception.SecurityQuestionNotSetException;
import com.redis.user.exception.UserNotFoundException;
import com.redis.auth.dto.request.ResetPasswordRequest;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordServiceImpl implements ResetPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationEventPublisher notificationEventPublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    @Override
    @Transactional
    @CacheEvict(value = RedisCacheConfig.CACHE_USERS, key = "#request.email", allEntries = false, beforeInvocation = false)
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing reset password request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Reset password failed — email not found");
                    return new UserNotFoundException(request.getEmail());
                });

        // Flow 1: Reset via Old Password Verification
        if (request.getOldPassword() != null && !request.getOldPassword().isBlank()) {
            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                log.warn("Reset password failed — old password mismatch for user ID: {}", user.getId());
                throw new PasswordMismatchException("Old password does not match");
            }
        } 
        // Flow 2: Reset via Security Answer Verification (Old Flow)
        else {
            if (request.getSecurityAnswer() == null || request.getSecurityAnswer().isBlank()) {
                log.warn("Reset password failed — security answer or old password required");
                throw new IllegalArgumentException("Either old password or security answer must be provided");
            }
            if (user.getSecurityAnswer() == null || user.getSecurityAnswer().isBlank()) {
                log.warn("Reset password failed — security question/answer not set for user ID: {}", user.getId());
                throw new SecurityQuestionNotSetException(request.getEmail());
            }
            if (!passwordEncoder.matches(request.getSecurityAnswer(), user.getSecurityAnswer())) {
                log.warn("Reset password failed — invalid security answer for user ID: {}", user.getId());
                throw new InvalidSecurityAnswerException();
            }
        }

        // Validate confirm password if provided
        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("Reset password failed — confirm password mismatch for user ID: {}", user.getId());
                throw new PasswordMismatchException("Confirm password does not match new password");
            }
        }

        // Encrypt new password using PasswordEncoder
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password reset successfully for user ID: {}", user.getId());

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(user.getId(), user.getEmail(), com.redis.audit.entity.AuditActionType.PASSWORD_RESET, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.USER, String.valueOf(user.getId()), "Password reset successfully");
        }

        // Publish password changed event
        try {
            notificationEventPublisher.publishPasswordChanged(user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish password changed event for user ID: {}", user.getId(), e);
        }
    }
}
