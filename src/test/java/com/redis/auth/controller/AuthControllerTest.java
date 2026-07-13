package com.redis.auth.controller;

import com.redis.user.entity.User;
import com.redis.infrastructure.config.CorsProperties;
import com.redis.infrastructure.security.CustomAccessDeniedHandler;
import com.redis.infrastructure.config.SecurityConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.auth.exception.TokenRefreshException;
import com.redis.user.exception.UserAlreadyExistsException;
import com.redis.user.exception.UserNotFoundException;
import com.redis.common.exception.InvalidSecurityAnswerException;
import com.redis.common.exception.PasswordMismatchException;
import com.redis.common.exception.SecurityQuestionNotSetException;
import com.redis.auth.dto.request.ForgotPasswordRequest;
import com.redis.auth.dto.request.LoginRequest;
import com.redis.common.dto.LogoutRequest;
import com.redis.auth.dto.request.RegisterRequest;
import com.redis.auth.dto.request.ResetPasswordRequest;
import com.redis.auth.dto.request.RefreshTokenRequest;
import com.redis.common.dto.ApiResponse;
import com.redis.auth.dto.response.ForgotPasswordResponse;
import com.redis.auth.dto.response.LoginResponse;
import com.redis.auth.dto.response.RegisterResponse;
import com.redis.auth.dto.response.RefreshTokenResponse;
import com.redis.auth.service.AuthService;
import com.redis.auth.service.ForgotPasswordService;
import com.redis.auth.service.ResetPasswordService;
import com.redis.auth.service.JwtService;
import com.redis.user.service.UserService;
import com.redis.auth.entity.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.Import;

@WebMvcTest(AuthController.class)
@Import(com.redis.infrastructure.config.SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthController Slice Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private ForgotPasswordService forgotPasswordService;

    @MockBean
    private ResetPasswordService resetPasswordService;

    // Mock beans required to satisfy Spring Security configuration context loading
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private com.redis.infrastructure.security.CustomAccessDeniedHandler accessDeniedHandler;

    @MockBean
    private com.redis.infrastructure.config.CorsProperties corsProperties;

    @MockBean
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    private RegisterRequest validRegisterRequest;
    private RegisterResponse registerResponse;
    private LoginRequest validLoginRequest;
    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequest.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("RawPassword123!")
                .build();

        registerResponse = RegisterResponse.builder()
                .message("User registered successfully")
                .userId(1L)
                .email("john@example.com")
                .build();

        validLoginRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("RawPassword123!")
                .build();

        loginResponse = LoginResponse.builder()
                .accessToken("mock-access-token")
                .refreshToken("mock-refresh-token")
                .build();
    }

    @Nested
    @DisplayName("POST /api/auth/register Tests")
    class RegisterEndpoints {

        @Test
        @DisplayName("✅ Success: Should return 201 Created and response data on valid request")
        void register_Success() throws Exception {
            when(userService.registerUser(any(RegisterRequest.class))).thenReturn(registerResponse);

            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.email").value("john@example.com"));
        }

        @Test
        @DisplayName("❌ Failure: Should return 400 Bad Request on invalid parameters")
        void register_ValidationError() throws Exception {
            RegisterRequest invalidRequest = RegisterRequest.builder()
                    .username("jo") // Too short
                    .email("invalid-email") // Bad email format
                    .password("") // Blank
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors").isArray());
        }

        @Test
        @DisplayName("❌ Failure: Should return 409 Conflict when email already exists")
        void register_EmailAlreadyExists() throws Exception {
            when(userService.registerUser(any(RegisterRequest.class)))
                    .thenThrow(new UserAlreadyExistsException("john@example.com"));

            mockMvc.perform(post("/api/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRegisterRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("USER_ALREADY_EXISTS"))
                    .andExpect(jsonPath("$.message").value("User already exists with email: john@example.com"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login Tests")
    class LoginEndpoints {

        @Test
        @DisplayName("✅ Success: Should return 200 OK and tokens on valid login credentials")
        void login_Success() throws Exception {
            when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User logged in successfully"))
                    .andExpect(jsonPath("$.data.accessToken").value("mock-access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("mock-refresh-token"));
        }

        @Test
        @DisplayName("❌ Failure: Should return 401 Unauthorized when credentials are bad")
        void login_BadCredentials() throws Exception {
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Invalid email or password"));

            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validLoginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("BAD_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh Tests")
    class RefreshEndpoints {

        @Test
        @DisplayName("✅ Success: Should return 200 OK and new access token on valid refresh token")
        void refresh_Success() throws Exception {
            RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                    .refreshToken("mock-refresh-token")
                    .build();

            RefreshTokenResponse refreshResponse = RefreshTokenResponse.builder()
                    .accessToken("new-mock-access-token")
                    .build();

            when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(refreshResponse);

            mockMvc.perform(post("/api/auth/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                    .andExpect(jsonPath("$.data.accessToken").value("new-mock-access-token"));
        }

        @Test
        @DisplayName("❌ Failure: Should return 400 Bad Request when refresh token is invalid or expired")
        void refresh_InvalidToken() throws Exception {
            RefreshTokenRequest refreshRequest = RefreshTokenRequest.builder()
                    .refreshToken("invalid-token")
                    .build();

            when(authService.refreshToken(any(RefreshTokenRequest.class)))
                    .thenThrow(new TokenRefreshException("Refresh token was expired"));

            mockMvc.perform(post("/api/auth/refresh")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("REFRESH_TOKEN_FAILED"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout Tests")
    class LogoutEndpoints {

        @Test
        @DisplayName("✅ Success: Should return 200 OK and invalidate token")
        void logout_Success() throws Exception {
            LogoutRequest logoutRequest = LogoutRequest.builder()
                    .refreshToken("mock-refresh-token")
                    .build();

            doNothing().when(authService).logout(any(LogoutRequest.class));

            mockMvc.perform(post("/api/auth/logout")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(logoutRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("User logged out and token invalidated successfully"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/forgot-password Tests")
    class ForgotPasswordEndpoints {

        @Test
        @DisplayName("✅ Success: Should return 200 OK and security question")
        void forgotPassword_Success() throws Exception {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("user@example.com")
                    .build();

            ForgotPasswordResponse response = ForgotPasswordResponse.builder()
                    .success(true)
                    .securityQuestion("What is your first school name?")
                    .build();

            when(forgotPasswordService.retrieveSecurityQuestion(any(ForgotPasswordRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/forgot-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.securityQuestion").value("What is your first school name?"));
        }

        @Test
        @DisplayName("❌ Failure: Should return 404 Not Found when user does not exist")
        void forgotPassword_UserNotFound() throws Exception {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("nonexistent@example.com")
                    .build();

            when(forgotPasswordService.retrieveSecurityQuestion(any(ForgotPasswordRequest.class)))
                    .thenThrow(new UserNotFoundException("nonexistent@example.com"));

            mockMvc.perform(post("/api/auth/forgot-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("User not found with email: nonexistent@example.com"));
        }

        @Test
        @DisplayName("❌ Failure: Should return 400 Bad Request when security question not set")
        void forgotPassword_QuestionNotSet() throws Exception {
            ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                    .email("user@example.com")
                    .build();

            when(forgotPasswordService.retrieveSecurityQuestion(any(ForgotPasswordRequest.class)))
                    .thenThrow(new SecurityQuestionNotSetException("user@example.com"));

            mockMvc.perform(post("/api/auth/forgot-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("SECURITY_QUESTION_NOT_SET"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/reset-password Tests")
    class ResetPasswordEndpoints {

        @Test
        @DisplayName("✅ Success: Should reset password and return 200 OK")
        void resetPassword_Success() throws Exception {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .email("user@example.com")
                    .securityAnswer("ABC School")
                    .newPassword("Password@123")
                    .confirmPassword("Password@123")
                    .build();

            doNothing().when(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));

            mockMvc.perform(post("/api/auth/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Password reset successfully."));
        }

        @Test
        @DisplayName("❌ Failure: Should return 400 Bad Request on invalid security answer")
        void resetPassword_InvalidAnswer() throws Exception {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .email("user@example.com")
                    .securityAnswer("Wrong Answer")
                    .newPassword("Password@123")
                    .confirmPassword("Password@123")
                    .build();

            doThrow(new InvalidSecurityAnswerException()).when(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));

            mockMvc.perform(post("/api/auth/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_SECURITY_ANSWER"));
        }

        @Test
        @DisplayName("❌ Failure: Should return 400 Bad Request on password mismatch")
        void resetPassword_PasswordMismatch() throws Exception {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .email("user@example.com")
                    .securityAnswer("ABC School")
                    .newPassword("Password@123")
                    .confirmPassword("Password@321")
                    .build();

            doThrow(new PasswordMismatchException()).when(resetPasswordService).resetPassword(any(ResetPasswordRequest.class));

            mockMvc.perform(post("/api/auth/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("PASSWORD_MISMATCH"));
        }

        @Test
        @DisplayName("❌ Failure: Should return 400 Bad Request on weak password validation")
        void resetPassword_ValidationError() throws Exception {
            ResetPasswordRequest request = ResetPasswordRequest.builder()
                    .email("user@example.com")
                    .securityAnswer("ABC School")
                    .newPassword("weak")
                    .confirmPassword("weak")
                    .build();

            mockMvc.perform(post("/api/auth/reset-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors").isArray());
        }
    }
}
