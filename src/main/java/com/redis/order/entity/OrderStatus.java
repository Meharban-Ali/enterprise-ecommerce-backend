package com.redis.order.entity;

public enum OrderStatus {
    PENDING,            // Legacy pending status
    PENDING_PAYMENT,    // Order placed, awaiting payment session completion
    PAID,               // Payment confirmed
    PROCESSING,         // Being prepared for shipment
    SHIPPED,            // Dispatched to delivery partner
    DELIVERED,          // Successfully delivered to customer
    CANCELLED,          // Order cancelled (by user or admin)
    PAYMENT_FAILED,     // Payment failed during webhook or redirect callback
    EXPIRED             // Payment timed out after 15 minutes of inactivity
}
