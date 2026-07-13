package com.redis.monitoring.controller;

import com.redis.common.dto.ApiResponse;
import com.redis.monitoring.dto.request.AlertRuleRequest;
import com.redis.monitoring.dto.response.AlertRuleResponse;
import com.redis.monitoring.service.AlertRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/alerts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AlertController {

    private final AlertRuleService alertRuleService;

    @GetMapping("/rules")
    public ResponseEntity<ApiResponse<List<AlertRuleResponse>>> getRules() {
        List<AlertRuleResponse> list = alertRuleService.getAllRules();
        return ResponseEntity.ok(ApiResponse.success("Alert rules retrieved successfully", list));
    }

    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> createRule(@Valid @RequestBody AlertRuleRequest request) {
        String username = getUsername();
        try {
            AlertRuleResponse response = alertRuleService.createRule(request, username);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Alert rule created successfully", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> updateRule(
            @PathVariable Long id,
            @Valid @RequestBody AlertRuleRequest request
    ) {
        String username = getUsername();
        AlertRuleResponse response = alertRuleService.updateRule(id, request, username);
        return ResponseEntity.ok(ApiResponse.success("Alert rule updated successfully", response));
    }

    @PatchMapping("/rules/{id}/enable")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> enableRule(@PathVariable Long id) {
        String username = getUsername();
        AlertRuleResponse response = alertRuleService.enableRule(id, username);
        return ResponseEntity.ok(ApiResponse.success("Alert rule enabled successfully", response));
    }

    @PatchMapping("/rules/{id}/disable")
    public ResponseEntity<ApiResponse<AlertRuleResponse>> disableRule(@PathVariable Long id) {
        String username = getUsername();
        AlertRuleResponse response = alertRuleService.disableRule(id, username);
        return ResponseEntity.ok(ApiResponse.success("Alert rule disabled successfully", response));
    }

    private String getUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
