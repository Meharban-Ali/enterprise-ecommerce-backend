package com.redis.security.service;

import com.redis.security.dto.response.ApiKeyResponse;
import com.redis.infrastructure.security.RevokedApiKeyBloomFilter;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.security.dto.request.ApiKeyRequest;
import com.redis.security.dto.response.ApiKeyRotationResponse;
import com.redis.audit.entity.AuditStatus;

import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.security.entity.ApiKey;
import com.redis.audit.entity.AuditActionType;
import com.redis.security.repository.ApiKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiSecurityProperties securityProperties;
    private final SecureRandom random = new SecureRandom();

    @Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    @Autowired(required = false)
    private com.redis.infrastructure.security.RevokedApiKeyBloomFilter bloomFilter;

    private void audit(AuditActionType action, String description, Long resourceId) {
        if (auditEventPublisher != null) {
            auditEventPublisher.publish(
                    null,
                    "system@ecommerce.com",
                    action,
                    com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.SYSTEM,
                    resourceId != null ? String.valueOf(resourceId) : "0",
                    description
            );
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = "apiKeys", allEntries = true)
    public ApiKeyResponse createKey(ApiKeyRequest request) {
        if (apiKeyRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("API Key name already exists: " + request.getName());
        }

        String rawKey = generateSecureKey();
        String hash = sha256(rawKey);

        LocalDateTime expires = null;
        if (request.getExpiresInDays() != null) {
            expires = LocalDateTime.now().plusDays(request.getExpiresInDays());
        }

        ApiKey apiKey = ApiKey.builder()
                .name(request.getName())
                .keyHash(hash)
                .permissions(request.getPermissions())
                .enabled(true)
                .revoked(false)
                .expiresAt(expires)
                .build();
        apiKey.setCreatedAt(LocalDateTime.now());

        apiKey = apiKeyRepository.save(apiKey);
        audit(AuditActionType.API_KEY_CREATED, "Created API key: " + apiKey.getName(), apiKey.getId());

        ApiKeyResponse response = mapToResponse(apiKey);
        response.setApiKey(rawKey); // Return raw key only once
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getAllKeys() {
        return apiKeyRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "apiKeys", allEntries = true)
    public ApiKeyRotationResponse rotateKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found with ID: " + id));

        // Copy current active key to rotation backup key
        apiKey.setRotationKeyHash(apiKey.getKeyHash());
        apiKey.setRotationExpiresAt(LocalDateTime.now().plusDays(securityProperties.getApiKeyRotationGracePeriodDays()));

        // Generate new primary key
        String newRawKey = generateSecureKey();
        String newHash = sha256(newRawKey);

        apiKey.setKeyHash(newHash);
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        audit(AuditActionType.API_KEY_ROTATED, "Rotated API key: " + apiKey.getName(), apiKey.getId());

        return ApiKeyRotationResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .newApiKey(newRawKey)
                .gracePeriodExpiresAt(apiKey.getRotationExpiresAt())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "apiKeys", allEntries = true)
    public void revokeKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found with ID: " + id));

        apiKey.setRevoked(true);
        apiKey.setEnabled(false);
        apiKey.setUpdatedAt(LocalDateTime.now());
        apiKeyRepository.save(apiKey);

        if (bloomFilter != null) {
            bloomFilter.add(apiKey.getKeyHash());
            if (apiKey.getRotationKeyHash() != null) {
                bloomFilter.add(apiKey.getRotationKeyHash());
            }
        }

        audit(AuditActionType.API_KEY_REVOKED, "Revoked API key: " + apiKey.getName(), apiKey.getId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "apiKeys", allEntries = true)
    public void deleteKey(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("API Key not found with ID: " + id));
        apiKeyRepository.delete(apiKey);
    }

    @Override
    @Transactional
    @CacheEvict(value = "apiKeys", allEntries = true)
    public void recordUsage(Long id, long latencyMs, boolean success, String ip, boolean rateLimitViolation) {
        apiKeyRepository.findById(id).ifPresent(key -> {
            key.setTotalRequests(key.getTotalRequests() + 1);
            if (!success) {
                key.setFailedRequests(key.getFailedRequests() + 1);
            }
            if (rateLimitViolation) {
                key.setRateLimitViolations(key.getRateLimitViolations() + 1);
            }
            
            // Calculate rolling latency average
            double currentAverage = key.getAverageLatencyMs();
            long count = key.getTotalRequests();
            double newAverage = ((currentAverage * (count - 1)) + latencyMs) / count;
            key.setAverageLatencyMs(newAverage);

            key.setLastUsedTime(LocalDateTime.now());
            key.setLastIpAddress(ip);

            if (securityProperties.isConsumerAnalyticsEnabled()) {
                LocalDateTime now = LocalDateTime.now();
                int currentHour = now.getHour();
                
                key.setRequestsPerHour(key.getRequestsPerHour() + 1);
                key.setRequestsPerDay(key.getRequestsPerDay() + 1);
                
                double errorRate = (key.getFailedRequests() * 100.0) / count;
                key.setErrorRate(errorRate);
                key.setSuccessRate(100.0 - errorRate);
                key.setPeakUsageHour(currentHour);

                org.springframework.web.context.request.ServletRequestAttributes attributes = 
                        (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    jakarta.servlet.http.HttpServletRequest req = attributes.getRequest();
                    if (req != null) {
                        String endpoint = req.getMethod() + " " + req.getRequestURI();
                        java.util.Map<String, Long> endpointMap = parseTopEndpoints(key.getTopEndpointsJson());
                        endpointMap.put(endpoint, endpointMap.getOrDefault(endpoint, 0L) + 1);
                        
                        String serialized = endpointMap.entrySet().stream()
                                .map(e -> e.getKey() + ":" + e.getValue())
                                .collect(Collectors.joining(","));
                        if (serialized.length() > 2000) {
                            serialized = serialized.substring(0, 2000);
                        }
                        key.setTopEndpointsJson(serialized);
                    }
                }
            }

            apiKeyRepository.save(key);
        });
    }

    private String generateSecureKey() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return "ak_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm missing", e);
        }
    }

    private ApiKeyResponse mapToResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .permissions(apiKey.getPermissions())
                .enabled(apiKey.isEnabled())
                .revoked(apiKey.isRevoked())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .updatedAt(apiKey.getUpdatedAt())
                .totalRequests(apiKey.getTotalRequests())
                .failedRequests(apiKey.getFailedRequests())
                .averageLatencyMs(apiKey.getAverageLatencyMs())
                .lastUsedTime(apiKey.getLastUsedTime())
                .lastIpAddress(apiKey.getLastIpAddress())
                .rateLimitViolations(apiKey.getRateLimitViolations())
                .lastSuccessfulAuthentication(apiKey.getLastSuccessfulAuthentication())
                .failedAuthenticationCount(apiKey.getFailedAuthenticationCount())
                .lockUntil(apiKey.getLockUntil())
                .requestsPerHour(apiKey.getRequestsPerHour())
                .requestsPerDay(apiKey.getRequestsPerDay())
                .errorRate(apiKey.getErrorRate())
                .successRate(apiKey.getSuccessRate())
                .peakUsageHour(apiKey.getPeakUsageHour())
                .topEndpoints(parseTopEndpoints(apiKey.getTopEndpointsJson()))
                .build();
    }

    private java.util.Map<String, Long> parseTopEndpoints(String json) {
        java.util.Map<String, Long> map = new java.util.HashMap<>();
        if (json == null || json.isEmpty()) {
            return map;
        }
        try {
            String[] entries = json.split(",");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    map.put(parts[0], Long.parseLong(parts[1]));
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return map;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "apiKeys", key = "#hash")
    public Optional<ApiKey> getApiKeyByHash(String hash) {
        if (apiKeyRepository == null) return Optional.empty();
        return apiKeyRepository.findByKeyHashOrRotationKeyHash(hash);
    }
}
