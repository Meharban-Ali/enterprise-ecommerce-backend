package com.redis.infrastructure.governance.service;

import com.redis.infrastructure.governance.dto.ApiGovernanceDashboardResponse;

public interface ApiGovernanceService {

    /**
     * Records transactional metrics for an incoming request.
     */
    void recordRequest(String uri, String method, long latencyMs, int statusCode, long payloadSize, long responseSize,
                       String consumerKey, boolean isIdempotencyHit, boolean isReplayAttack,
                       boolean isRateLimitExceeded, boolean isValidationError);

    default void recordRequest(String uri, String method, long latencyMs, int statusCode, long payloadSize,
                               String consumerKey, boolean isIdempotencyHit, boolean isReplayAttack,
                               boolean isRateLimitExceeded, boolean isValidationError) {
        recordRequest(uri, method, latencyMs, statusCode, payloadSize, 0L, consumerKey, isIdempotencyHit, isReplayAttack, isRateLimitExceeded, isValidationError);
    }

    /**
     * Aggregates and returns the active API governance metrics dashboard.
     */
    ApiGovernanceDashboardResponse getDashboard();
}
