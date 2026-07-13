package com.redis.analytics.dto;

import com.redis.product.entity.Product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsResponse {

    // User Metrics
    private long totalUsers;
    private long totalAdmins;
    private long totalActiveUsers;

    // Product Metrics
    private long totalProducts;
    private long outOfStockProducts;
    private long lowStockProducts;

    // Order Metrics
    private long totalOrders;
    private long pendingOrders;
    private long completedOrders;
    private long cancelledOrders;

    // Revenue Metrics
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;

    // Category Breakdown
    private Map<String, Long> ordersByStatus;
    private Map<String, Long> productsByCategory;
}