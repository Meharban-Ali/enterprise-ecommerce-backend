package com.redis.product.entity;

import com.redis.category.entity.Category;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_name",  columnList = "name"),
        @Index(name = "idx_products_price", columnList = "price"),
        @Index(name = "idx_products_rating", columnList = "rating"),
        @Index(name = "idx_products_stock_qty", columnList = "stock_quantity")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"createdAt", "updatedAt"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Product extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // ─── Primary Key ────────────────────────────────────────────────────────────
    

    // ─── Business Fields ────────────────────────────────────────────────────────
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "rating", nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    // ─── Category Relationship ──────────────────────────────────────────────────
    // Nullable — existing products without a category remain valid
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = true)
    private Category category;

    // ─── Optimistic Locking ─────────────────────────────────────────────────────
    // Prevents race conditions on concurrent writes (optimistic locking)
    

    // ─── Audit Fields ───────────────────────────────────────────────────────────
    // @PrePersist/@PreUpdate hatao — Hibernate annotations zyada reliable hain
    

    
}