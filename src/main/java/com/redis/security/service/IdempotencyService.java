package com.redis.security.service;

import java.util.Map;

public interface IdempotencyService {

    class IdempotentResponse {
        private final int status;
        private final String body;
        private final Map<String, String> headers;
        
        public IdempotentResponse(int status, String body, Map<String, String> headers) {
            this.status = status;
            this.body = body;
            this.headers = headers;
        }

        public int getStatus() { return status; }
        public String getBody() { return body; }
        public Map<String, String> getHeaders() { return headers; }
    }

    /**
     * Retrieves stored response for a completed idempotent request.
     * Returns null if key is not found or in-progress.
     */
    IdempotentResponse getCachedResponse(String key, String fingerprint);

    /**
     * Tries to acquire a lock (Redis or DB) to execute business logic.
     * Returns true if lock is acquired successfully (IN_PROGRESS saved).
     * Returns false if lock is held (throws Conflict / duplicate check).
     */
    boolean acquireLock(String key, String fingerprint, int ttlHours);

    /**
     * Updates an in-progress idempotency key to COMPLETED and caches the response.
     */
    void completeProcessing(String key, String fingerprint, int status, String body, Map<String, String> headers);

    /**
     * Cleans up expired idempotency keys from storage.
     */
    void cleanExpired();
}
