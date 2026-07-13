package com.redis.cart.controller;

import com.redis.cart.entity.Cart;

import com.redis.cart.dto.request.CartItemRequest;
import com.redis.common.dto.ApiResponse;
import com.redis.cart.dto.response.CartResponse;
import com.redis.user.entity.User;
import com.redis.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal User user) {
        log.info("API GET /api/cart — Fetch cart for user: {}", user.getEmail());
        CartResponse response = cartService.getCart(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CartItemRequest request) {
        log.info("API POST /api/cart/items — Add item to cart for user: {}", user.getEmail());
        CartResponse response = cartService.addItem(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully", response));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemRequest request) {
        log.info("API PUT /api/cart/items/{} — Update cart item for user: {}", itemId, user.getEmail());
        CartResponse response = cartService.updateItem(user.getId(), itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated successfully", response));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeCartItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long itemId) {
        log.info("API DELETE /api/cart/items/{} — Remove cart item for user: {}", itemId, user.getEmail());
        CartResponse response = cartService.removeItem(user.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully", response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(
            @AuthenticationPrincipal User user) {
        log.info("API DELETE /api/cart — Clear cart for user: {}", user.getEmail());
        CartResponse response = cartService.clearCart(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", response));
    }
}
