package com.redis.webhook.service;

import com.redis.webhook.dto.response.WebhookDeliveryResponse;
import com.redis.webhook.dto.response.WebhookResponse;
import com.redis.webhook.dto.request.WebhookRequest;
import com.redis.webhook.dto.response.WebhookTestResponse;
import com.redis.webhook.dto.response.WebhookSecretRotateResponse;

import com.redis.common.entity.IntegrationEventType;

import java.util.List;

public interface WebhookService {
    WebhookResponse registerWebhook(WebhookRequest request);
    WebhookResponse updateWebhook(Long id, WebhookRequest request);
    void deleteWebhook(Long id);
    WebhookResponse enableWebhook(Long id);
    WebhookResponse disableWebhook(Long id);
    WebhookTestResponse testWebhook(Long id);
    WebhookSecretRotateResponse rotateSecret(Long id);

    void publish(IntegrationEventType eventType, Object payload);
    void executeDelivery(Long notificationId);
    
    List<WebhookResponse> getEndpoints();
    List<WebhookDeliveryResponse> getDeliveries();
    List<WebhookDeliveryResponse> getDeadLetters();
}
