package com.redis.user.controller;

import com.redis.product.entity.Product;
import com.redis.user.entity.User;

import com.redis.common.dto.AdminPasswordResetRequest;
import com.redis.auth.dto.request.RegisterRequest;
import com.redis.common.dto.ApiResponse;
import com.redis.product.dto.response.ProductResponse;
import com.redis.product.dto.response.ProductStatsResponse;
import com.redis.user.dto.response.UserResponse;
import com.redis.user.dto.response.UserSessionResponse;
import com.redis.user.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    // ═══════════════════════════════════════════════════════════════════════════
    //  SESSION MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/sessions/online")
    public ResponseEntity<ApiResponse<Page<UserSessionResponse>>> getOnlineUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("API GET /api/super-admin/sessions/online — request received");
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastActivity").descending());
        Page<UserSessionResponse> response = superAdminService.getOnlineUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Online sessions retrieved successfully", response));
    }

    @GetMapping("/sessions/offline")
    public ResponseEntity<ApiResponse<Page<UserSessionResponse>>> getOfflineUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("API GET /api/super-admin/sessions/offline — request received");
        Pageable pageable = PageRequest.of(page, size, Sort.by("lastActivity").descending());
        Page<UserSessionResponse> response = superAdminService.getOfflineUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Offline sessions retrieved successfully", response));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  USER MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Boolean nonLocked,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        log.info("API GET /api/super-admin/users — filter request received");
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<UserResponse> response = superAdminService.listUsers(role, search, enabled, nonLocked, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users list retrieved successfully", response));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @RequestBody UserResponse request) {
        log.info("API PUT /api/super-admin/users/{} — update request received", id);
        UserResponse response = superAdminService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User details updated successfully", response));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        log.info("API DELETE /api/super-admin/users/{} — delete request received", id);
        superAdminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User account deleted successfully"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  STATUS TRANSITION ENGINES
    // ═══════════════════════════════════════════════════════════════════════════

    @PatchMapping("/users/{id}/activate")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(@PathVariable Long id) {
        log.info("API PATCH /api/super-admin/users/{}/activate — request received", id);
        UserResponse response = superAdminService.changeStatus(id, "activate");
        return ResponseEntity.ok(ApiResponse.success("User account activated successfully", response));
    }

    @PatchMapping("/users/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(@PathVariable Long id) {
        log.info("API PATCH /api/super-admin/users/{}/deactivate — request received", id);
        UserResponse response = superAdminService.changeStatus(id, "deactivate");
        return ResponseEntity.ok(ApiResponse.success("User account deactivated successfully", response));
    }

    @PatchMapping("/users/{id}/enable")
    public ResponseEntity<ApiResponse<UserResponse>> enableUser(@PathVariable Long id) {
        log.info("API PATCH /api/super-admin/users/{}/enable — request received", id);
        UserResponse response = superAdminService.changeStatus(id, "enable");
        return ResponseEntity.ok(ApiResponse.success("User account enabled successfully", response));
    }

    @PatchMapping("/users/{id}/disable")
    public ResponseEntity<ApiResponse<UserResponse>> disableUser(@PathVariable Long id) {
        log.info("API PATCH /api/super-admin/users/{}/disable — request received", id);
        UserResponse response = superAdminService.changeStatus(id, "disable");
        return ResponseEntity.ok(ApiResponse.success("User account disabled successfully", response));
    }

    @PatchMapping("/users/{id}/lock")
    public ResponseEntity<ApiResponse<UserResponse>> lockUser(@PathVariable Long id) {
        log.info("API PATCH /api/super-admin/users/{}/lock — request received", id);
        UserResponse response = superAdminService.changeStatus(id, "lock");
        return ResponseEntity.ok(ApiResponse.success("User account locked successfully", response));
    }

    @PatchMapping("/users/{id}/unlock")
    public ResponseEntity<ApiResponse<UserResponse>> unlockUser(@PathVariable Long id) {
        log.info("API PATCH /api/super-admin/users/{}/unlock — request received", id);
        UserResponse response = superAdminService.changeStatus(id, "unlock");
        return ResponseEntity.ok(ApiResponse.success("User account unlocked successfully", response));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PRODUCT ACCESS EXTENSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/products/stats")
    public ResponseEntity<ApiResponse<ProductStatsResponse>> getProductStats() {
        log.info("API GET /api/super-admin/products/stats — request received");
        ProductStatsResponse response = superAdminService.getProductStats();
        return ResponseEntity.ok(ApiResponse.success("Product statistics retrieved successfully", response));
    }

    @GetMapping("/products/inventory")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProductInventory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        log.info("API GET /api/super-admin/products/inventory — request received");
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> response = superAdminService.getProductInventory(pageable);
        return ResponseEntity.ok(ApiResponse.success("Product inventory overview retrieved successfully", response));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  ADMIN MANAGEMENT EXTENSIONS
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping("/admins")
    public ResponseEntity<ApiResponse<UserResponse>> createAdmin(
            @Valid @RequestBody RegisterRequest request) {
        log.info("API POST /api/super-admin/admins — admin creation request for email: {}", request.getEmail());
        UserResponse response = superAdminService.createAdmin(request);
        return ResponseEntity.ok(ApiResponse.success("Admin account created successfully", response));
    }

    @PatchMapping("/users/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(
            @PathVariable Long id,
            @Valid @RequestBody AdminPasswordResetRequest request) {
        log.info("API PATCH /api/super-admin/users/{id}/reset-password — password reset request for user ID: {}", id);
        superAdminService.resetUserPassword(id, request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully. Active sessions invalidated."));
    }
}
