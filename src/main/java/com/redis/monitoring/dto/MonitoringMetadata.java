package com.redis.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringMetadata {
    private LocalDateTime generatedAt;
    private String generatedBy;
    private long executionTimeMs;
    private boolean cacheHit;
    private String apiVersion;
    private String correlationId;
}
