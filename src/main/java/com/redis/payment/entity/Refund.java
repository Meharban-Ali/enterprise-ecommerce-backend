package com.redis.payment.entity;

import com.redis.common.base.AuditableEntity;

import com.redis.payment.entity.RefundStatus;
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
    name = "refunds",
    indexes = {
        @Index(name = "idx_refunds_payment_id", columnList = "payment_id"),
        @Index(name = "idx_refunds_status", columnList = "status")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"payment"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Refund extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RefundStatus status;

    @Column(name = "reason", length = 1000)
    private String reason;

    @Column(name = "gateway_reference_id", length = 255)
    private String gatewayReferenceId;

    

    
}