package com.redis.product.repository;

import com.redis.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Override
    @EntityGraph(attributePaths = {"category"})
    Page<Product> findAll(Pageable pageable);

    // ═══════════════════════════════════════════════════════════════════════════
    //  SEARCH QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Search by name substring — case-insensitive, paginated. */
    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /** Exact name match — used for duplicate validation during creation. */
    Optional<Product> findByNameIgnoreCase(String name);

    /** Price range filter — results sorted by price ascending. */
    @EntityGraph(attributePaths = {"category"})
    Page<Product> findByPriceBetweenOrderByPriceAsc(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    // ═══════════════════════════════════════════════════════════════════════════
    //  RATING QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Fetches products with an exact rating value. */
    List<Product> findByRating(BigDecimal rating);

    /** Fetches products with rating >= minRating, ordered by rating descending. */
    @Query("SELECT p FROM Product p WHERE p.rating >= :minRating ORDER BY p.rating DESC")
    List<Product> findProductsByMinRating(@Param("minRating") BigDecimal minRating);

    // ═══════════════════════════════════════════════════════════════════════════
    //  STOCK QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Fetches in-stock products priced at or below the maximum price. */
    @Query("SELECT p FROM Product p WHERE p.price <= :maxPrice " +
           "AND p.stockQuantity > 0 " +
           "ORDER BY p.price ASC")
    List<Product> findAffordableProductsInStock(@Param("maxPrice") BigDecimal maxPrice);

    /** Fetches low-stock products (stock > 0 but <= threshold), ordered by stock ascending. */
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0 " +
           "AND p.stockQuantity <= :threshold " +
           "ORDER BY p.stockQuantity ASC")
    List<Product> findLowStockProducts(@Param("threshold") int threshold);

    /** Fetches all out-of-stock products (stockQuantity = 0). */
    @Query("SELECT p FROM Product p WHERE p.stockQuantity = 0")
    List<Product> findOutOfStockProducts();

    // ═══════════════════════════════════════════════════════════════════════════
    //  UTILITY QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Counts products with the same name (case-insensitive) excluding a specific ID — used for duplicate checks during updates. */
    @Query("SELECT COUNT(p) FROM Product p " +
           "WHERE LOWER(p.name) = LOWER(:name) " +
           "AND p.id <> :excludeId")
    long countByNameIgnoreCaseAndIdNot(
            @Param("name") String name,
            @Param("excludeId") Long excludeId
    );

    /** Bulk stock update — more performant than entity-level save for batch operations. */
    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stockQuantity = :stock WHERE p.id = :id")
    int updateStock(@Param("id") Long id, @Param("stock") int stock);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity = 0")
    long countOutOfStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity > 0 AND p.stockQuantity <= :threshold")
    long countLowStock(@Param("threshold") int threshold);

    @Query("SELECT AVG(p.rating) FROM Product p")
    Double getAverageRating();
}
