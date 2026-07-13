package com.redis.product.repository;

import com.redis.product.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ProductRepository JPA Tests")
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager entityManager;

    private Product prod1;
    private Product prod2;
    private Product prod3;

    @BeforeEach
    void setUp() {
        // Prepare test data and persist it using H2 database
        prod1 = Product.builder()
                .name("MacBook Pro")
                .price(new BigDecimal("120000.00"))
                .rating(new BigDecimal("4.8"))
                .stockQuantity(15)
                .build();

        prod2 = Product.builder()
                .name("MacBook Air")
                .price(new BigDecimal("85000.00"))
                .rating(new BigDecimal("4.5"))
                .stockQuantity(5) // Low stock (<= 10)
                .build();

        prod3 = Product.builder()
                .name("iPad Pro")
                .price(new BigDecimal("70000.00"))
                .rating(new BigDecimal("4.7"))
                .stockQuantity(0) // Out of stock
                .build();

        productRepository.save(prod1);
        productRepository.save(prod2);
        productRepository.save(prod3);
    }

    @Test
    @DisplayName("✅ Success: Should find product by ID")
    void saveAndFindById() {
        Optional<Product> found = productRepository.findById(prod1.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("MacBook Pro");
    }

    @Test
    @DisplayName("✅ Success: Should perform case-insensitive partial search on name")
    void findByNameContainingIgnoreCase() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByNameContainingIgnoreCase("macbook", pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting("name")
                .containsExactlyInAnyOrder("MacBook Pro", "MacBook Air");
    }

    @Test
    @DisplayName("✅ Success: Should find product by exact case-insensitive name")
    void findByNameIgnoreCase() {
        Optional<Product> found = productRepository.findByNameIgnoreCase("macbook pro");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(prod1.getId());
    }

    @Test
    @DisplayName("✅ Success: Should filter by price range and sort ascending by price")
    void findByPriceBetweenOrderByPriceAsc() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> result = productRepository.findByPriceBetweenOrderByPriceAsc(
                new BigDecimal("60000.00"), new BigDecimal("90000.00"), pageable);

        // iPad Pro (70000) and MacBook Air (85000) should match, sorted ascending
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getName()).isEqualTo("iPad Pro");
        assertThat(result.getContent().get(1).getName()).isEqualTo("MacBook Air");
    }

    @Test
    @DisplayName("✅ Success: Should find products by minimum rating sorted descending")
    void findProductsByMinRating() {
        // Min rating 4.6 should return MacBook Pro (4.8) and iPad Pro (4.7) sorted desc
        List<Product> result = productRepository.findProductsByMinRating(new BigDecimal("4.6"));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("MacBook Pro");
        assertThat(result.get(1).getName()).isEqualTo("iPad Pro");
    }

    @Test
    @DisplayName("✅ Success: Should find affordable products in stock")
    void findAffordableProductsInStock() {
        // Under 90,000.00 and stock > 0. MacBook Air (85000) matches. iPad Pro has stock 0, so excluded.
        List<Product> result = productRepository.findAffordableProductsInStock(new BigDecimal("90000.00"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("MacBook Air");
    }

    @Test
    @DisplayName("✅ Success: Should find low stock products below threshold sorted ascending")
    void findLowStockProducts() {
        // Threshold 10. MacBook Air (5 stock) matches. iPad Pro has 0, so excluded.
        List<Product> result = productRepository.findLowStockProducts(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("MacBook Air");
    }

    @Test
    @DisplayName("✅ Success: Should find out of stock products")
    void findOutOfStockProducts() {
        // iPad Pro has stock = 0
        List<Product> result = productRepository.findOutOfStockProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("iPad Pro");
    }

    @Test
    @DisplayName("✅ Success: Should count products matching name but excluding current ID")
    void countByNameIgnoreCaseAndIdNot() {
        // Count duplicate name for "MacBook Pro" excluding prod1. Should be 0.
        long countSameId = productRepository.countByNameIgnoreCaseAndIdNot("MacBook Pro", prod1.getId());
        assertThat(countSameId).isZero();

        // Count duplicate name for "MacBook Pro" excluding prod2. Should be 1 (matches prod1).
        long countDiffId = productRepository.countByNameIgnoreCaseAndIdNot("MacBook Pro", prod2.getId());
        assertThat(countDiffId).isEqualTo(1L);
    }

    @Test
    @DisplayName("✅ Success: Should update stock directly in DB using modifying query")
    void updateStock() {
        int updatedRows = productRepository.updateStock(prod1.getId(), 40);
        assertThat(updatedRows).isEqualTo(1);

        // Clear persistence context to bypass first-level cache and force DB select
        entityManager.clear();

        // Fetch again to verify changes.
        // Note: JPA transaction rollback will auto reset database state after test method terminates.
        Optional<Product> updatedProduct = productRepository.findById(prod1.getId());
        assertThat(updatedProduct).isPresent();
        assertThat(updatedProduct.get().getStockQuantity()).isEqualTo(40);
    }
}
