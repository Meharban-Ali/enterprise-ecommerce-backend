package com.redis.analytics.controller;

import com.redis.analytics.dto.AnalyticsResponse;
import com.redis.common.dto.ApiResponse;
import com.redis.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalytics() {
        log.info("API GET /api/analytics — Fetch platform analytics");
        AnalyticsResponse response = analyticsService.getAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Platform analytics retrieved successfully", response));
    }
}
