package com.redis.common.controller;

import com.redis.observability.service.RedisMetricsService;
import com.redis.observability.service.QueueMetricsService;
import com.redis.observability.service.RuntimeDiagnosticsService;
import com.redis.observability.service.PerformanceMetricsService;
import com.redis.observability.entity.SchedulerMetricsAspect;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/observability")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ObservabilityController {

    private final PerformanceMetricsService metricsService;
    private final RuntimeDiagnosticsService runtimeDiagnosticsService;
    private final RedisMetricsService redisMetricsService;
    private final QueueMetricsService queueMetricsService;
    private final SchedulerMetricsAspect schedulerMetricsAspect;

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardSnapshot() {
        // High-performance snapshot response
        Map<String, Object> dashboard = new HashMap<>();
        
        dashboard.put("metrics", metricsService.getMetricsSnapshot());
        dashboard.put("runtime", runtimeDiagnosticsService.getDiagnosticsSnapshot());
        dashboard.put("redis", redisMetricsService.getRedisStats());
        dashboard.put("queues", queueMetricsService.getQueueStats());
        
        if (schedulerMetricsAspect != null) {
            dashboard.put("schedulers", schedulerMetricsAspect.getSchedulerStats());
        }
        
        // Calculate observability health score based on metrics
        dashboard.put("observabilityScore", calculateHealthScore(dashboard));

        return ResponseEntity.ok(dashboard);
    }
    
    private double calculateHealthScore(Map<String, Object> dashboard) {
        // Simple logic for Health Score (0-100)
        double score = 100.0;
        
        Map<String, Object> runtime = (Map<String, Object>) dashboard.get("runtime");
        if (runtime != null) {
            int blocked = (int) runtime.getOrDefault("blockedThreads", 0);
            if (blocked > 10) score -= 10;
        }
        
        return Math.max(0.0, score);
    }
}
