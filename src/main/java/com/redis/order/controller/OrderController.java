package com.redis.order.controller;

import com.redis.order.entity.Order;

import com.redis.order.dto.request.OrderRequest;
import com.redis.order.dto.request.OrderStatusUpdateRequest;
import com.redis.common.dto.ApiResponse;
import com.redis.order.dto.response.OrderResponse;
import com.redis.user.entity.User;
import com.redis.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody OrderRequest request) {
        log.info("API POST /api/orders — Place order for user: {}", user.getEmail());
        OrderResponse response = orderService.placeOrder(user.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order placed successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        log.info("API GET /api/orders/my — Fetch orders for user: {}", user.getEmail());
        Page<OrderResponse> response = orderService.getMyOrders(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        log.info("API GET /api/orders/{} — Fetch order details", orderId);
        boolean isAdmin = user.getRole().name().equals("ROLE_ADMIN") || user.getRole().name().equals("ROLE_SUPER_ADMIN");
        OrderResponse response = orderService.getOrderById(orderId, user.getId(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Order retrieved successfully", response));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        log.info("API POST /api/orders/{}/cancel — Cancel order", orderId);
        OrderResponse response = orderService.cancelOrder(orderId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        log.info("API GET /api/orders — Admin fetch all orders with status: {}", status);
        Page<OrderResponse> response = orderService.getAllOrders(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("All orders retrieved successfully", response));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        log.info("API PATCH /api/orders/{}/status — Update order status to: {}", orderId, request.getStatus());
        OrderResponse response = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", response));
    }
}
