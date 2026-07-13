package com.redis.observability.entity;

import lombok.Builder;
import lombok.Data;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Builder
public class TraceContext {
    private String traceId;
    private String spanId;
    private String parentSpanId;
    private String correlationId;
    private String userId;
    private String endpoint;
    private String module;

    @Builder.Default
    private Map<String, String> tags = new ConcurrentHashMap<>();

    public TraceContext cloneContext() {
        return TraceContext.builder()
                .traceId(this.traceId)
                .spanId(this.spanId)
                .parentSpanId(this.parentSpanId)
                .correlationId(this.correlationId)
                .userId(this.userId)
                .endpoint(this.endpoint)
                .module(this.module)
                .tags(new ConcurrentHashMap<>(this.tags))
                .build();
    }
}
