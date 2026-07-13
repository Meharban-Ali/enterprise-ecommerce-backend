// ─────────────────────────────────────────────────────────────
// ProductRequestDTO.java
// ─────────────────────────────────────────────────────────────
package com.redis.product.dto.request;

import com.redis.product.entity.Product;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Product name must be between 2 and 255 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9 _\\-().&]+$",
        message = "Product name contains invalid characters"
    )
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "Price must be at least 0.01")
    @DecimalMax(value = "999999.99", inclusive = true, message = "Price must not exceed 999,999.99")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 8 integer digits and 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Rating is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rating must be at least 0.0")
    @DecimalMax(value = "5.0", inclusive = true, message = "Rating must not exceed 5.0")
    @Digits(integer = 1, fraction = 1, message = "Rating must be in format X.X (e.g. 4.5)")
    private BigDecimal rating;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock cannot be negative")
    @Max(value = 1_000_000, message = "Stock must not exceed 1,000,000")
    private Integer stockQuantity;

    private Long categoryId; // Optional category mapping
}