package com.redis.order.entity;

import com.redis.product.entity.Product;

import com.redis.common.base.AuditableEntity;

import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(
    name = "order_items",
    indexes = {
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OrderItem extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    // ─── Order Association ───────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // ─── Product Snapshot ────────────────────────────────────────────────────────
    // Snapshot of product at time of order (product can change later)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName; // Snapshot for historical consistency

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Price at time of purchase (not current price)
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal; // quantity * unitPrice
}