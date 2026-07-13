package com.redis.observability.entity;

import com.redis.observability.service.TracingService;
import com.redis.observability.service.PerformanceMetricsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SchedulerMetricsAspect {

    private final TracingService tracingService;
    private final PerformanceMetricsService metricsService;
    
    // Memory store for tracking execution drift, last success, last failure per scheduler
    private final Map<String, SchedulerStats> stats = new ConcurrentHashMap<>();

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public Object measureScheduler(ProceedingJoinPoint joinPoint) throws Throwable {
        String schedulerName = joinPoint.getSignature().getDeclaringType().getSimpleName() + "." + joinPoint.getSignature().getName();
        TraceSpan span = tracingService.startSpan(schedulerName, SpanType.SCHEDULER, "Scheduler");

        stats.putIfAbsent(schedulerName, new SchedulerStats());
        SchedulerStats sStats = stats.get(schedulerName);
        sStats.executionCount++;

        Object result;
        try {
            result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - span.getStartTime().toEpochMilli();
            sStats.lastSuccessfulExecution = Instant.now();
            sStats.totalDurationMs += duration;
            if (duration > sStats.maxDurationMs) sStats.maxDurationMs = duration;
            if (sStats.minDurationMs == 0 || duration < sStats.minDurationMs) sStats.minDurationMs = duration;

            metricsService.recordMetric(PerformanceMetric.builder()
                    .metricName("scheduler." + schedulerName)
                    .durationMs(duration)
                    .timestamp(Instant.now())
                    .type(SpanType.SCHEDULER)
                    .build());

        } catch (Throwable t) {
            sStats.failureCount++;
            sStats.lastFailedExecution = Instant.now();
            tracingService.recordException(span, t);
            throw t;
        } finally {
            tracingService.finishSpan(span);
        }

        return result;
    }
    
    public Map<String, SchedulerStats> getSchedulerStats() {
        return stats;
    }

    public static class SchedulerStats {
        public long executionCount = 0;
        public long failureCount = 0;
        public Instant lastSuccessfulExecution;
        public Instant lastFailedExecution;
        public long maxDurationMs = 0;
        public long minDurationMs = 0;
        public long totalDurationMs = 0;
        
        public double getFailurePercentage() {
            if (executionCount == 0) return 0.0;
            return ((double) failureCount / executionCount) * 100.0;
        }
        
        public long getAverageRuntimeMs() {
            if (executionCount == 0) return 0;
            return totalDurationMs / executionCount;
        }
    }
}
