package com.redis.cart.service;

import com.redis.cart.dto.response.WishlistResponse;

public interface WishlistService {

    /** Get the current user's wishlist. Creates one if it doesn't exist. */
    WishlistResponse getWishlist(Long userId);

    /** Add a product to the wishlist. */
    WishlistResponse addProduct(Long userId, Long productId);

    /** Remove a product from the wishlist. */
    WishlistResponse removeProduct(Long userId, Long productId);

    /** Clear all products from the wishlist. */
    WishlistResponse clearWishlist(Long userId);
}
