package com.redis.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryResponse {
    private Long paymentId;
    private Long orderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String gateway;
}
