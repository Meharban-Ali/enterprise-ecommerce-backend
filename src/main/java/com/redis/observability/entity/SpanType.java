package com.redis.observability.entity;

public enum SpanType {
    HTTP_REQUEST,
    DATABASE,
    SERVICE,
    QUEUE,
    WEBHOOK,
    SCHEDULER,
    NOTIFICATION,
    PAYMENT,
    CACHE,
    SECURITY
}
