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
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category: {}", request.getName());

        categoryRepository.findByNameIgnoreCase(request.getName()).ifPresent(existing -> {
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
        });

        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
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
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + id));
        return toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesList() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
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
