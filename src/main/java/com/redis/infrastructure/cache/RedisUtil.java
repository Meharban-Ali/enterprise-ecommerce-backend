package com.redis.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for low-level Redis operations.
 * Used for direct key-value access (e.g., token blacklisting, custom TTL management)
 * that falls outside of Spring's @Cacheable abstraction.
 * Completely hardened with try-catch blocks to fall back gracefully if Redis is down.
 */
@Slf4j
@Component
@ConditionalOnBean(RedisTemplate.class)
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Fetches a value directly from Redis by key.
     *
     * @param key the Redis key
     * @return the stored value, or null if not found or on connection error
     */
    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Redis GET — key: {}, found: {}", key, value != null);
            return value;
        } catch (Exception e) {
            log.warn("Redis GET operation failed for key '{}': {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Stores a value in Redis with a specified TTL in minutes.
     *
     * @param key        the Redis key
     * @param value      the value to store
     * @param ttlMinutes time-to-live in minutes
     */
    public void set(String key, Object value, long ttlMinutes) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlMinutes, TimeUnit.MINUTES);
            log.debug("Redis SET — key: {}, TTL: {} min", key, ttlMinutes);
        } catch (Exception e) {
            log.warn("Redis SET operation failed for key '{}': {}", key, e.getMessage());
        }
    }

    /**
     * Checks whether a key exists in Redis.
     *
     * @param key the Redis key
     * @return true if the key exists, false otherwise
     */
    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.warn("Redis EXISTS check failed for key '{}': {}", key, e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a key from Redis.
     *
     * @param key the Redis key to delete
     * @return true if the key was deleted, false if it did not exist or on error
     */
    public boolean delete(String key) {
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.debug("Redis DELETE — key: {}, deleted: {}", key, deleted);
            return Boolean.TRUE.equals(deleted);
        } catch (Exception e) {
            log.warn("Redis DELETE operation failed for key '{}': {}", key, e.getMessage());
            return false;
        }
    }
}
