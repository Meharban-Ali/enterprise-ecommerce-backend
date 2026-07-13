package com.redis.security.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.infrastructure.config.ApiSecurityProperties;
import com.redis.security.entity.IdempotencyKey;
import com.redis.security.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final IdempotencyRepository repository;
    private final ApiSecurityProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(readOnly = true)
    public IdempotentResponse getCachedResponse(String key, String fingerprint) {
        Optional<IdempotencyKey> opt = repository.findByKey(key);
        if (opt.isPresent()) {
            IdempotencyKey ik = opt.get();
            if (ik.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.info("Idempotency key expired: {}", key);
                return null;
            }
            if (properties.isReplayProtectionEnabled() && ik.getRequestFingerprint() != null) {
                if (!ik.getRequestFingerprint().equals(fingerprint)) {
                    log.warn("Idempotency key fingerprint mismatch! Possible replay or duplicate attack: {}", key);
                    throw new IllegalArgumentException("Replay attack detected — fingerprint mismatch");
                }
            }
            if ("COMPLETED".equals(ik.getStatus())) {
                Map<String, String> headers = Collections.emptyMap();
                try {
                    if (ik.getResponseHeaders() != null) {
                        headers = objectMapper.readValue(ik.getResponseHeaders(), new TypeReference<Map<String, String>>() {});
                    }
                } catch (Exception e) {
                    log.error("Failed to parse cached headers", e);
                }
                return new IdempotentResponse(ik.getResponseStatus(), ik.getResponseBody(), headers);
            }
        }
        return null;
    }

    @Override
    @Transactional
    public boolean acquireLock(String key, String fingerprint, int ttlHours) {
        if (properties.isDistributedLockEnabled() && redisTemplate != null) {
            String lockKey = "lock:idempotency:" + key;
            try {
                Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(10));
                if (Boolean.FALSE.equals(success)) {
                    log.warn("Redis distributed lock acquisition failed for key: {}", key);
                    return false;
                }
            } catch (Exception e) {
                log.error("Redis distributed lock failed, falling back to DB lock: {}", e.getMessage());
            }
        }

        Optional<IdempotencyKey> opt = repository.findByKey(key);
        if (opt.isPresent()) {
            IdempotencyKey ik = opt.get();
            if (ik.getExpiresAt().isBefore(LocalDateTime.now())) {
                repository.delete(ik);
            } else {
                log.warn("Idempotency request already exists for key: {} (status: {})", key, ik.getStatus());
                return false;
            }
        }

        try {
            IdempotencyKey newKey = IdempotencyKey.builder()
                    .key(key)
                    .status("IN_PROGRESS")
                    .requestFingerprint(fingerprint)
                    .expiresAt(LocalDateTime.now().plusHours(ttlHours))
                    .build();
            repository.saveAndFlush(newKey);
            return true;
        } catch (Exception e) {
            log.warn("Failed to insert idempotency record (concurrent request conflict): {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void completeProcessing(String key, String fingerprint, int status, String body, Map<String, String> headers) {
        Optional<IdempotencyKey> opt = repository.findByKey(key);
        if (opt.isPresent()) {
            IdempotencyKey ik = opt.get();
            ik.setStatus("COMPLETED");
            ik.setResponseStatus(status);
            ik.setResponseBody(body);
            try {
                ik.setResponseHeaders(objectMapper.writeValueAsString(headers));
            } catch (Exception e) {
                log.error("Failed to serialize response headers", e);
            }
            repository.save(ik);
        }

        if (properties.isDistributedLockEnabled() && redisTemplate != null) {
            String lockKey = "lock:idempotency:" + key;
            try {
                redisTemplate.delete(lockKey);
            } catch (Exception e) {
                log.error("Failed to release Redis lock: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void cleanExpired() {
        int deleted = repository.deleteExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Purged {} expired idempotency keys", deleted);
        }
    }
}
