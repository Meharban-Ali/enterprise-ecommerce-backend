package com.redis.category.dto.response;

import java.time.LocalDateTime;

/**
 * Spring Data JPA Projection for Category summaries with pre-aggregated product counts.
 * Eliminates N+1 lazy loading queries during category listings.
 */
public interface CategorySummaryProjection {
    Long getId();
    String getName();
    String getDescription();
    long getProductCount();
    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
