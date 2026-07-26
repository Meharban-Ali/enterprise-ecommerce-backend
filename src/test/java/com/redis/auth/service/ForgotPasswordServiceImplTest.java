package com.redis.auth.service;

import com.redis.auth.dto.request.ForgotPasswordRequest;
import com.redis.auth.dto.request.ResetPasswordRequest;
import com.redis.auth.entity.PasswordResetToken;
import com.redis.auth.repository.PasswordResetTokenRepository;
import com.redis.common.dto.ApiResponse;
import com.redis.notification.event.NotificationEventPublisher;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import com.redis.user.service.UserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForgotPasswordServiceImpl Unit Tests — Link-Based Reset")
class ForgotPasswordServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    @InjectMocks
    private ForgotPasswordServiceImpl forgotPasswordService;

    private User testUser;
    private ForgotPasswordRequest forgotRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(forgotPasswordService, "frontendUrl", "https://easyshoping.fun");

        testUser = User.builder()
                .id(101L)
                .email("user@example.com")
                .password("OldEncodedPassword")
                .build();

        forgotRequest = ForgotPasswordRequest.builder()
                .email("user@example.com")
                .build();
    }

    @Test
    @DisplayName("✅ Success: Should generate reset token and dispatch email with captured real reset link")
    void requestPasswordReset_Success_DispatchesCapturedLink() {
        // Arrange
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

        // Act
        ApiResponse<Void> response = forgotPasswordService.requestPasswordReset(forgotRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("If this email is registered, a password reset link has been sent");

        verify(tokenRepository).deleteByUser(testUser);

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUser()).isEqualTo(testUser);
        assertThat(savedToken.getToken()).isNotBlank();
        assertThat(savedToken.isUsed()).isFalse();
        assertThat(savedToken.getExpiryDate()).isAfter(LocalDateTime.now());

        // Capture actual reset URL passed to notification publisher
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationEventPublisher).publishPasswordReset(eq(101L), eq("user@example.com"), urlCaptor.capture());

        String capturedUrl = urlCaptor.getValue();
        assertThat(capturedUrl).startsWith("https://easyshoping.fun/reset-password?token=");
        assertThat(capturedUrl).endsWith(savedToken.getToken());
    }

    @Test
    @DisplayName("🛡️ Security: Anti-enumeration — Non-existent email returns generic success without generating token or sending email")
    void requestPasswordReset_NonExistentEmail_AntiEnumeration() {
        // Arrange
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());
        ForgotPasswordRequest unknownRequest = ForgotPasswordRequest.builder().email("unknown@example.com").build();

        // Act
        ApiResponse<Void> response = forgotPasswordService.requestPasswordReset(unknownRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("If this email is registered, a password reset link has been sent");

        verify(tokenRepository, never()).save(any());
        verify(notificationEventPublisher, never()).publishPasswordReset(any(), any(), any());
    }

    @Test
    @DisplayName("✅ Success: Reset password with valid token updates password, marks token as used, and clears sessions")
    void resetPassword_Success() {
        // Arrange
        PasswordResetToken validToken = PasswordResetToken.builder()
                .id(1L)
                .user(testUser)
                .token("valid-uuid-token")
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token("valid-uuid-token")
                .newPassword("NewStrongPassword123!")
                .confirmPassword("NewStrongPassword123!")
                .build();

        when(tokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));
        when(passwordEncoder.encode("NewStrongPassword123!")).thenReturn("EncodedNewPassword");

        // Act
        ApiResponse<Void> response = forgotPasswordService.resetPassword(resetRequest);

        // Assert
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Password reset successfully.");

        assertThat(testUser.getPassword()).isEqualTo("EncodedNewPassword");
        assertThat(validToken.isUsed()).isTrue();

        verify(tokenRepository).save(validToken);
        verify(userRepository).save(testUser);
        verify(refreshTokenService).deleteByUserId(101L);
        verify(userSessionService).logoutSession("user@example.com");
        verify(notificationEventPublisher).publishPasswordChanged(101L, "user@example.com");
    }

    @Test
    @DisplayName("❌ Failure: Reset password with invalid / non-existent token throws exception")
    void resetPassword_TokenNotFound_ThrowsException() {
        // Arrange
        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token("non-existent-token")
                .newPassword("NewStrongPassword123!")
                .build();

        when(tokenRepository.findByToken("non-existent-token")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> forgotPasswordService.resetPassword(resetRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or non-existent password reset token");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Failure: Reset password with expired token throws exception")
    void resetPassword_ExpiredToken_ThrowsException() {
        // Arrange
        PasswordResetToken expiredToken = PasswordResetToken.builder()
                .id(1L)
                .user(testUser)
                .token("expired-token")
                .expiryDate(LocalDateTime.now().minusMinutes(5)) // Expired 5 mins ago
                .used(false)
                .build();

        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token("expired-token")
                .newPassword("NewStrongPassword123!")
                .build();

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        assertThatThrownBy(() -> forgotPasswordService.resetPassword(resetRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password reset token has expired");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("❌ Failure: Reset password with already used token throws exception")
    void resetPassword_ReusedToken_ThrowsException() {
        // Arrange
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .id(1L)
                .user(testUser)
                .token("used-token")
                .expiryDate(LocalDateTime.now().plusMinutes(10))
                .used(true) // Token already used
                .build();

        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token("used-token")
                .newPassword("NewStrongPassword123!")
                .build();

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(usedToken));

        // Act & Assert
        assertThatThrownBy(() -> forgotPasswordService.resetPassword(resetRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password reset token has already been used");

        verify(userRepository, never()).save(any());
    }
}
