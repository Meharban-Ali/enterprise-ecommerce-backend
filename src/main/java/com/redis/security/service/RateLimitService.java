package com.redis.security.service;

public interface RateLimitService {

    /**
     * Checks if a request is permitted under rate limit thresholds.
     * Returns true if allowed, false if limit is exceeded.
     */
    boolean isAllowed(String key, int limit, int windowSeconds);

    /**
     * Resolves key block retry cooldown in seconds.
     */
    int getRetryAfterSeconds(String key, int limit, int windowSeconds);
}
