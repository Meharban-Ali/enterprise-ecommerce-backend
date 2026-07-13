package com.redis.audit.entity;

import com.redis.security.dto.response.ApiKeyResponse;
import com.redis.security.service.ApiKeyService;
import com.redis.security.dto.request.ApiKeyRequest;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.audit.entity.AuditLog;
import com.redis.audit.entity.AuditActionType;
import com.redis.common.entity.Permission;
import com.redis.security.repository.ApiKeyRepository;
import com.redis.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class ApiAuditIntegrationTest {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        apiKeyRepository.deleteAll();
        auditLogRepository.deleteAll();
    }

    @Test
    void testApiKeyOperationsPublishAuditLogs() throws InterruptedException {
        ApiKeyRequest request = ApiKeyRequest.builder()
                .name("audit-test-key")
                .permissions(Set.of(Permission.USER_READ))
                .build();

        ApiKeyResponse response = apiKeyService.createKey(request);
        assertNotNull(response);

        // Wait for async log event persistence
        Thread.sleep(500);

        List<AuditLog> logs = auditLogRepository.findAll();
        assertFalse(logs.isEmpty());
        boolean hasCreatedAudit = logs.stream()
                .anyMatch(l -> l.getActionType() == AuditActionType.API_KEY_CREATED);
        assertTrue(hasCreatedAudit);
    }
}
