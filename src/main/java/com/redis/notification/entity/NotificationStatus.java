package com.redis.notification.entity;

public enum NotificationStatus {
    PENDING,
    DELIVERING,
    SENT,
    FAILED,
    RETRYING,
    DEAD_LETTER,
    SKIPPED
}
