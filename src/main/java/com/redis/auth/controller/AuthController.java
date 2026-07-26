package com.redis.auth.controller;

import com.redis.auth.dto.request.ForgotPasswordRequest;
import com.redis.auth.dto.request.LoginRequest;
import com.redis.auth.dto.request.RefreshTokenRequest;
import com.redis.auth.dto.request.RegisterRequest;
import com.redis.auth.dto.request.ResetPasswordRequest;
import com.redis.auth.dto.response.LoginResponse;
import com.redis.auth.dto.response.RefreshTokenResponse;
import com.redis.auth.dto.response.RegisterResponse;
import com.redis.auth.service.AuthService;
import com.redis.auth.service.ForgotPasswordService;
import com.redis.common.dto.ApiResponse;
import com.redis.common.dto.LogoutRequest;
import com.redis.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerUser(
            @Valid @RequestBody RegisterRequest request) {
        log.info("API POST /api/auth/register — registration request for email: {}", request.getEmail());
        RegisterResponse response = userService.registerUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED) // 201 Created
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginUser(
            @Valid @RequestBody LoginRequest request) {
        log.info("API POST /api/auth/login — login request for email: {}", request.getEmail());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("User logged in successfully", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshUserToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("API POST /api/auth/refresh — token refresh request received");
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logoutUser(
            @Valid @RequestBody LogoutRequest request) {
        log.info("API POST /api/auth/logout — logout request received");
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success("User logged out and token invalidated successfully"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.info("API POST /api/auth/forgot-password — forgot password request for email: {}", request.getEmail());
        ApiResponse<Void> response = forgotPasswordService.requestPasswordReset(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("API POST /api/auth/reset-password — link-based reset password request");
        ApiResponse<Void> response = forgotPasswordService.resetPassword(request);
        return ResponseEntity.ok(response);
    }
}
