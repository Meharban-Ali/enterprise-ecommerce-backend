package com.redis.webhook.entity;

public enum WebhookStatus {
    PENDING,
    PROCESSING,
    DELIVERED,
    FAILED,
    RETRYING,
    DEAD_LETTER,
    DISABLED
}
