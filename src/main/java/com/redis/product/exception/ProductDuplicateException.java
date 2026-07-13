
package com.redis.product.exception;

import com.redis.product.entity.Product;

public class ProductDuplicateException extends RuntimeException {

    public ProductDuplicateException(String name) {
        super("Product already exists with name: " + name);
    }
}