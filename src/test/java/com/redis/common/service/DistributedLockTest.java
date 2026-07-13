package com.redis.common.service;

import com.redis.security.service.IdempotencyService;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.security.repository.IdempotencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class DistributedLockTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testIdempotencyLockingAndComplete() {
        String key = "test-idempotency-lock-key";
        String fingerprint = "my-fingerprint-hash";

        assertTrue(idempotencyService.acquireLock(key, fingerprint, 1));
        assertFalse(idempotencyService.acquireLock(key, fingerprint, 1));

        idempotencyService.completeProcessing(key, fingerprint, 200, "{\"success\":true}", Collections.emptyMap());

        assertFalse(idempotencyService.acquireLock(key, fingerprint, 1));

        IdempotencyService.IdempotentResponse res = idempotencyService.getCachedResponse(key, fingerprint);
        assertNotNull(res);
        assertEquals(200, res.getStatus());
        assertEquals("{\"success\":true}", res.getBody());
    }
}
