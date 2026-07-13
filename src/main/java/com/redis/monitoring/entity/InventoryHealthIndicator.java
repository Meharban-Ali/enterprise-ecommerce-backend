package com.redis.monitoring.entity;

import com.redis.monitoring.service.HealthIndicatorService;

import com.redis.reliability.dto.ModuleHealthResponse;
import com.redis.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryHealthIndicator implements HealthIndicatorService, org.springframework.boot.actuate.health.HealthIndicator {

    private final ProductRepository productRepository;

    @Override
    public String getName() {
        return "Inventory";
    }

    @Override
    public ModuleHealthResponse checkHealth() {
        Map<String, Object> details = new HashMap<>();
        try {
            long total = productRepository.count();
            long outOfStock = productRepository.countOutOfStock();
            long lowStock = productRepository.countLowStock(10); // threshold = 10

            details.put("totalProductsCount", total);
            details.put("outOfStockCount", outOfStock);
            details.put("lowStockCount", lowStock);

            String status = "UP";
            String message = "Inventory is healthy";

            // If more than 30% of catalog is low or out of stock, we can transition to WARNING
            if (total > 0 && ((double) (outOfStock + lowStock) / total) > 0.3) {
                status = "WARNING";
                message = "High volume of low/out of stock products: " + (outOfStock + lowStock);
            }

            return ModuleHealthResponse.builder()
                    .moduleName(getName())
                    .status(status)
                    .message(message)
                    .details(details)
                    .build();
        } catch (Exception e) {
            log.error("Inventory health check failed: {}", e.getMessage());
            details.put("error", e.getMessage());
            return ModuleHealthResponse.builder()
                    .moduleName(getName())
                    .status("WARNING") // Internal module exception -> WARNING
                    .message("Inventory health check degraded: " + e.getMessage())
                    .details(details)
                    .build();
        }
    }

    @Override
    public org.springframework.boot.actuate.health.Health health() {
        ModuleHealthResponse res = checkHealth();
        org.springframework.boot.actuate.health.Health.Builder builder;
        if ("UP".equalsIgnoreCase(res.getStatus())) {
            builder = org.springframework.boot.actuate.health.Health.up();
        } else if ("DEGRADED".equalsIgnoreCase(res.getStatus())) {
            builder = org.springframework.boot.actuate.health.Health.status("DEGRADED");
        } else if ("WARNING".equalsIgnoreCase(res.getStatus())) {
            builder = org.springframework.boot.actuate.health.Health.status("WARNING");
        } else {
            builder = org.springframework.boot.actuate.health.Health.down();
        }
        if (res.getMessage() != null) {
            builder.withDetail("message", res.getMessage());
        }
        if (res.getDetails() != null) {
            builder.withDetails(res.getDetails());
        }
        return builder.build();
    }
}
