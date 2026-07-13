package com.redis.auth.service;

import com.redis.user.service.UserSessionService;
import com.redis.auth.service.JwtService;
import com.redis.auth.service.RefreshTokenService;

import com.redis.audit.event.AuditEventPublisher;
import com.redis.auth.exception.TokenRefreshException;
import com.redis.auth.dto.request.LoginRequest;
import com.redis.common.dto.LogoutRequest;
import com.redis.auth.dto.request.RefreshTokenRequest;
import com.redis.auth.dto.response.LoginResponse;
import com.redis.auth.dto.response.RefreshTokenResponse;
import com.redis.auth.entity.RefreshToken;
import com.redis.user.entity.User;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import com.redis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserSessionService userSessionService;
    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;

    @Autowired(required = false)
    private AuditEventPublisher auditEventPublisher;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Attempting login");

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            log.info("User authenticated successfully — ID: {}", user.getId());

            userSessionService.loginSession(user);

            String accessToken = jwtService.generateToken(user);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            if (auditEventPublisher != null) {
                auditEventPublisher.publish(user.getId(), user.getEmail(), AuditActionType.LOGIN, AuditStatus.SUCCESS,
                        ResourceType.USER, String.valueOf(user.getId()), "User logged in successfully");
            }

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken.getToken())
                    .build();
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("Login failed for email: {}", request.getEmail());
            Long userId = userRepository.findByEmail(request.getEmail()).map(User::getId).orElse(null);
            if (auditEventPublisher != null) {
                auditEventPublisher.publish(userId, request.getEmail(), AuditActionType.LOGIN_FAILED, AuditStatus.FAILED,
                        ResourceType.USER, null, "Failed login attempt: " + e.getMessage());
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        log.info("Processing token refresh request");

        try {
            return refreshTokenService.findByToken(requestRefreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        String accessToken = jwtService.generateToken(user);
                        log.info("New access token generated successfully for user ID: {}", user.getId());

                        if (auditEventPublisher != null) {
                            auditEventPublisher.publish(user.getId(), user.getEmail(), AuditActionType.TOKEN_REFRESH, AuditStatus.SUCCESS,
                                    ResourceType.USER, String.valueOf(user.getId()), "Access token refreshed");
                        }

                        return RefreshTokenResponse.builder()
                                .accessToken(accessToken)
                                .build();
                    })
                    .orElseThrow(() -> new TokenRefreshException(
                            "Refresh token is not present in database. Please log in again."));
        } catch (Exception e) {
            if (auditEventPublisher != null) {
                auditEventPublisher.publish(null, null, AuditActionType.TOKEN_REFRESH, AuditStatus.FAILED,
                        ResourceType.USER, null, "Token refresh failed: " + e.getMessage());
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        log.info("Logging out and invalidating session refresh token");
        refreshTokenService.deleteByToken(request.getRefreshToken());

        HttpServletRequest currentRequest = getCurrentRequest();
        String userEmail = null;
        Long userId = null;
        if (currentRequest != null) {
            String authHeader = currentRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String accessToken = authHeader.substring(7);
                try {
                    String email = jwtService.extractEmail(accessToken);
                    userEmail = email;
                    userSessionService.logoutSession(email);

                    User user = userRepository.findByEmail(email).orElse(null);
                    if (user != null) {
                        userId = user.getId();
                    }

                    Date expiration = jwtService.extractExpiration(accessToken);
                    long remainingTimeMs = expiration.getTime() - System.currentTimeMillis();
                    RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
                    if (remainingTimeMs > 0 && redisTemplate != null) {
                        String blacklistKey = "blacklist::" + accessToken;
                        redisTemplate.opsForValue().set(blacklistKey, "true", remainingTimeMs, TimeUnit.MILLISECONDS);
                        log.info("Access token blacklisted in Redis. Remaining TTL: {} ms", remainingTimeMs);
                    }
                } catch (Exception e) {
                    log.warn("Failed to blacklist access token: {}", e.getMessage());
                }
            }
        }

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(userId, userEmail, AuditActionType.LOGOUT, AuditStatus.SUCCESS,
                    ResourceType.USER, userId != null ? String.valueOf(userId) : null, "User logged out successfully");
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}
