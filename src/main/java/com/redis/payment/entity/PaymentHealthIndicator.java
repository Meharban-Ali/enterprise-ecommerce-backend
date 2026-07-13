package com.redis.payment.entity;

import com.redis.monitoring.service.HealthIndicatorService;

import com.redis.reliability.dto.ModuleHealthResponse;
import com.redis.payment.entity.PaymentStatus;
import com.redis.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentHealthIndicator implements HealthIndicatorService, org.springframework.boot.actuate.health.HealthIndicator {

    private final PaymentRepository paymentRepository;

    @Override
    public String getName() {
        return "Payments";
    }

    @Override
    public ModuleHealthResponse checkHealth() {
        Map<String, Object> details = new HashMap<>();
        try {
            long total = paymentRepository.count();
            long pending = paymentRepository.countByStatus(PaymentStatus.PENDING);
            long completed = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
            long failed = paymentRepository.countByStatus(PaymentStatus.FAILED);
            long refunded = paymentRepository.countByStatus(PaymentStatus.REFUNDED);

            details.put("totalPaymentsCount", total);
            details.put("pendingPaymentsCount", pending);
            details.put("completedPaymentsCount", completed);
            details.put("failedPaymentsCount", failed);
            details.put("refundedPaymentsCount", refunded);

            String status = "UP";
            String message = "Payment service is healthy";

            // If fail rate is extremely high, we can transition to WARNING
            if (total > 10 && ((double) failed / total) > 0.4) {
                status = "WARNING";
                message = "High payment failure rate detected: " + String.format("%.2f%%", ((double) failed / total) * 100);
            }

            return ModuleHealthResponse.builder()
                    .moduleName(getName())
                    .status(status)
                    .message(message)
                    .details(details)
                    .build();
        } catch (Exception e) {
            log.error("Payment health check failed: {}", e.getMessage());
            details.put("error", e.getMessage());
            return ModuleHealthResponse.builder()
                    .moduleName(getName())
                    .status("WARNING") // Internal module exception -> WARNING
                    .message("Payment health check degraded: " + e.getMessage())
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
