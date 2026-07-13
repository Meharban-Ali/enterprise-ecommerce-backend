package com.redis.product.exception;

import com.redis.product.entity.Product;

public class ProductNotFoundException extends RuntimeException {

    private final Long productId;

    public ProductNotFoundException(Long id) {
        super("Product not found with id: " + id);
        this.productId = id;
    }

    public ProductNotFoundException(String message) {
        super(message);
        this.productId = null;
    }

    public Long getProductId() {
        return productId;
    }
}