package com.redis.observability.dto.response;

import com.redis.monitoring.dto.MonitoringMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JvmMetricsResponse {
    private long heapUsedBytes;
    private long heapMaxBytes;
    private double heapUtilizationPercentage;
    private long nonHeapUsedBytes;
    private int threadCount;
    private int daemonThreadCount;
    private int peakThreadCount;
    private long garbageCollectionCount;
    private long garbageCollectionTimeMs;
    private MonitoringMetadata metadata;
}
