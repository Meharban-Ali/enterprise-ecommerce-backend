// ─────────────────────────────────────────────────────────────
// ProductController.java — Production Ready (Secured)
// ─────────────────────────────────────────────────────────────
package com.redis.product.controller;

import com.redis.product.entity.Product;

import com.redis.product.dto.request.ProductRequest;
import com.redis.common.dto.ApiResponse;
import com.redis.product.dto.response.ProductResponse;
import com.redis.product.service.ProductService;
import com.redis.infrastructure.cache.RedisUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.ObjectProvider;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product Management API with Redis Caching and Spring Security")
public class ProductController {

    private final ProductService productService;
    private final ObjectProvider<RedisUtil> redisUtilProvider;

    // ═══════════════════════════════════════════════════════════════════════════
    //  CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // restricted to admins
    @Operation(summary = "Create a new product", description = "Creates a new product with validation (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        log.info("POST /api/products — creating product: {}", request.getName());
        ProductResponse created = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED) // 201
                .body(ApiResponse.success("Product created successfully", created));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  READ — Single
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')") // allowed for both users and admins
    @Operation(summary = "Get product by ID", description = "Fetches a product by ID with Redis cache support")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {

        log.info("GET /api/products/{} — fetching product", id);

        String cacheKey = "product::" + id;
        boolean inCache = isKeyInCache(cacheKey);

        ProductResponse product = productService.getProductById(id);

        String message = inCache
                ? "Product fetched from Redis cache"
                : "Product fetched from database";

        return ResponseEntity.ok(ApiResponse.success(message, product, inCache));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  READ — All (Paginated)
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get all products (paginated)", description = "Retrieves all products with pagination and sorting support")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "id")   String sort,
            @RequestParam(defaultValue = "asc")  String dir) {

        log.info("GET /api/products — page: {}, size: {}", page, size);

        Sort.Direction direction = dir.equalsIgnoreCase("desc")
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        boolean inCache = isKeyInCache("products::page_" + page + "_size_" + size);
        Page<ProductResponse> products = productService.getAllProducts(pageable);

        String message = inCache
                ? "Products fetched from Redis cache"
                : "Products fetched from database";

        return ResponseEntity.ok(ApiResponse.success(message, products, inCache));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // restricted to admins
    @Operation(summary = "Update product", description = "Updates an existing product and refreshes cache (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        log.info("PUT /api/products/{} — updating product", id);
        ProductResponse updated = productService.updateProduct(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Product updated and cache refreshed", updated));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')") // restricted to admins
    @Operation(summary = "Delete product", description = "Deletes a product and evicts from cache (Admin only)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product deleted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        log.info("DELETE /api/products/{} — deleting product", id);
        productService.deleteProduct(id);

        return ResponseEntity.ok(
                ApiResponse.success("Product deleted and cache evicted"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SEARCH
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Search products by name", description = "Searches products by name (case-insensitive, paginated)")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam String name,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/products/search — name: {}", name);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.searchProductsByName(name, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Search results for: " + name, products));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PRICE RANGE
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/price-range")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Filter products by price range", description = "Gets products within a specified price range")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("GET /api/products/price-range — min: {}, max: {}", minPrice, maxPrice);

        Pageable pageable = PageRequest.of(page, size);
        Page<ProductResponse> products = productService.getProductsByPriceRange(minPrice, maxPrice, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Products in price range", products));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RATING & STOCK
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/rating")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get products by minimum rating", description = "Filters products with a minimum rating")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getByMinRating(
            @RequestParam BigDecimal minRating) {

        log.info("GET /api/products/rating — minRating: {}", minRating);
        List<ProductResponse> products = productService.getProductsByMinRating(minRating);

        return ResponseEntity.ok(
                ApiResponse.success("Products with min rating: " + minRating, products));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')") // Admin-only: Inventory management operation
    @Operation(summary = "Get low stock products", description = "Retrieves products with stock below threshold (Admin only)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStock(
            @RequestParam(defaultValue = "10") int threshold) {

        log.info("GET /api/products/low-stock — threshold: {}", threshold);
        List<ProductResponse> products = productService.getLowStockProducts(threshold);

        return ResponseEntity.ok(
                ApiResponse.success("Low stock products (threshold: " + threshold + ")", products));
    }

    @GetMapping("/out-of-stock")
    @PreAuthorize("hasRole('ADMIN')") // Admin-only: Inventory management operation
    @Operation(summary = "Get out of stock products", description = "Retrieves all out of stock products (Admin only)")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getOutOfStock() {
        log.info("GET /api/products/out-of-stock");
        List<ProductResponse> products = productService.getOutOfStockProducts();

        return ResponseEntity.ok(
                ApiResponse.success("Out of stock products", products));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    @DeleteMapping("/cache")
    @PreAuthorize("hasRole('ADMIN')") // Admin-only: system maintenance
    @Operation(summary = "Clear all product cache", description = "Manually clears all product caches (Admin only)")
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        log.warn("DELETE /api/products/cache — manual cache clear requested");

        boolean cleared = productService.clearProductCache();

        if (!cleared) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to clear cache", "CACHE_CLEAR_FAILED"));
        }

        return ResponseEntity.ok(ApiResponse.success("All product caches cleared"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CACHE DEMO
    // ═══════════════════════════════════════════════════════════════════════════

    @GetMapping("/cache-demo/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Cache demo endpoint", description = "Demonstrates Redis cache hit/miss for a product")
    public ResponseEntity<ApiResponse<ProductResponse>> cacheDemoEndpoint(
            @PathVariable Long id) {

        log.info("GET /api/products/cache-demo/{}", id);

        String cacheKey       = "product::" + id;
        RedisUtil redisUtil   = redisUtilProvider.getIfAvailable();
        boolean beforeFetch   = redisUtil != null && redisUtil.exists(cacheKey);
        long    startTime     = System.currentTimeMillis();

        ProductResponse product = productService.getProductById(id);

        long    responseTime  = System.currentTimeMillis() - startTime;
        boolean afterFetch    = redisUtil != null && redisUtil.exists(cacheKey);

        String cacheStatus;
        if (redisUtil == null) {
            cacheStatus = "CACHE BYPASSED — Running in stand-alone H2/Simple Cache mode";
        } else {
            cacheStatus = beforeFetch
                    ? "CACHE HIT — Fetched from Redis cache"
                    : "CACHE MISS — Fetched from database and stored in Redis cache";
        }

        log.info("Cache demo — id: {}, hit: {}, time: {}ms", id, beforeFetch, responseTime, afterFetch);

        return ResponseEntity.ok(
                ApiResponse.success(cacheStatus, product, afterFetch));
    }

    private boolean isKeyInCache(String key) {
        RedisUtil redisUtil = redisUtilProvider.getIfAvailable();
        return redisUtil != null && redisUtil.exists(key);
    }
}
