package com.redis.user.service;

import com.redis.user.entity.User;
import com.redis.user.entity.UserSession;
import com.redis.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider;

    private static final String REDIS_PREFIX = "user::active-session::";
    private static final long DEBOUNCE_MINUTES = 5;
    private static final long CACHE_TTL_MINUTES = 15;

    @Override
    @Transactional
    public void loginSession(User user) {
        log.info("Recording login session for user ID: {}", user.getId());
        LocalDateTime now = LocalDateTime.now();

        UserSession session = userSessionRepository.findByUserId(user.getId())
                .orElseGet(() -> UserSession.builder()
                        .userId(user.getId())
                        .username(user.getActualUsername())
                        .email(user.getEmail())
                        .build());

        session.setLoginTime(now);
        session.setLogoutTime(null);
        session.setLastActivity(now);
        session.setStatus("ONLINE");

        userSessionRepository.save(session);

        // Cache in Redis
        cacheSessionInRedis(user.getId(), now);
    }

    @Override
    @Transactional
    public void logoutSession(String email) {
        log.info("Recording logout session for user email: {}", email);
        LocalDateTime now = LocalDateTime.now();

        Optional<UserSession> sessionOpt = userSessionRepository.findByEmail(email);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.setLogoutTime(now);
            session.setLastActivity(now);
            session.setStatus("OFFLINE");
            userSessionRepository.save(session);
            
            // Remove from Redis
            evictSessionFromRedis(session.getUserId());
        }
    }

    @Override
    @Transactional
    public void updateSessionActivity(User user) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = user.getId();

        // Try getting Redis cache to debounce database updates
        RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            String cacheKey = REDIS_PREFIX + userId;
            try {
                Object cachedVal = redisTemplate.opsForValue().get(cacheKey);

                if (cachedVal != null) {
                    LocalDateTime lastSync = LocalDateTime.parse(cachedVal.toString());
                    // Debounce: If updated within the last 5 minutes, skip DB hit but refresh cache TTL
                    if (lastSync.plusMinutes(DEBOUNCE_MINUTES).isAfter(now)) {
                        redisTemplate.expire(cacheKey, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                        return;
                    }
                }
            } catch (Exception e) {
                log.warn("Redis connection or operation failed during session update for user ID: {}. Falling back to database: {}", userId, e.getMessage());
            }
        }

        // DB update
        log.debug("Updating database last activity for user ID: {}", userId);
        UserSession session = userSessionRepository.findByUserId(userId)
                .orElseGet(() -> UserSession.builder()
                        .userId(userId)
                        .username(user.getActualUsername())
                        .email(user.getEmail())
                        .build());

        session.setLastActivity(now);
        session.setStatus("ONLINE");
        userSessionRepository.save(session);

        // Update Redis cache with current timestamp
        cacheSessionInRedis(userId, now);
    }

    private void cacheSessionInRedis(Long userId, LocalDateTime time) {
        RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            String cacheKey = REDIS_PREFIX + userId;
            try {
                redisTemplate.opsForValue().set(
                        cacheKey,
                        time.toString(),
                        CACHE_TTL_MINUTES,
                        TimeUnit.MINUTES
                );
            } catch (Exception e) {
                log.warn("Failed to write session activity to Redis cache: {}", e.getMessage());
            }
        }
    }

    private void evictSessionFromRedis(Long userId) {
        RedisTemplate<String, Object> redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            String cacheKey = REDIS_PREFIX + userId;
            try {
                redisTemplate.delete(cacheKey);
            } catch (Exception e) {
                log.warn("Failed to delete session activity from Redis cache: {}", e.getMessage());
            }
        }
    }
}
