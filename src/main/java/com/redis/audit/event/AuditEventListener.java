package com.redis.audit.event;

import com.redis.monitoring.service.SystemMonitoringService;

import com.redis.audit.event.AuditEvent;
import com.redis.audit.entity.AuditLog;
import com.redis.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    private final java.util.concurrent.atomic.AtomicLong processedCount = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong totalDurationMs = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong failuresCount = new java.util.concurrent.atomic.AtomicLong(0);

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.beans.factory.ObjectProvider<SystemMonitoringService> monitoringServiceProvider;

    public long getProcessedCount() {
        return processedCount.get();
    }

    public double getAverageExecutionTimeMs() {
        long count = processedCount.get();
        return count > 0 ? (double) totalDurationMs.get() / count : 0.0;
    }

    public long getFailuresCount() {
        return failuresCount.get();
    }

    @Async("auditAsyncExecutor")
    @EventListener
    public void handleAuditEvent(AuditEvent event) {
        long startTime = System.currentTimeMillis();
        processedCount.incrementAndGet();
        log.info("Received audit event asynchronously on thread: {}", Thread.currentThread().getName());

        try {
            AuditLog auditLog = AuditLog.builder()
                    .eventId(event.getEventId())
                    .correlationId(event.getCorrelationId())
                    .requestId(event.getRequestId())
                    .userId(event.getUserId())
                    .email(event.getEmail())
                    .actionType(event.getActionType())
                    .status(event.getStatus())
                    .resourceType(event.getResourceType())
                    .resourceId(event.getResourceId())
                    .description(event.getDescription())
                    .ipAddress(event.getIpAddress())
                    .userAgent(event.getUserAgent())
                    .requestUri(event.getRequestUri())
                    .httpMethod(event.getHttpMethod())
                    .actorType(event.getActorType())
                    .roleName(event.getRoleName())
                    .sessionId(event.getSessionId())
                    .authenticationMethod(event.getAuthenticationMethod())
                    .clientType(event.getClientType())
                    .httpStatus(event.getHttpStatus())
                    .executionTimeMs(event.getExecutionTimeMs())
                    .entityVersion(event.getEntityVersion())
                    .build();

            auditLogRepository.save(auditLog);

            // Structured logging
            log.info("AUDIT_EVENT | EventId={} | CorrelationId={} | RequestId={} | Actor={} | Action={} | Resource={}:{} | ExecutionTime={}ms | Thread={} | Status={}",
                    event.getEventId(),
                    event.getCorrelationId(),
                    event.getRequestId(),
                    event.getEmail() != null ? event.getEmail() : "SYSTEM",
                    event.getActionType(),
                    event.getResourceType(),
                    event.getResourceId() != null ? event.getResourceId() : "NULL",
                    event.getExecutionTimeMs() != null ? event.getExecutionTimeMs() : 0,
                    Thread.currentThread().getName(),
                    event.getStatus()
            );

        } catch (Exception e) {
            failuresCount.incrementAndGet();
            // Fail-safe requirement: exceptions must never disrupt business operations
            log.error("Fatal error persisting audit log for event {}: {}", event.getEventId(), e.getMessage(), e);
            SystemMonitoringService service = monitoringServiceProvider.getIfAvailable();
            if (service != null) {
                service.registerError("AuditEventListener", e);
            }
        } finally {
            totalDurationMs.addAndGet(System.currentTimeMillis() - startTime);
        }
    }
}
