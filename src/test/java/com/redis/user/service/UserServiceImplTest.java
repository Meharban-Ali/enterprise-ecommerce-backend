package com.redis.user.service;

import com.redis.user.exception.UserAlreadyExistsException;
import com.redis.auth.dto.request.RegisterRequest;
import com.redis.auth.dto.response.RegisterResponse;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest testRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        testRequest = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("RawPassword123!")
                .build();

        testUser = User.builder()
                .id(100L)
                .username("john_doe")
                .email("john@example.com")
                .password("EncodedPassword123!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();
    }

    @Nested
    @DisplayName("registerUser() Tests")
    class RegisterUserTests {

        @Test
        @DisplayName("✅ Success: Should register user and return success response")
        void registerUser_Success() {
            // Arrange
            when(userRepository.existsByEmail(testRequest.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(testRequest.getPassword())).thenReturn("EncodedPassword123!");
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            // Act
            RegisterResponse response = userService.registerUser(testRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(100L);
            assertThat(response.getEmail()).isEqualTo("john@example.com");
            assertThat(response.getMessage()).isEqualTo("User registered successfully");

            verify(userRepository).existsByEmail(testRequest.getEmail());
            verify(passwordEncoder).encode(testRequest.getPassword());
            verify(userRepository).save(argThat(user -> 
                user.getUsername().equals(testRequest.getEmail()) &&
                user.getEmail().equals(testRequest.getEmail()) &&
                user.getPassword().equals("EncodedPassword123!") &&
                user.getRole() == Role.ROLE_USER &&
                user.isAccountEnabled() &&
                user.isAccountNonLocked()
            ));
        }

        @Test
        @DisplayName("❌ Failure: Should throw UserAlreadyExistsException when email exists")
        void registerUser_ThrowsUserAlreadyExistsException() {
            // Arrange
            when(userRepository.existsByEmail(testRequest.getEmail())).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.registerUser(testRequest))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining(testRequest.getEmail());

            verify(userRepository).existsByEmail(testRequest.getEmail());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("❌ Failure: Should throw IllegalArgumentException when security question is provided but answer is missing")
        void registerUser_ThrowsIllegalArgumentException_QuestionWithoutAnswer() {
            // Arrange
            testRequest.setSecurityQuestion("What is your pet's name?");
            testRequest.setSecurityAnswer("");

            // Act & Assert
            assertThatThrownBy(() -> userService.registerUser(testRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Both security question and security answer must be provided");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("❌ Failure: Should throw IllegalArgumentException when security answer is provided but question is missing")
        void registerUser_ThrowsIllegalArgumentException_AnswerWithoutQuestion() {
            // Arrange
            testRequest.setSecurityQuestion(null);
            testRequest.setSecurityAnswer("Fido");

            // Act & Assert
            assertThatThrownBy(() -> userService.registerUser(testRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Both security question and security answer must be provided");

            verify(userRepository, never()).save(any(User.class));
        }
    }
}
