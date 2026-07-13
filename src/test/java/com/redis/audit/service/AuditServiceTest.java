package com.redis.audit.service;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.audit.dto.response.AuditLogResponse;
import com.redis.audit.dto.request.AuditSearchRequest;
import com.redis.audit.dto.response.AuditSummaryResponse;
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

import java.io.StringWriter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class AuditServiceTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void testSearchAndPageLimitRestriction() {
        for (int i = 0; i < 110; i++) {
            auditLogRepository.save(AuditLog.builder()
                    .eventId(UUID.randomUUID().toString())
                    .correlationId("corr-" + i)
                    .actionType(AuditActionType.LOGIN)
                    .status(AuditStatus.SUCCESS)
                    .resourceType(ResourceType.USER)
                    .build());
        }

        // Test max page size constraint (100)
        Page<AuditLogResponse> response = auditService.searchLogs(new AuditSearchRequest(), PageRequest.of(0, 500));
        assertEquals(100, response.getSize()); // Checked: Restricted to 100 max
    }

    @Test
    void testAuditSummary() {
        auditLogRepository.save(AuditLog.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId("corr-summary")
                .actionType(AuditActionType.LOGIN)
                .status(AuditStatus.SUCCESS)
                .resourceType(ResourceType.USER)
                .build());

        AuditSummaryResponse summary = auditService.getAuditSummary();
        assertEquals(1, summary.getTotalEvents());
        assertEquals(1, summary.getSecurityEvents());
        assertEquals(0, summary.getPaymentEvents());
    }

    @Test
    void testExportCsv() {
        auditLogRepository.save(AuditLog.builder()
                .eventId(UUID.randomUUID().toString())
                .correlationId("corr-csv")
                .actionType(AuditActionType.LOGIN)
                .status(AuditStatus.SUCCESS)
                .resourceType(ResourceType.USER)
                .description("CSV Test log entry")
                .build());

        StringWriter writer = new StringWriter();
        auditService.exportLogsToCsv(new AuditSearchRequest(), writer);
        String csvContent = writer.toString();

        assertTrue(csvContent.contains("CSV Test log entry"));
        assertTrue(csvContent.contains("corr-csv"));
    }
}
