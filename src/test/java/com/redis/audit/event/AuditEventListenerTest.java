package com.redis.audit.event;

import com.redis.user.entity.User;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.audit.event.AuditEvent;
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
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class AuditEventListenerTest {

    @Autowired
    private AuditEventListener auditEventListener;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void testEventListenerPersistsLog() {
        AuditEvent event = new AuditEvent(
                this, "event-id-123", "corr-id-123", "req-id-123",
                1L, "user@example.com", AuditActionType.LOGIN, AuditStatus.SUCCESS,
                ResourceType.USER, "1", "User logged in",
                "127.0.0.1", "Mozilla", "/api/auth/login", "POST",
                "USER", "ROLE_USER", "sess-id-123", "JWT",
                "WEB", 200, 45L, 1
        );

        auditEventListener.handleAuditEvent(event);

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog saved = logs.stream()
                .filter(l -> "event-id-123".equals(l.getEventId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected audit log for event-id-123 not found"));
        assertEquals("event-id-123", saved.getEventId());
        assertEquals("corr-id-123", saved.getCorrelationId());
        assertEquals(AuditActionType.LOGIN, saved.getActionType());
        assertEquals(AuditStatus.SUCCESS, saved.getStatus());
        assertEquals(ResourceType.USER, saved.getResourceType());
        assertEquals("user@example.com", saved.getEmail());
        assertNotNull(saved.getCreatedAt());
    }
}
