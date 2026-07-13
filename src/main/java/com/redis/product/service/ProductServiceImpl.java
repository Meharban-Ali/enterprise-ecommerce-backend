// ─────────────────────────────────────────────────────────────
// ProductServiceImpl.java — Production Ready
// ─────────────────────────────────────────────────────────────
package com.redis.product.service;

import com.redis.audit.entity.AuditActionType;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.audit.entity.AuditStatus;

import com.redis.infrastructure.config.RedisCacheConfig;
import com.redis.product.exception.ProductDuplicateException;
import com.redis.product.exception.ProductNotFoundException;
import com.redis.product.mapper.ProductMapper;
import com.redis.product.dto.request.ProductRequest;
import com.redis.product.dto.response.ProductResponse;
import com.redis.common.dto.RestPageImpl;
import com.redis.product.entity.Product;
import com.redis.category.entity.Category;
import com.redis.product.repository.ProductRepository;
import com.redis.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper     productMapper;      // DTO-entity mapper

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    // ═══════════════════════════════════════════════════════════════════════════
    //  CREATE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Creates a new product.
     * - Validates that a product with the same name does not already exist.
     * - Evicts all entries from the products cache after saving to keep cached list fresh.
     */
    @Override
    @Transactional
    @CacheEvict(
        value = RedisCacheConfig.CACHE_PRODUCTS, // FIX #7: constant reuse
        allEntries = true                         // sab pages invalidate
    )
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.getName());

        // Duplicate name check — ensure no product exists with the same name
        if (productRepository.findByNameIgnoreCase(request.getName()).isPresent()) {
            throw new ProductDuplicateException(request.getName());
        }

        // Map request DTO to entity using ProductMapper
        Product product = productMapper.toEntity(request);
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + request.getCategoryId()));
            product.setCategory(category);
        }
        Product saved = productRepository.save(product);
        log.info("Product created — id: {}", saved.getId());

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(null, null, com.redis.audit.entity.AuditActionType.PRODUCT_CREATED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.PRODUCT, String.valueOf(saved.getId()), "Product created: " + saved.getName());
        }

        return productMapper.toResponse(saved);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  READ — Single
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Fetches a single product by its ID.
     * - Cacheable: Reads from database on cache miss and writes the result to Redis cache.
     * - Condition: Enabled only if id is greater than 0.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value     = RedisCacheConfig.CACHE_PRODUCT,
        key       = "#id",
        condition = "#id > 0"
    )
    public ProductResponse getProductById(Long id) {
        log.info("DB hit — fetching product id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        log.info("Product found — name: {}", product.getName());
        return productMapper.toResponse(product);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  READ — All (Paginated)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Retrieves all products paginated.
     * - Uses page number and page size in cache key for dynamic caching of different pages.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(
        value = RedisCacheConfig.CACHE_PRODUCTS,
        key   = "'page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize"
    )
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        log.info("DB hit — fetching all products, page: {}", pageable.getPageNumber());

        Page<ProductResponse> result = productRepository
                .findAll(pageable)
                .map(productMapper::toResponse);

        log.info("Found {} products", result.getTotalElements());
        return new RestPageImpl<>(
            result.getContent(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements()
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  UPDATE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Updates an existing product.
     * - CachePut: Refreshes the specific product cache entry.
     * - CacheEvict: Invalidates the paginated products list cache to prevent stale data.
     */
    @Override
    @Transactional
    @Caching(
        put   = { @CachePut(value = RedisCacheConfig.CACHE_PRODUCT, key = "#id") },
        evict = { @CacheEvict(value = RedisCacheConfig.CACHE_PRODUCTS, allEntries = true) }
    )
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product — id: {}", id);

        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Duplicate check — allow updating with the same name for current product but block others
        boolean isDuplicate = productRepository
                .countByNameIgnoreCaseAndIdNot(request.getName(), id)>0;
        if (isDuplicate) {
            throw new ProductDuplicateException(request.getName());
        }

        // Map update request attributes to the existing product entity
        productMapper.updateEntity(existing, request);
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + request.getCategoryId()));
            existing.setCategory(category);
        } else {
            existing.setCategory(null);
        }
        Product updated = productRepository.save(existing);

        log.info("Product updated — id: {}", updated.getId());

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(null, null, com.redis.audit.entity.AuditActionType.PRODUCT_UPDATED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.PRODUCT, String.valueOf(updated.getId()), "Product updated: " + updated.getName());
        }

        return productMapper.toResponse(updated);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DELETE
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Deletes a product by ID.
     * - Evicts both the specific product and paginated products list from cache.
     */
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = RedisCacheConfig.CACHE_PRODUCT,  key = "#id"),
        @CacheEvict(value = RedisCacheConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public void deleteProduct(Long id) {
        log.info("Deleting product — id: {}", id);

        // Verify the product exists before attempting deletion
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        productRepository.deleteById(id);
        log.info("Product deleted — id: {}", id);

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(null, null, com.redis.audit.entity.AuditActionType.PRODUCT_DELETED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.PRODUCT, String.valueOf(id), "Product deleted: ID " + id);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SEARCH
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Searches products by name substring (case-insensitive).
     * Caching is skipped due to the highly dynamic nature of search queries.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProductsByName(String name, Pageable pageable) {
        log.info("Searching products by name: {}", name);

        Page<ProductResponse> page = productRepository
                .findByNameContainingIgnoreCase(name, pageable)
                .map(productMapper::toResponse);
            
            return new RestPageImpl<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
            );
    }

    /**
     * Retrieves products filtered within a price range.
     * Validates that minPrice is not greater than maxPrice.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByPriceRange(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        if (minPrice == null || maxPrice == null) {
            throw new IllegalArgumentException("Minimum price and maximum price must not be null");
        }

        // Validate that minPrice does not exceed maxPrice
        if (minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException(
                "Minimum price cannot be greater than maximum price"
            );
        }

        log.info("Fetching products — price range: {} to {}", minPrice, maxPrice);

        Page<ProductResponse> page = productRepository
                .findByPriceBetweenOrderByPriceAsc(minPrice, maxPrice, pageable)
                .map(productMapper::toResponse);

            return new RestPageImpl<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
            );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RATING & STOCK
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Fetches products with a rating greater than or equal to the minimum rating.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByMinRating(BigDecimal minRating) {
        if (minRating == null) {
            throw new IllegalArgumentException("Minimum rating must not be null");
        }
        if (minRating.compareTo(BigDecimal.ZERO) < 0 || minRating.compareTo(new BigDecimal("5.0")) > 0) {
            throw new IllegalArgumentException("Minimum rating must be between 0.0 and 5.0");
        }
        log.info("Fetching products with min rating: {}", minRating);

        return productMapper.toResponseList(
            productRepository.findProductsByMinRating(minRating)
        );
    }

    /**
     * Fetches low stock products below a threshold value.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(int threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Stock threshold cannot be negative");
        }
        log.info("Fetching low stock products — threshold: {}", threshold);

        return productMapper.toResponseList(
            productRepository.findLowStockProducts(threshold)
        );
    }

    /**
     * Fetches out-of-stock products where stockQuantity is 0.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getOutOfStockProducts() {
        log.info("Fetching out of stock products");

        return productMapper.toResponseList(
            productRepository.findOutOfStockProducts()
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CACHE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Clears all cached product items manually.
     * @return true if successful
     */
    @Override
    @Caching(evict = {
        @CacheEvict(value = RedisCacheConfig.CACHE_PRODUCT,  allEntries = true),
        @CacheEvict(value = RedisCacheConfig.CACHE_PRODUCTS, allEntries = true)
    })
    public boolean clearProductCache() {
        try {
            log.warn("All product caches cleared manually");
            return true;
        } catch (Exception ex) {
            log.error("Failed to clear cache: {}", ex.getMessage(), ex);
            return false;
        }
    }
}