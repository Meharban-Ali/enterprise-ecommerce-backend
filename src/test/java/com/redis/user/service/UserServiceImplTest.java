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

    @Mock
    private com.redis.auth.service.RefreshTokenService refreshTokenService;

    @Mock
    private com.redis.notification.event.NotificationEventPublisher notificationEventPublisher;

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
    }

    @Nested
    @DisplayName("changePassword() Tests")
    class ChangePasswordTests {

        @Test
        @DisplayName("✅ Success: Should change password when correct old password and valid new password are provided")
        void changePassword_Success() {
            com.redis.user.dto.request.ChangePasswordRequest req = com.redis.user.dto.request.ChangePasswordRequest.builder()
                    .oldPassword("OldPassword123!")
                    .newPassword("NewPassword456!")
                    .confirmPassword("NewPassword456!")
                    .build();

            when(userRepository.findById(100L)).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("OldPassword123!", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("NewPassword456!")).thenReturn("EncodedNewPassword456!");

            userService.changePassword(100L, req);

            verify(passwordEncoder).encode("NewPassword456!");
            verify(userRepository).save(testUser);
            verify(refreshTokenService).deleteByUserId(100L);
        }

        @Test
        @DisplayName("✅ Success: Should trigger password change confirmation email using ArgumentCaptor")
        void changePassword_TriggersNotificationEmail_WithCorrectUserEmail() {
            com.redis.user.dto.request.ChangePasswordRequest req = com.redis.user.dto.request.ChangePasswordRequest.builder()
                    .oldPassword("OldPassword123!")
                    .newPassword("NewPassword456!")
                    .confirmPassword("NewPassword456!")
                    .build();

            when(userRepository.findById(100L)).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("OldPassword123!", testUser.getPassword())).thenReturn(true);
            when(passwordEncoder.encode("NewPassword456!")).thenReturn("EncodedNewPassword456!");

            userService.changePassword(100L, req);

            org.mockito.ArgumentCaptor<Long> userIdCaptor = org.mockito.ArgumentCaptor.forClass(Long.class);
            org.mockito.ArgumentCaptor<String> emailCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

            verify(notificationEventPublisher).publishPasswordChanged(userIdCaptor.capture(), emailCaptor.capture());

            assertThat(userIdCaptor.getValue()).isEqualTo(100L);
            assertThat(emailCaptor.getValue()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("❌ Failure: Should throw IllegalArgumentException when old password is incorrect")
        void changePassword_WrongOldPassword_ThrowsException() {
            com.redis.user.dto.request.ChangePasswordRequest req = com.redis.user.dto.request.ChangePasswordRequest.builder()
                    .oldPassword("WrongOldPassword123!")
                    .newPassword("NewPassword456!")
                    .confirmPassword("NewPassword456!")
                    .build();

            when(userRepository.findById(100L)).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("WrongOldPassword123!", testUser.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> userService.changePassword(100L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Current password does not match");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("❌ Failure: Should throw IllegalArgumentException when new password and confirm password differ")
        void changePassword_ConfirmMismatch_ThrowsException() {
            com.redis.user.dto.request.ChangePasswordRequest req = com.redis.user.dto.request.ChangePasswordRequest.builder()
                    .oldPassword("OldPassword123!")
                    .newPassword("NewPassword456!")
                    .confirmPassword("DifferentPassword456!")
                    .build();

            when(userRepository.findById(100L)).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("OldPassword123!", testUser.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> userService.changePassword(100L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("New password and confirm password do not match");

            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("❌ Failure: Should throw IllegalArgumentException when new password is identical to old password")
        void changePassword_SamePassword_ThrowsException() {
            com.redis.user.dto.request.ChangePasswordRequest req = com.redis.user.dto.request.ChangePasswordRequest.builder()
                    .oldPassword("OldPassword123!")
                    .newPassword("OldPassword123!")
                    .confirmPassword("OldPassword123!")
                    .build();

            when(userRepository.findById(100L)).thenReturn(java.util.Optional.of(testUser));
            when(passwordEncoder.matches("OldPassword123!", testUser.getPassword())).thenReturn(true);

            assertThatThrownBy(() -> userService.changePassword(100L, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("New password cannot be the same as the current password");

            verify(userRepository, never()).save(any(User.class));
        }
    }
}
