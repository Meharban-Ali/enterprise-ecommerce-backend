package com.redis.security.entity;

import com.redis.security.dto.request.ApiKeyRequest;
import com.redis.security.dto.response.ApiKeyResponse;
import com.redis.security.dto.response.ApiKeyRotationResponse;
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
public class ApiKeyRotationTest {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
    }

    @Test
    void testApiKeyCreationAndRotationLifecycle() {
        ApiKeyRequest request = ApiKeyRequest.builder()
                .name("test-key")
                .permissions(Set.of(Permission.ORDER_READ))
                .build();

        ApiKeyResponse response = apiKeyService.createKey(request);
        assertNotNull(response.getApiKey());
        
        ApiKeyRotationResponse rot = apiKeyService.rotateKey(response.getId());
        assertNotNull(rot.getNewApiKey());
        assertNotEquals(response.getApiKey(), rot.getNewApiKey());
        assertNotNull(rot.getGracePeriodExpiresAt());

        ApiKey updated = apiKeyRepository.findById(response.getId()).orElseThrow();
        assertNotNull(updated.getRotationKeyHash());
        assertEquals(apiKeyRepository.findByKeyHashOrRotationKeyHash(updated.getRotationKeyHash()).get().getId(), updated.getId());
    }
}
