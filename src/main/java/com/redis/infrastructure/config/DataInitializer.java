package com.redis.infrastructure.config;

import com.redis.notification.entity.Notification;
import com.redis.payment.entity.Payment;
import com.redis.infrastructure.config.DataInitializerProperties;

import com.redis.product.entity.Product;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.monitoring.entity.AlertRule;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.monitoring.entity.AlertSource;
import com.redis.product.repository.ProductRepository;
import com.redis.user.repository.UserRepository;
import com.redis.monitoring.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataInitializerProperties properties;

    @Override
    public void run(String... args) {
        log.info("DataInitializer running: Checking system initial requirements...");
        
        try {
            initializeSuperAdmin();
        } catch (Exception ex) {
            log.error("Failed to seed default Super Admin on startup: {}", ex.getMessage(), ex);
        }

        try {
            initializeAlertRules();
        } catch (Exception ex) {
            log.error("Failed to seed default alert rules: {}", ex.getMessage(), ex);
        }

        if (!properties.isEnabled()) {
            log.info("Sample product seeder is disabled — skipping product data seeding");
            return;
        }

        log.info("Sample product seeder is enabled — checking if product data needs seeding...");
        try {
            initializeProducts();
        } catch (Exception ex) {
            log.error("Failed to initialize sample product data: {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void initializeSuperAdmin() {
        if (!userRepository.existsByRole(Role.ROLE_SUPER_ADMIN)) {
            log.info("ROLE_SUPER_ADMIN does not exist — seeding default Super Admin account...");
            
            User superAdmin = User.builder()
                    .username("superadmin")
                    .email("superadmin@ecommerce.local")
                    .password(passwordEncoder.encode("ChangeMe@123"))
                    .role(Role.ROLE_SUPER_ADMIN)
                    .accountEnabled(true)
                    .accountNonLocked(true)
                    .build();
            
            userRepository.save(superAdmin);
            log.info("Default Super Admin seeded successfully: superadmin@ecommerce.local");
        } else {
            log.debug("ROLE_SUPER_ADMIN already exists — skipping default Super Admin seeding");
        }
    }

    @Transactional
    public void initializeAlertRules() {
        seedAlertRule("DB_DOWN", "Database Connection Down", AlertSource.DATABASE, AlertSeverity.CRITICAL, 0.0, 10, 60);
        seedAlertRule("REDIS_DOWN", "Redis Cache Down", AlertSource.REDIS, AlertSeverity.HIGH, 0.0, 10, 60);
        seedAlertRule("SCHEDULER_FAIL", "Scheduler Executions Failing", AlertSource.SCHEDULER, AlertSeverity.HIGH, 0.0, 30, 120);
        seedAlertRule("NOTIF_FAIL_RATE", "Notification Failure Rate Exceeds Limit", AlertSource.NOTIFICATION, AlertSeverity.MEDIUM, 0.10, 30, 120);
        seedAlertRule("PAYMENT_FAIL_RATE", "Payment Transaction Failure Rate Exceeds Limit", AlertSource.PAYMENT, AlertSeverity.CRITICAL, 0.05, 30, 120);
        seedAlertRule("DISK_USAGE_HIGH", "Free disk space below limit", AlertSource.SYSTEM, AlertSeverity.HIGH, 85.0, 60, 300);
        seedAlertRule("MEMORY_USAGE_HIGH", "JVM Heap utilization exceeds limit", AlertSource.SYSTEM, AlertSeverity.MEDIUM, 90.0, 30, 120);
        seedAlertRule("FEATURE_FLAG_DISABLED", "Critical Feature Flag Disabled", AlertSource.SYSTEM, AlertSeverity.HIGH, 0.0, 30, 60);
        seedAlertRule("CONFIG_CORRUPT", "Configuration Integrity Corrupted", AlertSource.SYSTEM, AlertSeverity.CRITICAL, 0.0, 30, 60);
        seedAlertRule("DR_VALIDATION_FAILED", "Disaster Recovery Verification Failed", AlertSource.SYSTEM, AlertSeverity.CRITICAL, 0.0, 30, 60);
    }

    private void seedAlertRule(String code, String name, AlertSource source, AlertSeverity severity, double threshold, int interval, int cooldown) {
        if (alertRuleRepository.findByRuleCode(code).isEmpty()) {
            AlertRule rule = AlertRule.builder()
                    .ruleCode(code)
                    .ruleName(name)
                    .source(source)
                    .severity(severity)
                    .threshold(threshold)
                    .enabled(true)
                    .evaluationIntervalSeconds(interval)
                    .cooldownSeconds(cooldown)
                    .notificationEnabled(true)
                    .build();
            rule.setUpdatedBy("SYSTEM");
            alertRuleRepository.save(rule);
            log.info("Seeded default alert rule: {}", code);
        }
    }

    private void initializeProducts() {
        long existingCount = productRepository.count();

        if (existingCount > 0) {
            log.info("Database already has {} product(s) — skipping seed to prevent duplication", existingCount);
            return;
        }

        List<Product> sampleProducts = buildSampleProducts();
        productRepository.saveAll(sampleProducts);
        log.info("{} sample products seeded into database successfully", sampleProducts.size());
    }

    private List<Product> buildSampleProducts() {
        return List.of(
                Product.builder()
                        .name("Gaming Laptop - ASUS ROG")
                        .price(new BigDecimal("85000.00"))
                        .rating(new BigDecimal("4.5"))
                        .stockQuantity(50)
                        .build(),
                Product.builder()
                        .name("iPhone 15 Pro Max")
                        .price(new BigDecimal("134900.00"))
                        .rating(new BigDecimal("4.8"))
                        .stockQuantity(200)
                        .build(),
                Product.builder()
                        .name("Samsung Galaxy S24 Ultra")
                        .price(new BigDecimal("129999.00"))
                        .rating(new BigDecimal("4.7"))
                        .stockQuantity(150)
                        .build(),
                Product.builder()
                        .name("Sony WH-1000XM5 Headphones")
                        .price(new BigDecimal("29990.00"))
                        .rating(new BigDecimal("4.6"))
                        .stockQuantity(100)
                        .build(),
                Product.builder()
                        .name("Dell 27-inch 4K Monitor")
                        .price(new BigDecimal("45000.00"))
                        .rating(new BigDecimal("4.4"))
                        .stockQuantity(75)
                        .build()
        );
    }
}
