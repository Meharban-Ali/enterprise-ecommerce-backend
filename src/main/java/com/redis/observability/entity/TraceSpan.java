package com.redis.observability.entity;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
public class TraceSpan {
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String operationName;
    private SpanType type;
    private String module;

    private Instant startTime;
    private Instant endTime;
    
    @Builder.Default
    private boolean isFinished = false;
    
    private String status;
    private Throwable exception;

    @Builder.Default
    private Map<String, Object> attributes = new ConcurrentHashMap<>();
    
    private long selfTimeMs;
    private long totalTimeMs;
    private long childDurationMs;
}
