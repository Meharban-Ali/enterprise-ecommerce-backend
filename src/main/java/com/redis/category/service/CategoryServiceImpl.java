package com.redis.category.service;

import com.redis.audit.entity.AuditActionType;
import com.redis.common.entity.ResourceType;
import com.redis.audit.entity.AuditStatus;
import com.redis.audit.event.AuditEventPublisher;

import com.redis.category.dto.request.CategoryRequest;
import com.redis.category.dto.response.CategoryResponse;
import com.redis.category.entity.Category;
import com.redis.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import com.redis.infrastructure.config.RedisCacheConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = RedisCacheConfig.CACHE_CATEGORY, allEntries = true),
        @CacheEvict(value = RedisCacheConfig.CACHE_CATEGORIES, allEntries = true)
    })
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category: {}", request.getName());

        categoryRepository.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
        });

        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .createdBy("system")
                .updatedBy("system")
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .build();


        Category saved = categoryRepository.save(category);
        log.info("Category created — id: {}", saved.getId());

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(null, null, com.redis.audit.entity.AuditActionType.CATEGORY_CREATED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.CATEGORY, String.valueOf(saved.getId()), "Category created: " + saved.getName());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisCacheConfig.CACHE_CATEGORY, key = "#id")
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));
        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisCacheConfig.CACHE_CATEGORIES, key = "'page_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort")
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findAllCategorySummaries(pageable).map(proj -> CategoryResponse.builder()
                .id(proj.getId())
                .name(proj.getName())
                .description(proj.getDescription())
                .productCount(proj.getProductCount())
                .createdAt(proj.getCreatedAt())
                .updatedAt(proj.getUpdatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = RedisCacheConfig.CACHE_CATEGORIES, key = "'list_all'")
    public List<CategoryResponse> getAllCategoriesList() {
        return categoryRepository.findAllCategorySummaries().stream()
                .map(proj -> CategoryResponse.builder()
                        .id(proj.getId())
                        .name(proj.getName())
                        .description(proj.getDescription())
                        .productCount(proj.getProductCount())
                        .createdAt(proj.getCreatedAt())
                        .updatedAt(proj.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = RedisCacheConfig.CACHE_CATEGORY, allEntries = true),
        @CacheEvict(value = RedisCacheConfig.CACHE_CATEGORIES, allEntries = true)
    })
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));

        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());

        Category updated = categoryRepository.save(category);

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(null, null, com.redis.audit.entity.AuditActionType.CATEGORY_UPDATED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.CATEGORY, String.valueOf(updated.getId()), "Category updated: " + updated.getName());
        }

        return toResponse(updated);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = RedisCacheConfig.CACHE_CATEGORY, allEntries = true),
        @CacheEvict(value = RedisCacheConfig.CACHE_CATEGORIES, allEntries = true)
    })
    public void deleteCategory(Long id) {
        log.info("Deleting category id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));

        long productCount = categoryRepository.countProductsByCategoryId(id);
        if (productCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot delete category '" + category.getName() + "' — it has " + productCount + " linked product(s)."
            );
        }

        categoryRepository.delete(category);
        log.info("Category deleted — id: {}", id);

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(null, null, com.redis.audit.entity.AuditActionType.CATEGORY_DELETED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.CATEGORY, String.valueOf(id), "Category deleted: ID " + id);
        }
    }

    private CategoryResponse toResponse(Category category) {
        long productCount = category.getProducts() != null ? category.getProducts().size() : 0;
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .productCount(productCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
