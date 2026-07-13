package com.redis.observability.entity;

import org.slf4j.MDC;

public class TraceContextHolder {
    private static final ThreadLocal<TraceContext> CONTEXT = new ThreadLocal<>();

    public static void setContext(TraceContext context) {
        CONTEXT.set(context);
        updateMDC(context);
    }

    public static TraceContext getContext() {
        return CONTEXT.get();
    }

    public static void clearContext() {
        CONTEXT.remove();
        MDC.clear();
    }

    private static void updateMDC(TraceContext context) {
        if (context == null) {
            MDC.clear();
            return;
        }
        if (context.getTraceId() != null) MDC.put("traceId", context.getTraceId());
        if (context.getSpanId() != null) MDC.put("spanId", context.getSpanId());
        if (context.getCorrelationId() != null) MDC.put("correlationId", context.getCorrelationId());
        if (context.getUserId() != null) MDC.put("userId", context.getUserId());
        if (context.getEndpoint() != null) MDC.put("endpoint", context.getEndpoint());
        if (context.getModule() != null) MDC.put("module", context.getModule());
    }
}
