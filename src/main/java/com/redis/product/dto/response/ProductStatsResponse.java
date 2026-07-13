package com.redis.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStatsResponse {
    private long totalProducts;
    private BigDecimal averageRating;
    private long outOfStockCount;
    private long lowStockCount;
}
