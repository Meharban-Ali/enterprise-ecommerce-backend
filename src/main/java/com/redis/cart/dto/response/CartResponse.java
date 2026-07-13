package com.redis.cart.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CartResponse {
    private Long cartId;
    private Long userId;
    private List<CartItemResponse> items;
    private int itemCount;
    private int totalItems;
    private int totalQuantity;
    private BigDecimal totalAmount;
    private BigDecimal subtotal;
    private BigDecimal grandTotal;

    // Future extension placeholders
    private BigDecimal discount;
    private BigDecimal shippingCharge;
    private BigDecimal tax;
    private BigDecimal couponDiscount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
