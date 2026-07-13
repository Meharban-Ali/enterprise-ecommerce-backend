package com.redis.order.dto;

import com.redis.notification.dto.NotificationTemplateData;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.List;

@Getter
@SuperBuilder
public class OrderEmailTemplateData extends NotificationTemplateData {
    private final String customerName;
    private final Long orderId;
    private final BigDecimal totalAmount;
    private final List<String> productList;
}
