package com.redis.observability.entity;

import com.redis.observability.entity.TraceSpan;

/**
 * OpenTelemetry Abstraction Layer for Trace Exporting.
 * Decouples business logic from OTEL SDK directly.
 */
public interface TraceExporter {
    void export(TraceSpan span);

    class NoOpTraceExporter implements TraceExporter {
        @Override
        public void export(TraceSpan span) {
            // Future OTEL integrations will replace this logic
        }
    }
}
