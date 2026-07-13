package com.redis.infrastructure.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

/**
 * Resilient cache error handler that intercepts connection or operations errors
 * with Redis, logging them at WARN level rather than crashing the client API call.
 */
public class CustomCacheErrorHandler implements CacheErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomCacheErrorHandler.class);

    private String sanitizeKey(Object key) {
        if (key == null) return "null";
        String str = key.toString();
        if (str.contains("@")) {
            int atIdx = str.indexOf("@");
            if (atIdx > 1) {
                return str.substring(0, 1) + "****" + str.substring(atIdx);
            }
        }
        return str;
    }

    @Override
    public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache GET failed for cache '{}' and key '{}': {}", cache.getName(), sanitizeKey(key), exception.getMessage());
    }

    @Override
    public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("Redis cache PUT failed for cache '{}' and key '{}': {}", cache.getName(), sanitizeKey(key), exception.getMessage());
    }

    @Override
    public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("Redis cache EVICT failed for cache '{}' and key '{}': {}", cache.getName(), sanitizeKey(key), exception.getMessage());
    }

    @Override
    public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("Redis cache CLEAR failed for cache '{}': {}", cache.getName(), exception.getMessage());
    }
}
