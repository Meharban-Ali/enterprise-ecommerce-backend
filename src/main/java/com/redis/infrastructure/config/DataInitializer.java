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
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    @Override
    public void run(String... args) {
        log.info("DataInitializer running: Checking system initial requirements...");
        
        try {
            initializeSuperAdmin();
        } catch (Exception ex) {
            log.error("Failed to execute secure Super Admin bootstrap: {}", ex.getMessage());
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
        if (isBootstrapCompleted() || userRepository.existsByRole(Role.ROLE_SUPER_ADMIN)) {
            log.debug("IDENTITY_BOOTSTRAP | Bootstrap lock active or SUPER_ADMIN role already exists. Skipping bootstrap.");
            return;
        }

        String name = getEnv("SUPER_ADMIN_NAME");
        String email = getEnv("SUPER_ADMIN_EMAIL");
        String password = getEnv("SUPER_ADMIN_PASSWORD");
        String phone = getEnv("SUPER_ADMIN_PHONE");

        if (name == null || name.isBlank() ||
            email == null || email.isBlank() ||
            password == null || password.isBlank() ||
            phone == null || phone.isBlank()) {
            log.warn("IDENTITY_BOOTSTRAP | Skipping bootstrap - super admin credentials environment variables not set.");
            return;
        }

        log.info("IDENTITY_BOOTSTRAP | Starting secure Super Admin identity bootstrap process...");
        publishAudit(null, email, com.redis.audit.entity.AuditActionType.IDENTITY_BOOTSTRAP_STARTED, com.redis.audit.entity.AuditStatus.SUCCESS, "Bootstrap Started");

        // Validations
        if (!email.matches("^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+$")) {
            failBootstrap(email, "Invalid email format");
        }

        boolean isStrong = password.length() >= 12 &&
                           password.matches(".*[A-Z].*") &&
                           password.matches(".*[a-z].*") &&
                           password.matches(".*[0-9].*") &&
                           password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?\\\\|].*");

        if (!isStrong) {
            failBootstrap(email, "Password does not meet complexity requirements (min 12 chars, upper, lower, digit, special)");
        }

        if (!phone.matches("^\\+?[1-9]\\d{1,14}$")) {
            failBootstrap(email, "Invalid phone format");
        }

        if (userRepository.existsByEmail(email)) {
            failBootstrap(email, "Duplicate email");
        }

        if (userRepository.existsByUsername(name)) {
            failBootstrap(email, "Duplicate username");
        }

        User superAdmin = User.builder()
                .username(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .phone(phone)
                .role(Role.ROLE_SUPER_ADMIN)
                .accountEnabled(true)
                .accountNonLocked(true)
                .passwordChangeRequired(true)
                .build();

        userRepository.save(superAdmin);

        markBootstrapCompleted();

        log.info("IDENTITY_BOOTSTRAP | Super Admin identity bootstrap completed successfully.");
        publishAudit(superAdmin.getId(), email, com.redis.audit.entity.AuditActionType.IDENTITY_BOOTSTRAP_COMPLETED, com.redis.audit.entity.AuditStatus.SUCCESS, "Bootstrap Completed");
    }

    private boolean isBootstrapCompleted() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM system_settings WHERE setting_key = 'bootstrap.completed' AND setting_value = 'true'",
                    Integer.class
            );
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("IDENTITY_BOOTSTRAP | Failed to query system_settings table. It might not be created yet: {}", e.getMessage());
            return false;
        }
    }

    private void markBootstrapCompleted() {
        try {
            jdbcTemplate.update(
                    "INSERT INTO system_settings (setting_key, setting_value) VALUES ('bootstrap.completed', 'true')"
            );
            log.info("IDENTITY_BOOTSTRAP | Bootstrap state permanently locked in system_settings table.");
        } catch (Exception e) {
            log.error("IDENTITY_BOOTSTRAP | Failed to persist bootstrap state in system_settings: {}", e.getMessage());
            throw new RuntimeException("Bootstrap state lock failed", e);
        }
    }

    private void publishAudit(Long userId, String email, com.redis.audit.entity.AuditActionType action, com.redis.audit.entity.AuditStatus status, String desc) {
        if (auditEventPublisher != null) {
            try {
                String version = "v1.0.0";
                String host;
                try {
                    host = java.net.InetAddress.getLocalHost().getHostName();
                } catch (Exception ex) {
                    host = "unknown";
                }
                String env = System.getProperty("spring.profiles.active", "prod");
                String fullDesc = String.format("%s | Version: %s | Host: %s | Env: %s", desc, version, host, env);
                auditEventPublisher.publish(userId, email, action, status, com.redis.common.entity.ResourceType.USER, userId != null ? String.valueOf(userId) : "0", fullDesc);
            } catch (Exception e) {
                log.error("Failed to publish audit event for identity bootstrap: {}", e.getMessage());
            }
        }
    }

    private void failBootstrap(String email, String reason) {
        publishAudit(null, email, com.redis.audit.entity.AuditActionType.IDENTITY_BOOTSTRAP_FAILED, com.redis.audit.entity.AuditStatus.FAILED, "Bootstrap Failed: " + reason);
        throw new IllegalArgumentException("Identity Bootstrap failed: " + reason);
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

    protected String getEnv(String name) {
        return System.getenv(name);
    }
}
