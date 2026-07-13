package com.redis.reliability.dto;

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
public class SchedulerStatusResponse {
    private String schedulerName;
    private LocalDateTime lastExecutionTime;
    private long lastExecutionDurationMs;
    private double averageExecutionTimeMs;
    private long executionCount;
    private long failuresCount;
    private double successRate; // percentage (0.0 to 100.0)
    private LocalDateTime lastFailureTime;
    private String status; // UP, WARNING, DOWN, IDLE

    // Sprint 9.1 enhancements
    private Long minExecutionTimeMs;
    private Long maxExecutionTimeMs;
    private LocalDateTime lastSuccessfulExecution;
    private LocalDateTime lastFailedExecution;
    private double failurePercentage;
    private double successPercentage;
    private long totalProcessedRecords;
}
