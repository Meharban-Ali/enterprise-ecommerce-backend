package com.redis.cart.entity;

import com.redis.product.entity.Product;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(
    name = "cart_items",
    indexes = {
        @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"),
        @Index(name = "idx_cart_items_product_id", columnList = "product_id")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CartItem extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    // ─── Cart Association ────────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    // ─── Product Association ─────────────────────────────────────────────────────
    @NotNull(message = "Product is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // ─── Quantity ────────────────────────────────────────────────────────────────
    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}