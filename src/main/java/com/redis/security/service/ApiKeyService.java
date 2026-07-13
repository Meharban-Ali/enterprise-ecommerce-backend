package com.redis.security.service;

import com.redis.security.dto.response.ApiKeyResponse;
import com.redis.security.dto.request.ApiKeyRequest;
import com.redis.security.dto.response.ApiKeyRotationResponse;

import com.redis.security.entity.ApiKey;
import java.util.List;
import java.util.Optional;

public interface ApiKeyService {
    ApiKeyResponse createKey(ApiKeyRequest request);
    List<ApiKeyResponse> getAllKeys();
    ApiKeyRotationResponse rotateKey(Long id);
    void revokeKey(Long id);
    void deleteKey(Long id);
    void recordUsage(Long id, long latencyMs, boolean success, String ip, boolean rateLimitViolation);
    Optional<ApiKey> getApiKeyByHash(String hash);
}
