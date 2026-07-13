package com.redis.cart.entity;

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
    name = "carts",
    indexes = {
        @Index(name = "idx_carts_user_id", columnList = "user_id", unique = true)
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"items", "user"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cart extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    // ─── One Cart per User ───────────────────────────────────────────────────────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ─── Cart Items ──────────────────────────────────────────────────────────────
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    // ─── Audit Fields ───────────────────────────────────────────────────────────
    

    
}