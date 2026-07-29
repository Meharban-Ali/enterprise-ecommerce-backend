package com.redis.category.repository;

import com.redis.product.entity.Product;

import com.redis.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.redis.category.dto.response.CategorySummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Single-query category listing with pre-aggregated product counts (resolves N+1 query issue). */
    @Query("SELECT c.id AS id, c.name AS name, c.description AS description, COUNT(p) AS productCount, c.createdAt AS createdAt, c.updatedAt AS updatedAt FROM Category c LEFT JOIN c.products p GROUP BY c.id, c.name, c.description, c.createdAt, c.updatedAt")
    List<CategorySummaryProjection> findAllCategorySummaries();

    /** Paginated category listing with pre-aggregated product counts. */
    @Query("SELECT c.id AS id, c.name AS name, c.description AS description, COUNT(p) AS productCount, c.createdAt AS createdAt, c.updatedAt AS updatedAt FROM Category c LEFT JOIN c.products p GROUP BY c.id, c.name, c.description, c.createdAt, c.updatedAt")
    Page<CategorySummaryProjection> findAllCategorySummaries(Pageable pageable);

    /** Exact name match for duplicate validation (case-insensitive). */
    Optional<Category> findByNameIgnoreCase(String name);

    /** Count of products that belong to a specific category. */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countProductsByCategoryId(@Param("categoryId") Long categoryId);

    /** Check existence by name (excluding specific ID — useful for update validation). */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.id <> :excludeId")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("excludeId") Long excludeId);
}
