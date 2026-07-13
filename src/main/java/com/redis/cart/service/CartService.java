package com.redis.cart.service;

import com.redis.cart.dto.request.CartItemRequest;
import com.redis.cart.dto.response.CartResponse;

public interface CartService {

    /** Get the current user's cart. Creates one if it doesn't exist. */
    CartResponse getCart(Long userId);

    /** Add a product to the cart. If already present, increments quantity. */
    CartResponse addItem(Long userId, CartItemRequest request);

    /** Update the quantity of an existing cart item. */
    CartResponse updateItem(Long userId, Long itemId, CartItemRequest request);

    /** Remove a specific item from the cart. */
    CartResponse removeItem(Long userId, Long itemId);

    /** Clear all items from the cart. */
    CartResponse clearCart(Long userId);
}
