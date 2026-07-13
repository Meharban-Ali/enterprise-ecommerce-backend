package com.redis.cart.controller;

import com.redis.product.entity.Product;
import com.redis.cart.entity.Wishlist;

import com.redis.common.dto.ApiResponse;
import com.redis.cart.dto.response.WishlistResponse;
import com.redis.user.entity.User;
import com.redis.cart.service.WishlistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<WishlistResponse>> getWishlist(
            @AuthenticationPrincipal User user) {
        log.info("API GET /api/wishlist — Fetch wishlist for user: {}", user.getEmail());
        WishlistResponse response = wishlistService.getWishlist(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved successfully", response));
    }

    @PostMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> addProductToWishlist(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId) {
        log.info("API POST /api/wishlist/products/{} — Add product to wishlist for user: {}", productId, user.getEmail());
        WishlistResponse response = wishlistService.addProduct(user.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Product added to wishlist successfully", response));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> removeProductFromWishlist(
            @AuthenticationPrincipal User user,
            @PathVariable Long productId) {
        log.info("API DELETE /api/wishlist/products/{} — Remove product from wishlist for user: {}", productId, user.getEmail());
        WishlistResponse response = wishlistService.removeProduct(user.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist successfully", response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<WishlistResponse>> clearWishlist(
            @AuthenticationPrincipal User user) {
        log.info("API DELETE /api/wishlist — Clear wishlist for user: {}", user.getEmail());
        WishlistResponse response = wishlistService.clearWishlist(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Wishlist cleared successfully", response));
    }
}
