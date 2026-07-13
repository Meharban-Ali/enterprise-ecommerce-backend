// ─────────────────────────────────────────────────────────────
// ProductResponseDTO.java
// ─────────────────────────────────────────────────────────────
package com.redis.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductResponse {

    // ─── Core Fields ───────────────────────────────────────────────────────────
    private Long       id;
    private String     name;
    private BigDecimal price;
    private BigDecimal rating;
    private Integer    stockQuantity;

    // ─── Derived Fields ────────────────────────────────────────────────────────
    private String  stockStatus;     // "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK"
    private String  priceFormatted;  // "₹ 1,999.00"  — display ready
    private Boolean isAvailable;     // totalProduct > 0
    private Long    categoryId;
    private String  categoryName;

    // ─── Audit ─────────────────────────────────────────────────────────────────
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}