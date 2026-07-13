package com.redis.notification.entity;

import lombok.Getter;

@Getter
public enum NotificationTemplate {
    WELCOME("welcome"),
    PASSWORD_RESET("password_reset"),
    ORDER_PLACED("order_placed"),
    PAYMENT_SUCCESS("payment_success"),
    PAYMENT_FAILED("payment_failed"),
    ORDER_SHIPPED("order_shipped"),
    ORDER_DELIVERED("order_delivered"),
    REFUND_SUCCESS("refund_success");

    private final String templateName;

    NotificationTemplate(String templateName) {
        this.templateName = templateName;
    }
}
