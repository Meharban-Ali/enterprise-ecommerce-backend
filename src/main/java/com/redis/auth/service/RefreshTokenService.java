package com.redis.auth.service;

import com.redis.auth.exception.TokenRefreshException;
import com.redis.auth.entity.RefreshToken;
import com.redis.user.entity.User;
import com.redis.auth.repository.RefreshTokenRepository;
import com.redis.user.repository.UserRepository;
import com.redis.infrastructure.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        log.info("Generating new refresh token for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString()) // Cryptographically secure random UUID
                .expiryDate(Instant.now().plusMillis(jwtProperties.getRefreshExpirationMs()))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken verifyExpiration(RefreshToken token) {
        // Compare token expiry with current Instant
        if (token.getExpiryDate().isBefore(Instant.now())) {
            Long userId = token.getUser() != null ? token.getUser().getId() : null;
            log.warn("Refresh token expired for user ID: {}", userId);
            refreshTokenRepository.delete(token); // Auto-cleanup expired token
            throw new TokenRefreshException(token.getToken() + ": Refresh token was expired. Please sign in again.");
        }
        return token;
    }

    @Transactional
    public void deleteByToken(String token) {
        log.info("Deleting/revoking refresh token");
        refreshTokenRepository.deleteByToken(token);
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        log.info("Deleting all active refresh tokens for user ID: {}", userId);
        userRepository.findById(userId).ifPresent(refreshTokenRepository::deleteByUser);
    }
}
