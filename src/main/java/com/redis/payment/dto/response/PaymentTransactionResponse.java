package com.redis.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {
    private Long id;
    private String gatewayReferenceId;
    private String type;
    private BigDecimal amount;
    private String failureReason;
    private LocalDateTime createdAt;
}
