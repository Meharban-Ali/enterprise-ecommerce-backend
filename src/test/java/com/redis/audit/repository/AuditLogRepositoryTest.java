package com.redis.audit.repository;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.audit.entity.AuditLog;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import com.redis.audit.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class AuditLogRepositoryTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void testRepositoryFilteringAndPageable() {
        AuditLog log1 = AuditLog.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId("corr-1")
                .userId(100L)
                .email("u1@example.com")
                .actionType(AuditActionType.LOGIN)
                .status(AuditStatus.SUCCESS)
                .resourceType(ResourceType.USER)
                .build();

        AuditLog log2 = AuditLog.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId("corr-2")
                .userId(200L)
                .email("u2@example.com")
                .actionType(AuditActionType.PAYMENT_SUCCESS)
                .status(AuditStatus.SUCCESS)
                .resourceType(ResourceType.PAYMENT)
                .build();

        auditLogRepository.save(log1);
        auditLogRepository.save(log2);

        Page<AuditLog> userLogs = auditLogRepository.findByUserId(100L, PageRequest.of(0, 10));
        assertEquals(1, userLogs.getTotalElements());
        assertEquals("u1@example.com", userLogs.getContent().get(0).getEmail());

        Page<AuditLog> actionLogs = auditLogRepository.findByActionType(AuditActionType.PAYMENT_SUCCESS, PageRequest.of(0, 10));
        assertEquals(1, actionLogs.getTotalElements());

        long count = auditLogRepository.countSecurityEvents();
        assertEquals(1, count); // LOGIN is security event
    }
}
