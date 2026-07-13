package com.redis.analytics.service;

import com.redis.product.entity.Product;

import com.redis.analytics.dto.AnalyticsResponse;
import com.redis.user.entity.Role;
import com.redis.order.entity.OrderStatus;
import com.redis.order.repository.OrderRepository;
import com.redis.product.repository.ProductRepository;
import com.redis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    private static final int LOW_STOCK_THRESHOLD = 10;

    @Override
    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics() {
        log.info("Generating platform analytics");

        // User metrics
        long totalUsers = userRepository.count();
        long totalAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ROLE_ADMIN).count();
        long totalActiveUsers = userRepository.findAll().stream()
                .filter(u -> u.isAccountEnabled()).count();

        // Product metrics
        long totalProducts = productRepository.count();
        long outOfStockProducts = productRepository.findOutOfStockProducts().size();
        long lowStockProducts = productRepository.findLowStockProducts(LOW_STOCK_THRESHOLD).size();

        // Order metrics
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long completedOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED);

        // Revenue
        BigDecimal totalRevenue = orderRepository.getTotalRevenue();
        BigDecimal avgOrderValue = orderRepository.getAverageOrderValue();

        // Order by status breakdown
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            ordersByStatus.put(status.name(), orderRepository.countByStatus(status));
        }

        return AnalyticsResponse.builder()
                .totalUsers(totalUsers)
                .totalAdmins(totalAdmins)
                .totalActiveUsers(totalActiveUsers)
                .totalProducts(totalProducts)
                .outOfStockProducts(outOfStockProducts)
                .lowStockProducts(lowStockProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue)
                .averageOrderValue(avgOrderValue)
                .ordersByStatus(ordersByStatus)
                .build();
    }
}