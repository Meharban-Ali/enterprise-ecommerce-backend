package com.redis.auth.service;

import com.redis.user.service.UserSessionService;
import com.redis.auth.service.JwtService;
import com.redis.auth.service.RefreshTokenService;

import com.redis.auth.exception.TokenRefreshException;
import com.redis.auth.dto.request.LoginRequest;
import com.redis.common.dto.LogoutRequest;
import com.redis.auth.dto.request.RefreshTokenRequest;
import com.redis.auth.dto.response.LoginResponse;
import com.redis.auth.dto.response.RefreshTokenResponse;
import com.redis.auth.entity.RefreshToken;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Unit Tests")
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserSessionService userSessionService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@example.com")
                .password("EncodedPassword!")
                .role(Role.ROLE_USER)
                .accountEnabled(true)
                .accountNonLocked(true)
                .build();

        testRefreshToken = RefreshToken.builder()
                .id(10L)
                .token("mock-refresh-token")
                .user(testUser)
                .build();
    }

    @Nested
    @DisplayName("login() Tests")
    class LoginTests {

        @Test
        @DisplayName("✅ Success: Should login successfully and return access & refresh tokens")
        void login_Success() {
            // Arrange
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("john@example.com")
                    .password("RawPassword!")
                    .build();

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            when(jwtService.generateToken(testUser)).thenReturn("mock-access-token");
            when(refreshTokenService.createRefreshToken(testUser.getId())).thenReturn(testRefreshToken);

            // Act
            LoginResponse response = authService.login(loginRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("mock-access-token");
            assertThat(response.getRefreshToken()).isEqualTo("mock-refresh-token");

            verify(authenticationManager).authenticate(argThat(auth ->
                auth.getPrincipal().equals("john@example.com") &&
                auth.getCredentials().equals("RawPassword!")
            ));
            verify(jwtService).generateToken(testUser);
            verify(refreshTokenService).createRefreshToken(testUser.getId());
        }

        @Test
        @DisplayName("❌ Failure: Should propagate BadCredentialsException if authentication fails")
        void login_ThrowsBadCredentialsException() {
            // Arrange
            LoginRequest loginRequest = LoginRequest.builder()
                    .email("john@example.com")
                    .password("WrongPassword!")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));
            when(userRepository.findByEmail(anyString())).thenReturn(java.util.Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid credentials");

            verify(jwtService, never()).generateToken(any(User.class));
            verify(refreshTokenService, never()).createRefreshToken(anyLong());
        }
    }

    @Nested
    @DisplayName("refreshToken() Tests")
    class RefreshTokenTests {

        @Test
        @DisplayName("✅ Success: Should generate new access token using a valid refresh token")
        void refreshToken_Success() {
            // Arrange
            RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                    .refreshToken("mock-refresh-token")
                    .build();

            when(refreshTokenService.findByToken("mock-refresh-token")).thenReturn(Optional.of(testRefreshToken));
            when(refreshTokenService.verifyExpiration(testRefreshToken)).thenReturn(testRefreshToken);
            when(jwtService.generateToken(testUser)).thenReturn("new-mock-access-token");

            // Act
            RefreshTokenResponse response = authService.refreshToken(refreshRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-mock-access-token");

            verify(refreshTokenService).findByToken("mock-refresh-token");
            verify(refreshTokenService).verifyExpiration(testRefreshToken);
            verify(jwtService).generateToken(testUser);
        }

        @Test
        @DisplayName("❌ Failure: Should throw TokenRefreshException when refresh token is missing or expired")
        void refreshToken_ThrowsTokenRefreshException() {
            // Arrange
            RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                    .refreshToken("invalid-or-expired-token")
                    .build();

            when(refreshTokenService.findByToken("invalid-or-expired-token")).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> authService.refreshToken(refreshRequest))
                    .isInstanceOf(TokenRefreshException.class)
                    .hasMessageContaining("Refresh token is not present in database");

            verify(refreshTokenService).findByToken("invalid-or-expired-token");
            verify(refreshTokenService, never()).verifyExpiration(any(RefreshToken.class));
            verify(jwtService, never()).generateToken(any(User.class));
        }
    }

    @Nested
    @DisplayName("logout() Tests")
    class LogoutTests {

        @Test
        @DisplayName("✅ Success: Should delete/revoke refresh token from the database")
        void logout_Success() {
            // Arrange
            LogoutRequest logoutRequest = LogoutRequest.builder()
                    .refreshToken("mock-refresh-token")
                    .build();

            // Act
            authService.logout(logoutRequest);

            // Assert
            verify(refreshTokenService).deleteByToken("mock-refresh-token");
        }
    }
}
