package com.redis.cart.entity;

import com.redis.product.entity.Product;
import com.redis.user.entity.User;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
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
    name = "wishlists",
    indexes = {
        @Index(name = "idx_wishlists_user_id", columnList = "user_id", unique = true)
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"products", "user"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Wishlist extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    // ─── One Wishlist per User ──────────────────────────────────────────────────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ─── Wishlist Products (ManyToMany via join table) ──────────────────────────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "wishlist_products",
        joinColumns = @JoinColumn(name = "wishlist_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id"),
        indexes = {
            @Index(name = "idx_wp_wishlist_id", columnList = "wishlist_id"),
            @Index(name = "idx_wp_product_id", columnList = "product_id")
        }
    )
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    // ─── Audit Fields ───────────────────────────────────────────────────────────
    

    
}