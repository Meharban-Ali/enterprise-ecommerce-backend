package com.redis.auth.entity;

import com.redis.user.service.UserServiceImpl;
import com.redis.user.service.UserSessionService;
import com.redis.auth.service.ResetPasswordServiceImpl;
import com.redis.auth.service.RefreshTokenService;
import com.redis.auth.service.ForgotPasswordServiceImpl;

import com.redis.common.exception.InvalidSecurityAnswerException;
import com.redis.common.exception.PasswordMismatchException;
import com.redis.auth.dto.request.ForgotPasswordRequest;
import com.redis.auth.dto.request.ForgotPasswordResetRequest;
import com.redis.auth.dto.request.ForgotPasswordVerifyRequest;
import com.redis.common.dto.ProfileUpdateRequest;
import com.redis.auth.dto.request.ResetPasswordRequest;
import com.redis.common.dto.ApiResponse;
import com.redis.auth.dto.response.ForgotPasswordResponse;
import com.redis.user.dto.response.UserResponse;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import com.redis.infrastructure.cache.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User and Authentication Enhancement Unit Tests")
class UserAndAuthEnhancementTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private ObjectProvider<RedisUtil> redisUtilProvider;

    @InjectMocks
    private UserServiceImpl userService;

    @InjectMocks
    private ForgotPasswordServiceImpl forgotPasswordService;

    @InjectMocks
    private ResetPasswordServiceImpl resetPasswordService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("bcrypt_hashed_password")
                .role(Role.ROLE_USER)
                .securityQuestion("Pet name?")
                .securityAnswer("encoded_answer")
                .build();
    }

    @Test
    @DisplayName("✅ Success: Retrieve security question associated with email")
    void forgotPassword_RetrievesQuestion() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email("john@example.com")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

        ForgotPasswordResponse response = forgotPasswordService.retrieveSecurityQuestion(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSecurityQuestion()).isEqualTo("Pet name?");
    }

    @Test
    @DisplayName("✅ Success: Verify security answer and cache verification state")
    void verifySecurityAnswer_Success() {
        ForgotPasswordVerifyRequest request = ForgotPasswordVerifyRequest.builder()
                .email("john@example.com")
                .securityAnswer("Buddy")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Buddy", "encoded_answer")).thenReturn(true);
        when(redisUtilProvider.getIfAvailable()).thenReturn(null); // Local cache fallback will be tested

        ApiResponse<String> response = forgotPasswordService.verifySecurityAnswer(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("Verification successful");
    }

    @Test
    @DisplayName("❌ Failure: Verify wrong security answer throws exception")
    void verifySecurityAnswer_WrongAnswer_ThrowsException() {
        ForgotPasswordVerifyRequest request = ForgotPasswordVerifyRequest.builder()
                .email("john@example.com")
                .securityAnswer("Wrong")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Wrong", "encoded_answer")).thenReturn(false);

        assertThatThrownBy(() -> forgotPasswordService.verifySecurityAnswer(request))
                .isInstanceOf(InvalidSecurityAnswerException.class);
    }

    @Test
    @DisplayName("✅ Success: Reset password after verifying answer via local cache verification state")
    void resetForgotPassword_Success() {
        // 1. Verify first to populate local verification cache
        ForgotPasswordVerifyRequest verifyRequest = ForgotPasswordVerifyRequest.builder()
                .email("john@example.com")
                .securityAnswer("Buddy")
                .build();
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Buddy", "encoded_answer")).thenReturn(true);
        when(redisUtilProvider.getIfAvailable()).thenReturn(null);

        forgotPasswordService.verifySecurityAnswer(verifyRequest);

        // 2. Perform reset
        ForgotPasswordResetRequest resetRequest = ForgotPasswordResetRequest.builder()
                .email("john@example.com")
                .newPassword("NewSecurePassword@123")
                .confirmPassword("NewSecurePassword@123")
                .build();

        when(passwordEncoder.encode("NewSecurePassword@123")).thenReturn("new_bcrypt_hash");

        ApiResponse<Void> response = forgotPasswordService.resetForgotPassword(resetRequest);

        assertThat(response.isSuccess()).isTrue();
        assertThat(testUser.getPassword()).isEqualTo("new_bcrypt_hash");
        verify(userRepository).save(testUser);
        verify(refreshTokenService).deleteByUserId(1L);
        verify(userSessionService).logoutSession("john@example.com");
    }

    @Test
    @DisplayName("❌ Failure: Reset password without verification throws exception")
    void resetForgotPassword_NotVerified_ThrowsException() {
        ForgotPasswordResetRequest resetRequest = ForgotPasswordResetRequest.builder()
                .email("john@example.com")
                .newPassword("NewSecurePassword@123")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(redisUtilProvider.getIfAvailable()).thenReturn(null);

        assertThatThrownBy(() -> forgotPasswordService.resetForgotPassword(resetRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired or was not completed");
    }

    @Test
    @DisplayName("✅ Success: Reset password using oldPassword verification flow")
    void resetPassword_OldPasswordFlow_Success() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("john@example.com")
                .oldPassword("plain_password_123")
                .newPassword("NewSuperSecretPassword123!")
                .confirmPassword("NewSuperSecretPassword123!")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plain_password_123", "bcrypt_hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("NewSuperSecretPassword123!")).thenReturn("new_bcrypt_hash");

        resetPasswordService.resetPassword(request);

        assertThat(testUser.getPassword()).isEqualTo("new_bcrypt_hash");
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("❌ Failure: Reset password with incorrect old password throws exception")
    void resetPassword_OldPasswordMismatch_ThrowsException() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .email("john@example.com")
                .oldPassword("wrong_old_password")
                .newPassword("NewSecret123!")
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong_old_password", "bcrypt_hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> resetPasswordService.resetPassword(request))
                .isInstanceOf(PasswordMismatchException.class);
    }

    @Test
    @DisplayName("✅ Success: Update user profile details")
    void updateProfile_Success() {
        ProfileUpdateRequest request = ProfileUpdateRequest.builder()
                .username("john_new")
                .email("john_new@example.com")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByEmail("john_new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse response = userService.updateProfile(1L, request);

        assertThat(response.getUsername()).isEqualTo("john_new");
        assertThat(response.getEmail()).isEqualTo("john_new@example.com");
        verify(userRepository).save(testUser);
    }
}
