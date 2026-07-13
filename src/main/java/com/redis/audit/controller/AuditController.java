package com.redis.audit.controller;

import com.redis.user.entity.User;

import com.redis.audit.dto.response.AuditLogResponse;
import com.redis.audit.dto.request.AuditSearchRequest;
import com.redis.audit.dto.response.AuditSummaryResponse;
import com.redis.common.dto.ApiResponse;
import com.redis.audit.entity.AuditActionType;
import com.redis.audit.entity.AuditStatus;
import com.redis.common.entity.ResourceType;
import com.redis.audit.service.AuditService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AuditSummaryResponse>> getAuditSummary() {
        log.info("API GET /api/admin/audit/summary — request received");
        AuditSummaryResponse response = auditService.getAuditSummary();
        return ResponseEntity.ok(ApiResponse.success("Audit log summary statistics retrieved successfully", response));
    }

    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> searchLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        log.info("API GET /api/admin/audit/logs — searching with filters");
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        AuditSearchRequest searchRequest = AuditSearchRequest.builder()
                .userId(userId)
                .email(email)
                .actionType(actionType != null ? AuditActionType.valueOf(actionType.toUpperCase()) : null)
                .status(status != null ? AuditStatus.valueOf(status.toUpperCase()) : null)
                .resourceType(resourceType != null ? ResourceType.valueOf(resourceType.toUpperCase()) : null)
                .resourceId(resourceId)
                .correlationId(correlationId)
                .requestId(requestId)
                .startDate(startDate != null ? LocalDateTime.parse(startDate) : null)
                .endDate(endDate != null ? LocalDateTime.parse(endDate) : null)
                .build();

        Page<AuditLogResponse> response = auditService.searchLogs(searchRequest, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", response));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getUserAuditHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("API GET /api/admin/audit/users/{} — request received", id);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AuditLogResponse> response = auditService.getUserAuditHistory(id, pageable);
        return ResponseEntity.ok(ApiResponse.success("User audit history logs retrieved successfully", response));
    }

    @GetMapping("/actions/{type}")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getLogsByActionType(
            @PathVariable String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("API GET /api/admin/audit/actions/{} — request received", type);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        AuditSearchRequest searchRequest = AuditSearchRequest.builder()
                .actionType(AuditActionType.valueOf(type.toUpperCase()))
                .build();
        Page<AuditLogResponse> response = auditService.searchLogs(searchRequest, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs filtered by action retrieved successfully", response));
    }

    @GetMapping("/resource/{type}/{id}")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getLogsByResource(
            @PathVariable String type,
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("API GET /api/admin/audit/resource/{}/{} — request received", type, id);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        AuditSearchRequest searchRequest = AuditSearchRequest.builder()
                .resourceType(ResourceType.valueOf(type.toUpperCase()))
                .resourceId(id)
                .build();
        Page<AuditLogResponse> response = auditService.searchLogs(searchRequest, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs filtered by resource retrieved successfully", response));
    }

    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogsByCorrelationId(@PathVariable String correlationId) {
        log.info("API GET /api/admin/audit/correlation/{} — request received", correlationId);
        List<AuditLogResponse> response = auditService.getLogsByCorrelationId(correlationId);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved by correlation ID", response));
    }

    @GetMapping("/export")
    public void exportLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) String correlationId,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletResponse response) throws IOException {

        log.info("API GET /api/admin/audit/export — exporting logs as CSV");
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-export.csv\"");

        AuditSearchRequest searchRequest = AuditSearchRequest.builder()
                .userId(userId)
                .email(email)
                .actionType(actionType != null ? AuditActionType.valueOf(actionType.toUpperCase()) : null)
                .status(status != null ? AuditStatus.valueOf(status.toUpperCase()) : null)
                .resourceType(resourceType != null ? ResourceType.valueOf(resourceType.toUpperCase()) : null)
                .resourceId(resourceId)
                .correlationId(correlationId)
                .requestId(requestId)
                .startDate(startDate != null ? LocalDateTime.parse(startDate) : null)
                .endDate(endDate != null ? LocalDateTime.parse(endDate) : null)
                .build();

        auditService.exportLogsToCsv(searchRequest, response.getWriter());
    }
}
