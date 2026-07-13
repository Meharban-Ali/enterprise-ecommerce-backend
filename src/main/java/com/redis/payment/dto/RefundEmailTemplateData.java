package com.redis.payment.dto;

import com.redis.notification.dto.NotificationTemplateData;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@SuperBuilder
public class RefundEmailTemplateData extends NotificationTemplateData {
    private final String customerName;
    private final Long paymentId;
    private final BigDecimal refundAmount;
}
