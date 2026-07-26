package com.redis.auth.service;

import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.auth.dto.request.ForgotPasswordRequest;
import com.redis.auth.dto.request.ResetPasswordRequest;
import com.redis.auth.entity.PasswordResetToken;
import com.redis.auth.repository.PasswordResetTokenRepository;
import com.redis.common.dto.ApiResponse;
import com.redis.common.entity.ResourceType;
import com.redis.common.exception.PasswordMismatchException;
import com.redis.notification.event.NotificationEventPublisher;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import com.redis.user.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final UserSessionService userSessionService;
    private final NotificationEventPublisher notificationEventPublisher;

    @Autowired(required = false)
    private AuditEventPublisher auditEventPublisher;

    @Value("${app.frontend.url:https://easyshoping.fun}")
    private String frontendUrl;

    private static final long TOKEN_EXPIRY_MINUTES = 15;
    private static final String GENERIC_RESET_MESSAGE = "If this email is registered, a password reset link has been sent.";

    @Override
    @Transactional
    public ApiResponse<Void> requestPasswordReset(ForgotPasswordRequest request) {
        log.info("Processing password reset request for email: {}", request.getEmail());

        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {} — Returning anti-enumeration response", request.getEmail());
            return ApiResponse.success(GENERIC_RESET_MESSAGE);
        }

        User user = userOptional.get();

        // Delete any existing reset token for this user to ensure single active token
        tokenRepository.deleteByUser(user);

        // Generate secure random token
        String tokenStr = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(tokenStr)
                .expiryDate(LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        String resetUrl = frontendUrl + "/reset-password?token=" + tokenStr;
        log.info("Generated password reset token for user ID: {}. Dispatching email notification.", user.getId());

        try {
            notificationEventPublisher.publishPasswordReset(user.getId(), user.getEmail(), resetUrl);
        } catch (Exception e) {
            log.error("Failed to publish password reset notification for user ID: {}", user.getId(), e);
        }

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(user.getId(), user.getEmail(), AuditActionType.PASSWORD_RESET, AuditStatus.SUCCESS,
                    ResourceType.USER, String.valueOf(user.getId()), "Password reset link requested");
        }

        return ApiResponse.success(GENERIC_RESET_MESSAGE);
    }

    @Override
    @Transactional
    public ApiResponse<Void> resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset via link token");

        PasswordResetToken resetToken = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> {
                    log.warn("Password reset failed — token not found");
                    return new IllegalArgumentException("Invalid or non-existent password reset token");
                });

        if (resetToken.isUsed()) {
            log.warn("Password reset failed — token already used for user ID: {}", resetToken.getUser().getId());
            throw new IllegalArgumentException("Password reset token has already been used");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Password reset failed — token expired at {} for user ID: {}", resetToken.getExpiryDate(), resetToken.getUser().getId());
            throw new IllegalArgumentException("Password reset token has expired");
        }

        if (request.getConfirmPassword() != null && !request.getConfirmPassword().isBlank()) {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new PasswordMismatchException("Confirm password does not match new password");
            }
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        // Invalidate active sessions and refresh tokens
        refreshTokenService.deleteByUserId(user.getId());
        userSessionService.logoutSession(user.getEmail());

        log.info("Password updated successfully via link reset token for user ID: {}", user.getId());

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(user.getId(), user.getEmail(), AuditActionType.PASSWORD_RESET, AuditStatus.SUCCESS,
                    ResourceType.USER, String.valueOf(user.getId()), "Password reset successfully via link token");
        }

        try {
            notificationEventPublisher.publishPasswordChanged(user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to publish password changed event for user ID: {}", user.getId(), e);
        }

        return ApiResponse.success("Password reset successfully.");
    }
}
