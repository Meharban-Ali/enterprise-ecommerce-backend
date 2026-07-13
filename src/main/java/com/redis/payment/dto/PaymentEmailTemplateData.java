package com.redis.payment.dto;

import com.redis.notification.dto.NotificationTemplateData;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@SuperBuilder
public class PaymentEmailTemplateData extends NotificationTemplateData {
    private final String customerName;
    private final Long orderId;
    private final BigDecimal paymentAmount;
    private final String paymentGateway;
}
