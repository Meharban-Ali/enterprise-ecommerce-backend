package com.redis.common.entity;

public enum ResourceType {
    USER,
    ORDER,
    PAYMENT,
    PRODUCT,
    CATEGORY,
    INVENTORY,
    NOTIFICATION,
    ROLE,
    SYSTEM,

    // Sprint 9.2 resources
    ALERT_RULE,
    INCIDENT,
    BACKUP,
    RESTORE,
    FEATURE_FLAG,
    CONFIGURATION
}
