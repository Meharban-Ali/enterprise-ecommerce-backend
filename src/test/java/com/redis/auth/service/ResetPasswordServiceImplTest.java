package com.redis.auth.service;

import com.redis.common.exception.InvalidSecurityAnswerException;
import com.redis.common.exception.PasswordMismatchException;
import com.redis.common.exception.SecurityQuestionNotSetException;
import com.redis.user.exception.UserNotFoundException;
import com.redis.auth.dto.request.ResetPasswordRequest;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResetPasswordServiceImpl Unit Tests")
class ResetPasswordServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ResetPasswordServiceImpl resetPasswordService;

    private ResetPasswordRequest testRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        testRequest = ResetPasswordRequest.builder()
                .email("user@example.com")
                .securityAnswer("ABC School")
                .newPassword("Password@123")
                .confirmPassword("Password@123")
                .build();

        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .securityQuestion("What is your first school name?")
                .securityAnswer("HashedABC")
                .password("OldHashedPassword")
                .build();
    }

    @Test
    @DisplayName("✅ Success: Should reset password when request is valid and security answer matches")
    void resetPassword_Success() {
        // Arrange
        when(userRepository.findByEmail(testRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testRequest.getSecurityAnswer(), testUser.getSecurityAnswer())).thenReturn(true);
        when(passwordEncoder.encode(testRequest.getNewPassword())).thenReturn("NewHashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        resetPasswordService.resetPassword(testRequest);

        // Assert
        verify(userRepository).findByEmail(testRequest.getEmail());
        verify(passwordEncoder).matches(testRequest.getSecurityAnswer(), testUser.getSecurityAnswer());
        verify(passwordEncoder).encode(testRequest.getNewPassword());
        verify(userRepository).save(argThat(user -> user.getPassword().equals("NewHashedPassword")));
    }

    @Test
    @DisplayName("❌ Failure: Should throw UserNotFoundException when user is not found")
    void resetPassword_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(testRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> resetPasswordService.resetPassword(testRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("user@example.com");

        verify(userRepository).findByEmail(testRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("❌ Failure: Should throw SecurityQuestionNotSetException when user question is not set")
    void resetPassword_SecurityQuestionNotSet() {
        // Arrange
        testUser.setSecurityQuestion(null);
        testUser.setSecurityAnswer(null);
        when(userRepository.findByEmail(testRequest.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> resetPasswordService.resetPassword(testRequest))
                .isInstanceOf(SecurityQuestionNotSetException.class)
                .hasMessageContaining("user@example.com");

        verify(userRepository).findByEmail(testRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("❌ Failure: Should throw InvalidSecurityAnswerException when security answer is wrong")
    void resetPassword_InvalidAnswer() {
        // Arrange
        when(userRepository.findByEmail(testRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testRequest.getSecurityAnswer(), testUser.getSecurityAnswer())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> resetPasswordService.resetPassword(testRequest))
                .isInstanceOf(InvalidSecurityAnswerException.class);

        verify(userRepository).findByEmail(testRequest.getEmail());
        verify(passwordEncoder).matches(testRequest.getSecurityAnswer(), testUser.getSecurityAnswer());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("❌ Failure: Should throw PasswordMismatchException when new and confirm passwords do not match")
    void resetPassword_PasswordMismatch() {
        // Arrange
        testRequest.setConfirmPassword("DifferentPassword123!");
        when(userRepository.findByEmail(testRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(testRequest.getSecurityAnswer(), testUser.getSecurityAnswer())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> resetPasswordService.resetPassword(testRequest))
                .isInstanceOf(PasswordMismatchException.class);

        verify(userRepository).findByEmail(testRequest.getEmail());
        verify(passwordEncoder).matches(testRequest.getSecurityAnswer(), testUser.getSecurityAnswer());
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }
}
