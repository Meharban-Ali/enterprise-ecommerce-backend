package com.redis.reliability.service;

import com.redis.infrastructure.config.PlatformReliabilityProperties;

import com.redis.audit.event.AuditEventPublisher;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

@Slf4j
@Service
public class PlatformResilienceService {

    @Autowired(required = false)
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Autowired(required = false)
    private RetryRegistry retryRegistry;

    @Autowired(required = false)
    private BulkheadRegistry bulkheadRegistry;

    @Autowired(required = false)
    private com.redis.infrastructure.config.PlatformReliabilityProperties properties;

    @Autowired(required = false)
    private AuditEventPublisher auditEventPublisher;

    private final AtomicLong fallbackActivations = new AtomicLong(0);
    private final AtomicLong circuitBreakerOpenCount = new AtomicLong(0);

    public <T> T execute(String name, Supplier<T> operation, Supplier<T> fallback) {
        if (properties != null && !properties.isResilienceEnabled()) {
            return operation.get();
        }

        if (circuitBreakerRegistry == null) {
            return operation.get();
        }

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(name);
        Retry retry = retryRegistry != null ? retryRegistry.retry(name) : null;
        Bulkhead bulkhead = bulkheadRegistry != null ? bulkheadRegistry.bulkhead(name) : null;

        cb.getEventPublisher().onStateTransition(event -> {
            String from = event.getStateTransition().getFromState().name();
            String to = event.getStateTransition().getToState().name();
            log.info("CIRCUIT_BREAKER_STATE_CHANGED | Breaker={} | From={} | To={}", name, from, to);
            
            if ("OPEN".equals(to)) {
                circuitBreakerOpenCount.incrementAndGet();
            }

            if (auditEventPublisher != null) {
                try {
                    auditEventPublisher.publish(
                            null, "SYSTEM", AuditActionType.CIRCUIT_BREAKER_STATE_CHANGED, AuditStatus.SUCCESS,
                            ResourceType.SYSTEM, name,
                            "Circuit Breaker state changed from " + from + " to " + to
                    );
                } catch (Exception e) {
                    // Ignore
                }
            }
        });

        Supplier<T> decorated = cb.decorateSupplier(operation);
        if (retry != null) {
            decorated = Retry.decorateSupplier(retry, decorated);
        }
        if (bulkhead != null) {
            decorated = Bulkhead.decorateSupplier(bulkhead, decorated);
        }

        try {
            return decorated.get();
        } catch (Exception e) {
            fallbackActivations.incrementAndGet();
            log.warn("FALLBACK_TRIGGERED | Breaker={} | Error={} | Triggering fallback...", name, e.getMessage());
            if (auditEventPublisher != null) {
                try {
                    auditEventPublisher.publish(
                            null, "SYSTEM", AuditActionType.SYSTEM_EVENT, AuditStatus.FAILED,
                            ResourceType.SYSTEM, name,
                            "Fallback activated due to: " + e.getMessage()
                    );
                } catch (Exception ex) {
                    // Ignore
                }
            }
            return fallback.get();
        }
    }

    public long getFallbackActivations() {
        return fallbackActivations.get();
    }

    public long getCircuitBreakerOpenCount() {
        return circuitBreakerOpenCount.get();
    }
}
