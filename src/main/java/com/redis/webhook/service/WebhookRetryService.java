package com.redis.webhook.service;

import com.redis.webhook.entity.WebhookDelivery;

public interface WebhookRetryService {
    void scheduleRetry(WebhookDelivery delivery, String reason);
}
