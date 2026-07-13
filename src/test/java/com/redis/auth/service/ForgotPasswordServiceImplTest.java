package com.redis.auth.service;

import com.redis.common.exception.SecurityQuestionNotSetException;
import com.redis.user.exception.UserNotFoundException;
import com.redis.auth.dto.request.ForgotPasswordRequest;
import com.redis.auth.dto.response.ForgotPasswordResponse;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ForgotPasswordServiceImpl Unit Tests")
class ForgotPasswordServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ForgotPasswordServiceImpl forgotPasswordService;

    private ForgotPasswordRequest testRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        testRequest = ForgotPasswordRequest.builder()
                .email("user@example.com")
                .build();

        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .securityQuestion("What is your first school name?")
                .securityAnswer("EncodedAnswer")
                .build();
    }

    @Test
    @DisplayName("✅ Success: Should retrieve security question when user exists and question is configured")
    void retrieveSecurityQuestion_Success() {
        // Arrange
        when(userRepository.findByEmail(testRequest.getEmail())).thenReturn(Optional.of(testUser));

        // Act
        ForgotPasswordResponse response = forgotPasswordService.retrieveSecurityQuestion(testRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getSecurityQuestion()).isEqualTo("What is your first school name?");
        verify(userRepository).findByEmail(testRequest.getEmail());
    }

    @Test
    @DisplayName("❌ Failure: Should throw UserNotFoundException when email does not exist")
    void retrieveSecurityQuestion_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(testRequest.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> forgotPasswordService.retrieveSecurityQuestion(testRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("user@example.com");
        verify(userRepository).findByEmail(testRequest.getEmail());
    }

    @Test
    @DisplayName("❌ Failure: Should throw SecurityQuestionNotSetException when question is not configured")
    void retrieveSecurityQuestion_QuestionNotSet() {
        // Arrange
        testUser.setSecurityQuestion(null);
        testUser.setSecurityAnswer(null);
        when(userRepository.findByEmail(testRequest.getEmail())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> forgotPasswordService.retrieveSecurityQuestion(testRequest))
                .isInstanceOf(SecurityQuestionNotSetException.class)
                .hasMessageContaining("user@example.com");
        verify(userRepository).findByEmail(testRequest.getEmail());
    }
}
