package com.redis.observability.entity;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
public class PerformanceMetric {
    private String metricName;
    private long durationMs;
    private long value;
    private Instant timestamp;
    private SpanType type;
    
    @Builder.Default
    private Map<String, String> tags = new ConcurrentHashMap<>();
}
