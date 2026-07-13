package com.redis.user.service;

import com.redis.auth.dto.request.RegisterRequest;
import com.redis.product.dto.response.ProductResponse;
import com.redis.product.dto.response.ProductStatsResponse;
import com.redis.user.dto.response.UserResponse;
import com.redis.user.dto.response.UserSessionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SuperAdminService {

    Page<UserSessionResponse> getOnlineUsers(Pageable pageable);

    Page<UserSessionResponse> getOfflineUsers(Pageable pageable);

    Page<UserResponse> listUsers(String role, String search, Boolean enabled, Boolean nonLocked, Pageable pageable);

    UserResponse updateUser(Long id, UserResponse request);

    void deleteUser(Long id);

    UserResponse changeStatus(Long id, String action);

    ProductStatsResponse getProductStats();

    Page<ProductResponse> getProductInventory(Pageable pageable);

    // Creates a new business Admin account (ROLE_ADMIN)
    UserResponse createAdmin(RegisterRequest request);

    // Resets a user/admin password directly and invalidates active session/refresh tokens
    void resetUserPassword(Long id, String newPassword);
}
