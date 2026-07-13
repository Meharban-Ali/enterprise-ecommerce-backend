package com.redis.audit.event;

import com.redis.user.entity.User;

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
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuditEventPublisherTest {

    @Mock
    private ApplicationEventPublisher mockPublisher;

    private AuditEventPublisher auditEventPublisher;

    @BeforeEach
    void setUp() {
        auditEventPublisher = new AuditEventPublisher(mockPublisher);
    }

    @Test
    void testPublisherPublishesEvent() {
        auditEventPublisher.publish(
                1L, "actor@example.com", AuditActionType.LOGIN, AuditStatus.SUCCESS,
                ResourceType.USER, "1", "User logged in successfully"
        );

        verify(mockPublisher).publishEvent(any(AuditEvent.class));
    }
}
