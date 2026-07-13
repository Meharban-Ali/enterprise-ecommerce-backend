package com.redis.observability.entity;

import com.redis.observability.entity.PerformanceMetric;

/**
 * OpenTelemetry Abstraction Layer for Metric Exporting.
 */
public interface MetricExporter {
    void export(PerformanceMetric metric);

    class NoOpMetricExporter implements MetricExporter {
        @Override
        public void export(PerformanceMetric metric) {
            // Future OTEL integrations will replace this logic
        }
    }
}
