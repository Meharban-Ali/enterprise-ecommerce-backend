package com.redis.observability.service;

/**
 * OpenTelemetry Abstraction Layer for Log Exporting.
 */
public interface LogExporter {
    void export(String structuredLogJson);

    class NoOpLogExporter implements LogExporter {
        @Override
        public void export(String structuredLogJson) {
            // Future OTEL integrations will replace this logic
        }
    }
}
