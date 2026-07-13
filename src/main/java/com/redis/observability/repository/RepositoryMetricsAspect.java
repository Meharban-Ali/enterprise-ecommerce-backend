package com.redis.observability.repository;

import com.redis.observability.entity.TraceSpan;
import com.redis.observability.entity.PerformanceMetric;
import com.redis.observability.entity.SpanType;
import com.redis.observability.service.TracingService;
import com.redis.observability.service.PerformanceMetricsService;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Aspect
@Component
@RequiredArgsConstructor
public class RepositoryMetricsAspect {

    private final TracingService tracingService;
    private final PerformanceMetricsService metricsService;

    @Around("execution(* com.redis.repository..*(..))")
    public Object measureRepositoryMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String operationName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();
        TraceSpan span = tracingService.startSpan(operationName, SpanType.DATABASE, "Repository");

        Object result;
        try {
            result = joinPoint.proceed();
            
            // Log collection metrics
            if (result instanceof java.util.Collection) {
                span.getAttributes().put("db.rows_returned", ((java.util.Collection<?>) result).size());
            }

            metricsService.recordMetric(PerformanceMetric.builder()
                    .metricName(operationName)
                    .durationMs(System.currentTimeMillis() - span.getStartTime().toEpochMilli())
                    .timestamp(Instant.now())
                    .type(SpanType.DATABASE)
                    .build());

        } catch (Throwable t) {
            tracingService.recordException(span, t);
            throw t;
        } finally {
            tracingService.finishSpan(span);
        }

        return result;
    }
}
