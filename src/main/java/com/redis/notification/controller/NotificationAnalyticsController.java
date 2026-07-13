package com.redis.notification.controller;

import com.redis.notification.dto.response.NotificationPriorityStatisticsResponse;
import com.redis.notification.dto.response.NotificationDashboardResponse;
import com.redis.notification.dto.response.NotificationTypeStatisticsResponse;
import com.redis.notification.dto.response.NotificationAnalyticsResponse;
import com.redis.notification.dto.response.NotificationResponse;
import com.redis.notification.dto.response.NotificationChannelStatisticsResponse;

import com.redis.notification.service.NotificationAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/notifications/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class NotificationAnalyticsController {

    private final NotificationAnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<NotificationDashboardResponse> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(analyticsService.getDashboard(start, end));
    }

    @GetMapping("/summary")
    public ResponseEntity<NotificationAnalyticsResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(analyticsService.getOverallAnalytics(start, end));
    }

    @GetMapping("/channels")
    public ResponseEntity<List<NotificationChannelStatisticsResponse>> getChannels(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(analyticsService.getChannelStatistics(start, end));
    }

    @GetMapping("/types")
    public ResponseEntity<List<NotificationTypeStatisticsResponse>> getTypes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(analyticsService.getTypeStatistics(start, end));
    }

    @GetMapping("/priorities")
    public ResponseEntity<List<NotificationPriorityStatisticsResponse>> getPriorities(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(analyticsService.getPriorityStatistics(start, end));
    }

    @GetMapping("/retries")
    public ResponseEntity<NotificationAnalyticsResponse> getRetries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(analyticsService.getRetryStatistics(start, end));
    }

    @GetMapping("/dead-letter")
    public ResponseEntity<List<NotificationResponse>> getDeadLetter(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(analyticsService.getDeadLetterSummary(start, end, page, size));
    }

    @GetMapping("/failures")
    public ResponseEntity<List<NotificationResponse>> getFailures(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(analyticsService.getRecentFailures(start, end, page, size));
    }

    @GetMapping("/delivery-performance")
    public ResponseEntity<NotificationAnalyticsResponse> getDeliveryPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(analyticsService.getOverallAnalytics(start, end));
    }

    @GetMapping("/date-range")
    public ResponseEntity<NotificationAnalyticsResponse> getDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(analyticsService.getOverallAnalytics(start, end));
    }
}
