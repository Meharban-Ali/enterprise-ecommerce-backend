// ─────────────────────────────────────────────────────────────
// ProductMapper.java
// ─────────────────────────────────────────────────────────────
package com.redis.product.mapper;

import com.redis.product.dto.request.ProductRequest;
import com.redis.product.dto.response.ProductResponse;
import com.redis.product.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ProductMapper {

    // ─── Constants ─────────────────────────────────────────────────────────────
    private static final int  LOW_STOCK_THRESHOLD = 10;
    private static final Locale INDIA_LOCALE      = Locale.forLanguageTag("en-IN");

    // ═══════════════════════════════════════════════════════════════════════════
    //  RequestDTO → Entity
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Create ke liye — fresh Entity banata hai
     */
    public Product toEntity(ProductRequest dto) {
        return Product.builder()
                .name(sanitizeName(dto.getName()))
                .price(roundPrice(dto.getPrice()))
                .rating(roundRating(dto.getRating()))
                .stockQuantity(dto.getStockQuantity())
                .build();
    }

    /**
     * Update ke liye — existing Entity fields update karta hai
     * (id, version, createdAt unchanged rehte hain)
     */
    public void updateEntity(Product product, ProductRequest dto) {
        product.setName(sanitizeName(dto.getName()));
        product.setPrice(roundPrice(dto.getPrice()));
        product.setRating(roundRating(dto.getRating()));
        product.setStockQuantity(dto.getStockQuantity());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Entity → ResponseDTO
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Single Entity → ResponseDTO
     */
    public ProductResponse toResponse(Product product) {
        int stock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .rating(product.getRating())
                .stockQuantity(stock)
                .stockStatus(resolveStockStatus(stock))
                .priceFormatted(formatPrice(product.getPrice()))
                .isAvailable(stock > 0)
                .categoryId(categoryId)
                .categoryName(categoryName)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    /**
     * List<Entity> → List<ResponseDTO>  — Service/Controller mein seedha use karo
     */
    public List<ProductResponse> toResponseList(List<Product> products) {
        return products.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Private Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /** Cleans up trailing/leading spaces and collapses multiple internal spaces to a single space. */
    private String sanitizeName(String name) {
        if (name == null) return null;
        return name.trim().replaceAll("\\s{2,}", " ");
    }

    /** Rounds price to exactly 2 decimal places using HALF_UP rounding. */
    private BigDecimal roundPrice(BigDecimal price) {
        if (price == null) return null;
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    /** Rounds rating to exactly 1 decimal place using HALF_UP rounding. */
    private BigDecimal roundRating(BigDecimal rating) {
        if (rating == null) return null;
        return rating.setScale(1, RoundingMode.HALF_UP);
    }

    /** Resolves readable stock status from stock quantity level. */
    private String resolveStockStatus(int stock) {
        if (stock == 0)                  return "OUT_OF_STOCK";
        if (stock <= LOW_STOCK_THRESHOLD) return "LOW_STOCK";
        return                                   "IN_STOCK";
    }

    /** Formats a BigDecimal price to Indian Rupee localized currency string. */
    private String formatPrice(BigDecimal price) {
        if (price == null) return null;
        NumberFormat formatter = NumberFormat.getCurrencyInstance(INDIA_LOCALE);
        formatter.setCurrency(Currency.getInstance("INR"));
        return formatter.format(price);
    }
}