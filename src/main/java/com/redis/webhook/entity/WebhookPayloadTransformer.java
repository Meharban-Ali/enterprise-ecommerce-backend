package com.redis.webhook.entity;

import com.redis.common.entity.IntegrationEventType;

public interface WebhookPayloadTransformer {
    String transform(IntegrationEventType eventType, Object payload, String version);
}
