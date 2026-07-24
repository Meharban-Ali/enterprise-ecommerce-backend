package com.redis.payment.entity;

import com.redis.common.base.AuditableEntity;

import com.redis.payment.entity.PaymentTransactionType;
import jakarta.persistence.*;
import lombok.experimental.SuperBuilder;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "payment_transactions",
    indexes = {
        @Index(name = "idx_transactions_payment_id", columnList = "payment_id"),
        @Index(name = "idx_transactions_gateway_ref", columnList = "gateway_reference_id")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"payment"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PaymentTransaction extends AuditableEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "gateway_reference_id", length = 255)
    private String gatewayReferenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30, columnDefinition = "varchar(30)")
    private PaymentTransactionType type;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "gateway_response", length = 4000)
    private String gatewayResponse;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "webhook_id", length = 255)
    private String webhookId;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    
}