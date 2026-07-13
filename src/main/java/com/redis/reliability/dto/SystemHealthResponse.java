package com.redis.reliability.dto;

import com.redis.monitoring.dto.MonitoringMetadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthResponse {
    private String applicationStatus; // UP, WARNING, DEGRADED, DOWN
    private String databaseStatus;
    private String redisStatus;
    private String notificationStatus;
    private String paymentStatus;
    private String schedulerStatus;
    private long uptimeSeconds;
    private int activeUsersCount;
    private LocalDateTime timestamp;
    private List<ModuleHealthResponse> modules;
    private MonitoringMetadata metadata;
}
