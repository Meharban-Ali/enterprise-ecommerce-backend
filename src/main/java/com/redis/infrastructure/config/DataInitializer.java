package com.redis.infrastructure.config;

import com.redis.category.entity.Category;
import com.redis.category.repository.CategoryRepository;
import com.redis.product.entity.Product;
import com.redis.product.repository.ProductRepository;
import com.redis.user.entity.Role;
import com.redis.user.entity.User;
import com.redis.user.repository.UserRepository;
import com.redis.monitoring.entity.AlertRule;
import com.redis.monitoring.entity.AlertSeverity;
import com.redis.monitoring.entity.AlertSource;
import com.redis.monitoring.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataInitializerProperties properties;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
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

        try {
            initializeCategoriesAndProducts();
        } catch (Exception ex) {
            log.error("Failed to initialize sample category and product data: {}", ex.getMessage(), ex);
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
    }

    private boolean isBootstrapCompleted() {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM system_settings WHERE setting_key = 'bootstrap.completed' AND setting_value = 'true'",
                    Integer.class
            );
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void markBootstrapCompleted() {
        try {
            jdbcTemplate.update(
                    "INSERT INTO system_settings (setting_key, setting_value) VALUES ('bootstrap.completed', 'true')"
            );
        } catch (Exception e) {
            log.error("Failed to mark bootstrap completed: {}", e.getMessage());
        }
    }

    @Transactional
    public void initializeAlertRules() {
        seedAlertRule("DB_DOWN", "Database Connection Down", AlertSource.DATABASE, AlertSeverity.CRITICAL, 0.0, 10, 60);
        seedAlertRule("REDIS_DOWN", "Redis Cache Down", AlertSource.REDIS, AlertSeverity.HIGH, 0.0, 10, 60);
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
        }
    }

    @Transactional
    public void initializeCategoriesAndProducts() {
        long prodCount = productRepository.count();

        if (prodCount > 0) {
            log.info("Database already has {} product(s) — skipping seed", prodCount);
            return;
        }

        log.info("Seeding initial categories and products into database...");

        // 1. Categories
        Map<String, Category> categoryMap = new HashMap<>();

        List<Category> categoriesToSeed = List.of(
            Category.builder().name("Smartphones & Tablets").description("Latest flagship smartphones, mobile devices and accessories").build(),
            Category.builder().name("Computers & Laptops").description("High-performance gaming laptops, desktop PCs and peripherals").build(),
            Category.builder().name("Audio & Accessories").description("Premium noise-canceling headphones, wireless earbuds and speakers").build(),
            Category.builder().name("Electronics & Gadgets").description("High-tech devices, smart monitors, and entertainment systems").build(),
            Category.builder().name("Home & Appliances").description("Modern smart home equipment, coffee makers and daily appliances").build()
        );

        for (Category cat : categoriesToSeed) {
            Category saved = categoryRepository.findByNameIgnoreCase(cat.getName())
                    .orElseGet(() -> categoryRepository.save(cat));
            categoryMap.put(saved.getName(), saved);
        }

        log.info("{} categories seeded or verified.", categoryMap.size());

        // 2. Products
        Category catSmartphones = categoryMap.get("Smartphones & Tablets");
        Category catComputers = categoryMap.get("Computers & Laptops");
        Category catAudio = categoryMap.get("Audio & Accessories");
        Category catElectronics = categoryMap.get("Electronics & Gadgets");
        Category catHome = categoryMap.get("Home & Appliances");

        List<Product> sampleProducts = List.of(
            Product.builder()
                .name("iPhone 15 Pro Max 256GB")
                .price(new BigDecimal("134900.00"))
                .rating(new BigDecimal("4.8"))
                .stockQuantity(150)
                .category(catSmartphones)
                .build(),
            Product.builder()
                .name("Samsung Galaxy S24 Ultra")
                .price(new BigDecimal("129999.00"))
                .rating(new BigDecimal("4.7"))
                .stockQuantity(120)
                .category(catSmartphones)
                .build(),
            Product.builder()
                .name("ASUS ROG Strix Gaming Laptop")
                .price(new BigDecimal("145000.00"))
                .rating(new BigDecimal("4.9"))
                .stockQuantity(45)
                .category(catComputers)
                .build(),
            Product.builder()
                .name("Sony WH-1000XM5 ANC Headphones")
                .price(new BigDecimal("29990.00"))
                .rating(new BigDecimal("4.8"))
                .stockQuantity(200)
                .category(catAudio)
                .build(),
            Product.builder()
                .name("Apple MacBook Air M3 16GB")
                .price(new BigDecimal("114900.00"))
                .rating(new BigDecimal("4.9"))
                .stockQuantity(80)
                .category(catComputers)
                .build(),
            Product.builder()
                .name("Dell UltraSharp 27 inch 4K USB-C Monitor")
                .price(new BigDecimal("54990.00"))
                .rating(new BigDecimal("4.6"))
                .stockQuantity(60)
                .category(catElectronics)
                .build(),
            Product.builder()
                .name("Bose QuietComfort Ultra Earbuds")
                .price(new BigDecimal("24900.00"))
                .rating(new BigDecimal("4.7"))
                .stockQuantity(110)
                .category(catAudio)
                .build(),
            Product.builder()
                .name("iPad Air M2 11-inch")
                .price(new BigDecimal("59900.00"))
                .rating(new BigDecimal("4.8"))
                .stockQuantity(95)
                .category(catSmartphones)
                .build(),
            Product.builder()
                .name("Dyson V15 Detect Vacuum Cleaner")
                .price(new BigDecimal("62900.00"))
                .rating(new BigDecimal("4.6"))
                .stockQuantity(30)
                .category(catHome)
                .build(),
            Product.builder()
                .name("Nespresso Vertuo Pop Coffee Machine")
                .price(new BigDecimal("16990.00"))
                .rating(new BigDecimal("4.5"))
                .stockQuantity(85)
                .category(catHome)
                .build()
        );

        List<Product> saved = productRepository.saveAll(sampleProducts);
        log.info("{} sample products seeded successfully", saved.size());
    }

    protected String getEnv(String name) {
        return System.getenv(name);
    }
}
