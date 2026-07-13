package com.redis.observability.service;

import com.redis.observability.entity.TraceSpan;
import com.redis.observability.entity.SpanType;
import com.redis.observability.entity.TraceContextHolder;
import com.redis.observability.entity.TraceContext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.Instant;
import java.util.Stack;
import java.util.UUID;

@Slf4j
@Service
public class TracingService {

    private static final ThreadLocal<Stack<TraceSpan>> SPAN_STACK = ThreadLocal.withInitial(Stack::new);

    @Autowired(required = false)
    private SlowOperationDetector slowOperationDetector;
    
    @Autowired(required = false)
    private ApplicationEventPublisher eventPublisher;

    public TraceSpan startSpan(String operationName, SpanType type, String module) {
        TraceContext context = TraceContextHolder.getContext();
        if (context == null) {
            // Create a minimal context if it's missing (e.g., background thread without filter)
            context = TraceContext.builder()
                    .traceId(UUID.randomUUID().toString())
                    .spanId(UUID.randomUUID().toString())
                    .correlationId(UUID.randomUUID().toString())
                    .module(module)
                    .build();
            TraceContextHolder.setContext(context);
        }

        String parentSpanId = context.getSpanId();
        
        Stack<TraceSpan> stack = SPAN_STACK.get();
        if (!stack.isEmpty()) {
            parentSpanId = stack.peek().getSpanId();
        }

        String newSpanId = UUID.randomUUID().toString();
        
        // Temporarily update context to reflect current span
        context.setParentSpanId(parentSpanId);
        context.setSpanId(newSpanId);
        TraceContextHolder.setContext(context);

        TraceSpan span = TraceSpan.builder()
                .traceId(context.getTraceId())
                .spanId(newSpanId)
                .parentSpanId(parentSpanId)
                .operationName(operationName)
                .type(type)
                .module(module)
                .startTime(Instant.now())
                .status("STARTED")
                .build();

        stack.push(span);
        publishEvent("TRACE_STARTED", span);
        
        return span;
    }

    public void finishSpan(TraceSpan span) {
        if (span.isFinished()) return;
        
        span.setEndTime(Instant.now());
        span.setTotalTimeMs(Duration.between(span.getStartTime(), span.getEndTime()).toMillis());
        span.setSelfTimeMs(span.getTotalTimeMs() - span.getChildDurationMs());
        span.setFinished(true);
        if (span.getStatus().equals("STARTED")) {
            span.setStatus("COMPLETED");
        }

        Stack<TraceSpan> stack = SPAN_STACK.get();
        if (!stack.isEmpty() && stack.peek().getSpanId().equals(span.getSpanId())) {
            stack.pop();
        }

        // Add duration to parent
        if (!stack.isEmpty()) {
            TraceSpan parent = stack.peek();
            parent.setChildDurationMs(parent.getChildDurationMs() + span.getTotalTimeMs());
            // Restore context to parent
            TraceContext ctx = TraceContextHolder.getContext();
            if (ctx != null) {
                ctx.setSpanId(parent.getSpanId());
                ctx.setParentSpanId(parent.getParentSpanId());
                TraceContextHolder.setContext(ctx);
            }
        }

        if (slowOperationDetector != null) {
            slowOperationDetector.analyzeSpan(span);
        }
        
        publishEvent("SPAN_COMPLETED", span);
    }

    public void recordException(TraceSpan span, Throwable ex) {
        span.setException(ex);
        span.setStatus("FAILED");
        span.getAttributes().put("error.message", ex.getMessage());
        span.getAttributes().put("error.type", ex.getClass().getName());
        
        // Will be correlated with incident framework automatically via aspects or slow op detector logic later.
    }

    private void publishEvent(String eventType, TraceSpan span) {
        if (eventPublisher != null) {
            // We can publish an internal event wrapper if needed.
            // For now, simple logging representation is a fallback.
        }
    }
}
