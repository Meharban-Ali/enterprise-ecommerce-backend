package com.redis.security;

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
public class ReplayProtectionTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private IdempotencyRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void testReplayAttackPrevention() {
        String key = "replay-lock-key";
        String f1 = "fingerprint-one";
        String f2 = "fingerprint-two";

        assertTrue(idempotencyService.acquireLock(key, f1, 1));
        idempotencyService.completeProcessing(key, f1, 200, "success", Collections.emptyMap());

        assertNotNull(idempotencyService.getCachedResponse(key, f1));

        assertThrows(IllegalArgumentException.class, () -> {
            idempotencyService.getCachedResponse(key, f2);
        });
    }
}
