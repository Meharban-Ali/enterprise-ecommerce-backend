package com.redis.auth.service;

import com.redis.auth.dto.request.LoginRequest;
import com.redis.common.dto.LogoutRequest;
import com.redis.auth.dto.request.RefreshTokenRequest;
import com.redis.auth.dto.response.LoginResponse;
import com.redis.auth.dto.response.RefreshTokenResponse;

public interface AuthService {

    // Authenticates user credentials, registers user session, and issues access/refresh tokens
    LoginResponse login(LoginRequest request);

    // Validates a refresh token and issues a new short-lived access token
    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    // Revokes and deletes the active refresh token session
    void logout(LogoutRequest request);
}
