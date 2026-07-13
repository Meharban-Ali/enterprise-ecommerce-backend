package com.redis.security.entity;

import com.redis.security.service.RateLimitService;
import com.redis.security.service.RateLimitServiceImpl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RateLimitTest {

    @Test
    void testSlidingWindowRateLimiting() throws InterruptedException {
        RateLimitService service = new RateLimitServiceImpl();
        String key = "test-rate-limit-key";

        // Limit of 2 requests per 2 seconds
        assertTrue(service.isAllowed(key, 2, 2));
        assertTrue(service.isAllowed(key, 2, 2));
        
        // 3rd request should be blocked
        assertFalse(service.isAllowed(key, 2, 2));

        // Wait for sliding window to expire
        Thread.sleep(2050);

        // Should be allowed again
        assertTrue(service.isAllowed(key, 2, 2));
    }
}
