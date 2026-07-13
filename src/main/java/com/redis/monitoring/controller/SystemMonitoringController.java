package com.redis.monitoring.controller;

import com.redis.observability.dto.response.JvmMetricsResponse;
import com.redis.observability.dto.response.SystemMetricsResponse;
import com.redis.reliability.dto.RecentErrorsResponse;
import com.redis.reliability.dto.ModulesHealthResponse;
import com.redis.reliability.dto.SystemHealthResponse;
import com.redis.reliability.dto.DashboardResponse;
import com.redis.reliability.dto.SystemInfoResponse;
import com.redis.user.entity.User;

import com.redis.common.dto.ApiResponse;
import com.redis.monitoring.service.SystemMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class SystemMonitoringController {

    private final SystemMonitoringService monitoringService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        long start = System.currentTimeMillis();
        DashboardResponse response = monitoringService.getDashboard();
        logMetrics("/api/admin/system/dashboard", start, 200);
        return ResponseEntity.ok(ApiResponse.success("Operational dashboard data retrieved successfully", response));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<SystemHealthResponse>> getHealth() {
        long start = System.currentTimeMillis();
        SystemHealthResponse response = monitoringService.getSystemHealth();
        logMetrics("/api/admin/system/health", start, 200);
        return ResponseEntity.ok(ApiResponse.success("System health details retrieved successfully", response));
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<SystemMetricsResponse>> getMetrics() {
        long start = System.currentTimeMillis();
        SystemMetricsResponse response = monitoringService.getSystemMetrics();
        logMetrics("/api/admin/system/metrics", start, 200);
        return ResponseEntity.ok(ApiResponse.success("System metrics details retrieved successfully", response));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<SystemInfoResponse>> getInfo() {
        long start = System.currentTimeMillis();
        SystemInfoResponse response = monitoringService.getSystemInfo();
        logMetrics("/api/admin/system/info", start, 200);
        return ResponseEntity.ok(ApiResponse.success("System info details retrieved successfully", response));
    }

    @GetMapping("/jvm")
    public ResponseEntity<ApiResponse<JvmMetricsResponse>> getJvmMetrics() {
        long start = System.currentTimeMillis();
        JvmMetricsResponse response = monitoringService.getJvmMetrics();
        logMetrics("/api/admin/system/jvm", start, 200);
        return ResponseEntity.ok(ApiResponse.success("JVM metrics details retrieved successfully", response));
    }

    @GetMapping("/modules")
    public ResponseEntity<ApiResponse<ModulesHealthResponse>> getModulesHealth() {
        long start = System.currentTimeMillis();
        ModulesHealthResponse response = monitoringService.getModulesHealthWrapped();
        logMetrics("/api/admin/system/modules", start, 200);
        return ResponseEntity.ok(ApiResponse.success("Individual module health details retrieved successfully", response));
    }

    @GetMapping("/recent-errors")
    public ResponseEntity<ApiResponse<RecentErrorsResponse>> getRecentErrors() {
        long start = System.currentTimeMillis();
        RecentErrorsResponse response = monitoringService.getRecentErrorsWrapped();
        logMetrics("/api/admin/system/recent-errors", start, 200);
        return ResponseEntity.ok(ApiResponse.success("Recent system errors retrieved successfully", response));
    }

    private void logMetrics(String endpoint, long startTimeMs, int statusCode) {
        long executionTime = System.currentTimeMillis() - startTimeMs;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String user = (auth != null) ? auth.getName() : "anonymousUser";
        String correlationId = MDC.get("CorrelationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "SYSTEM";
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        log.info("SYSTEM_MONITOR | CorrelationId={} | Endpoint={} | User={} | ExecutionTime={}ms | ResponseStatus={} | Timestamp={}",
                correlationId, endpoint, user, executionTime, statusCode, timestamp);
    }
}
