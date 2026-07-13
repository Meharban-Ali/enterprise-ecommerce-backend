package com.redis.category.service;

import com.redis.category.dto.request.CategoryRequest;
import com.redis.category.dto.response.CategoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CategoryService {

    /** Admin: Create a new category. */
    CategoryResponse createCategory(CategoryRequest request);

    /** Public: Get a single category by ID. */
    CategoryResponse getCategoryById(Long id);

    /** Public: Get all categories, paginated. */
    Page<CategoryResponse> getAllCategories(Pageable pageable);

    /** Public: Get all categories as a flat list. */
    List<CategoryResponse> getAllCategoriesList();

    /** Admin: Update a category. */
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    /** Admin: Delete a category (only if no products are linked). */
    void deleteCategory(Long id);
}
