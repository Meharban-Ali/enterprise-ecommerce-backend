// ─────────────────────────────────────────────────────────────
// ProductService.java
// ─────────────────────────────────────────────────────────────
package com.redis.product.service;

import com.redis.product.dto.request.ProductRequest;
import com.redis.product.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    // ═══════════════════════════════════════════════════════════════════════════
    //  CRUD OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Creates a new product with duplicate name validation. */
    ProductResponse createProduct(ProductRequest request);

    /** Fetches a single product by ID. Cached via @Cacheable in Redis. */
    ProductResponse getProductById(Long id);

    /** Retrieves all products with pagination support. Page-wise cache enabled. */
    Page<ProductResponse> getAllProducts(Pageable pageable);

    /** Updates an existing product. Cache is refreshed via @CachePut. */
    ProductResponse updateProduct(Long id, ProductRequest request);

    /** Deletes a product and evicts it from cache. */
    void deleteProduct(Long id);

    // ═══════════════════════════════════════════════════════════════════════════
    //  SEARCH OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Searches products by name substring (case-insensitive, paginated). */
    Page<ProductResponse> searchProductsByName(String name, Pageable pageable);

    /** Retrieves products filtered within a price range. */
    Page<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    /** Fetches products with a rating greater than or equal to the minimum rating. */
    List<ProductResponse> getProductsByMinRating(BigDecimal minRating);

    /** Retrieves products with stock below a threshold (inventory management). */
    List<ProductResponse> getLowStockProducts(int threshold);

    /** Retrieves all out-of-stock products. */
    List<ProductResponse> getOutOfStockProducts();

    // ═══════════════════════════════════════════════════════════════════════════
    //  CACHE OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Manually clears all product caches. Returns true if successful. */
    boolean clearProductCache();
}
