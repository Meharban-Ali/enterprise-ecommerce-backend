package com.redis.category.entity;

import com.redis.product.entity.Product;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "categories",
    indexes = {
        @Index(name = "idx_categories_name", columnList = "name", unique = true)
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"products", "createdAt", "updatedAt"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    // ─── Primary Key ────────────────────────────────────────────────────────────
    

    // ─── Business Fields ────────────────────────────────────────────────────────
    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    // ─── Bidirectional Relationship ─────────────────────────────────────────────
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    // ─── Audit Fields ───────────────────────────────────────────────────────────
    

    
}