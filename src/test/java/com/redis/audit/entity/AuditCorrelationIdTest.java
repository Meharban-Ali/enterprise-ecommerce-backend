package com.redis.audit.entity;

import com.redis.audit.event.AuditEvent;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuditCorrelationIdTest {

    @Mock
    private ApplicationEventPublisher mockPublisher;

    private AuditEventPublisher auditEventPublisher;

    @BeforeEach
    void setUp() {
        auditEventPublisher = new AuditEventPublisher(mockPublisher);
    }

    @Test
    void testCorrelationIdHeaderPriority() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "header-corr-id");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        MDC.put("CorrelationId", "mdc-corr-id");

        try {
            auditEventPublisher.publish(
                    1L, "user@example.com",
                    AuditActionType.LOGIN,
                    AuditStatus.SUCCESS,
                    ResourceType.USER,
                    "1", "Test desc"
            );

            verify(mockPublisher).publishEvent(argThat(event -> {
                AuditEvent auditEvent = (AuditEvent) event;
                return "header-corr-id".equals(auditEvent.getCorrelationId());
            }));
        } finally {
            MDC.clear();
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void testCorrelationIdMdcPriorityWhenHeaderAbsent() {
        MDC.put("CorrelationId", "mdc-corr-id");

        try {
            auditEventPublisher.publish(
                    1L, "user@example.com",
                    AuditActionType.LOGIN,
                    AuditStatus.SUCCESS,
                    ResourceType.USER,
                    "1", "Test desc"
            );

            verify(mockPublisher).publishEvent(argThat(event -> {
                AuditEvent auditEvent = (AuditEvent) event;
                return "mdc-corr-id".equals(auditEvent.getCorrelationId());
            }));
        } finally {
            MDC.clear();
        }
    }
}
