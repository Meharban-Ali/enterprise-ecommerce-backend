package com.redis.common.controller;

import com.redis.category.entity.Category;
import com.redis.category.repository.CategoryRepository;
import com.redis.common.dto.ApiResponse;
import com.redis.product.entity.Product;
import com.redis.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/seed")
@RequiredArgsConstructor
public class DataSeedController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> seedData() {
        log.info("API POST /api/seed — Force data seeding requested");

        // 1. Seed Categories
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

        // 2. Seed Products
        Category catSmartphones = categoryMap.get("Smartphones & Tablets");
        Category catComputers = categoryMap.get("Computers & Laptops");
        Category catAudio = categoryMap.get("Audio & Accessories");
        Category catElectronics = categoryMap.get("Electronics & Gadgets");
        Category catHome = categoryMap.get("Home & Appliances");

        List<Product> products = List.of(
            Product.builder().name("iPhone 15 Pro Max 256GB").price(new BigDecimal("134900.00")).rating(new BigDecimal("4.8")).stockQuantity(150).category(catSmartphones).build(),
            Product.builder().name("Samsung Galaxy S24 Ultra").price(new BigDecimal("129999.00")).rating(new BigDecimal("4.7")).stockQuantity(120).category(catSmartphones).build(),
            Product.builder().name("ASUS ROG Strix Gaming Laptop").price(new BigDecimal("145000.00")).rating(new BigDecimal("4.9")).stockQuantity(45).category(catComputers).build(),
            Product.builder().name("Sony WH-1000XM5 ANC Headphones").price(new BigDecimal("29990.00")).rating(new BigDecimal("4.8")).stockQuantity(200).category(catAudio).build(),
            Product.builder().name("Apple MacBook Air M3 16GB").price(new BigDecimal("114900.00")).rating(new BigDecimal("4.9")).stockQuantity(80).category(catComputers).build(),
            Product.builder().name("Dell UltraSharp 27 inch 4K USB-C Monitor").price(new BigDecimal("54990.00")).rating(new BigDecimal("4.6")).stockQuantity(60).category(catElectronics).build(),
            Product.builder().name("Bose QuietComfort Ultra Earbuds").price(new BigDecimal("24900.00")).rating(new BigDecimal("4.7")).stockQuantity(110).category(catAudio).build(),
            Product.builder().name("iPad Air M2 11-inch").price(new BigDecimal("59900.00")).rating(new BigDecimal("4.8")).stockQuantity(95).category(catSmartphones).build(),
            Product.builder().name("Dyson V15 Detect Vacuum Cleaner").price(new BigDecimal("62900.00")).rating(new BigDecimal("4.6")).stockQuantity(30).category(catHome).build(),
            Product.builder().name("Nespresso Vertuo Pop Coffee Machine").price(new BigDecimal("16990.00")).rating(new BigDecimal("4.5")).stockQuantity(85).category(catHome).build()
        );

        int count = 0;
        for (Product p : products) {
            if (productRepository.findByNameIgnoreCase(p.getName()).isEmpty()) {
                productRepository.save(p);
                count++;
            }
        }

        log.info("Force seeding completed: {} new products and {} categories saved", count, categoryMap.size());

        return ResponseEntity.ok(ApiResponse.success("Seeded " + count + " new products and " + categoryMap.size() + " categories successfully", "SUCCESS"));
    }
}
