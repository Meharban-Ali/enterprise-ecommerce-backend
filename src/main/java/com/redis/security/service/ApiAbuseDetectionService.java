package com.redis.security.service;

public interface ApiAbuseDetectionService {
    /**
     * Records a security violation (e.g., rate limit, 404 probing, invalid keys) from a client IP.
     * Triggers a critical security alert if abuse threshold is breached.
     */
    void recordViolation(String clientIp, String violationType);
}
