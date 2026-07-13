package com.redis.infrastructure.governance.controller;

import com.redis.security.dto.response.ApiKeyResponse;
import com.redis.infrastructure.governance.dto.ApiGovernanceDashboardResponse;
import com.redis.security.dto.request.ApiKeyRequest;
import com.redis.security.dto.response.ApiKeyRotationResponse;

import com.redis.common.dto.ApiResponse;
import com.redis.infrastructure.governance.service.ApiGovernanceService;
import com.redis.security.service.ApiKeyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class ApiGovernanceController {

    private final ApiKeyService apiKeyService;
    private final ApiGovernanceService governanceService;

    @GetMapping("/api-keys")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> getApiKeys() {
        List<ApiKeyResponse> keys = apiKeyService.getAllKeys();
        return ResponseEntity.ok(ApiResponse.success("API Keys retrieved successfully", keys));
    }

    @PostMapping("/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> createApiKey(@Valid @RequestBody ApiKeyRequest request) {
        ApiKeyResponse response = apiKeyService.createKey(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("API Key created successfully", response));
    }

    @PostMapping("/api-keys/{id}/rotate")
    public ResponseEntity<ApiResponse<ApiKeyRotationResponse>> rotateApiKey(@PathVariable Long id) {
        ApiKeyRotationResponse response = apiKeyService.rotateKey(id);
        return ResponseEntity.ok(ApiResponse.success("API Key rotated successfully", response));
    }

    @PostMapping("/api-keys/{id}/revoke")
    public ResponseEntity<ApiResponse<Void>> revokeApiKey(@PathVariable Long id) {
        apiKeyService.revokeKey(id);
        return ResponseEntity.ok(ApiResponse.success("API Key revoked successfully", null));
    }

    @DeleteMapping("/api-keys/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteApiKey(@PathVariable Long id) {
        apiKeyService.deleteKey(id);
        return ResponseEntity.ok(ApiResponse.success("API Key deleted successfully", null));
    }

    @GetMapping("/system/governance")
    public ResponseEntity<ApiResponse<ApiGovernanceDashboardResponse>> getGovernanceDashboard() {
        ApiGovernanceDashboardResponse dashboard = governanceService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("API Governance dashboard retrieved successfully", dashboard));
    }
}
