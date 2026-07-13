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
public class RefundResponse {
    private Long id;
    private BigDecimal amount;
    private String status;
    private String reason;
    private String gatewayReferenceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
