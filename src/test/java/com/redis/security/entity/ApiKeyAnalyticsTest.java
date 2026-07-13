package com.redis.security.entity;

import com.redis.security.dto.request.ApiKeyRequest;
import com.redis.security.dto.response.ApiKeyResponse;
import com.redis.security.service.ApiKeyService;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.security.entity.ApiKey;
import com.redis.common.entity.Permission;
import com.redis.security.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class ApiKeyAnalyticsTest {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
    }

    @Test
    void testUsageMetricsCollection() {
        ApiKeyRequest request = ApiKeyRequest.builder()
                .name("metrics-key")
                .permissions(Set.of(Permission.PRODUCT_READ))
                .build();

        ApiKeyResponse response = apiKeyService.createKey(request);
        Long id = response.getId();

        apiKeyService.recordUsage(id, 100L, true, "192.168.1.100", false);
        apiKeyService.recordUsage(id, 200L, false, "192.168.1.100", true);

        ApiKey key = apiKeyRepository.findById(id).orElseThrow();
        assertEquals(2L, key.getTotalRequests());
        assertEquals(1L, key.getFailedRequests());
        assertEquals(1L, key.getRateLimitViolations());
        assertEquals(150.0, key.getAverageLatencyMs());
        assertEquals("192.168.1.100", key.getLastIpAddress());
        assertNotNull(key.getLastUsedTime());
    }
}
