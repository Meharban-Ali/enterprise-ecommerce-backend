package com.redis.webhook.service;

import com.redis.webhook.entity.WebhookDelivery;
import com.redis.webhook.entity.WebhookEndpoint;
import com.redis.webhook.entity.WebhookStatus;
import com.redis.webhook.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookRetryServiceImpl implements WebhookRetryService {

    private final WebhookDeliveryRepository deliveryRepository;

    @Override
    @Transactional
    public void scheduleRetry(WebhookDelivery delivery, String reason) {
        WebhookEndpoint endpoint = delivery.getWebhookEndpoint();
        int maxRetries = endpoint.isRetryEnabled() ? endpoint.getMaxRetryCount() : 0;

        delivery.setRetryCount(delivery.getRetryCount() + 1);
        delivery.setFailureReason(reason);

        if (delivery.getRetryCount() > maxRetries) {
            delivery.setDeliveryStatus(WebhookStatus.DEAD_LETTER);
            log.warn("Webhook delivery ID {} exhausted all retries ({}). Moving to DLQ.", delivery.getId(), maxRetries);
        } else {
            delivery.setDeliveryStatus(WebhookStatus.RETRYING);
            log.info("Scheduling retry attempt {} for webhook delivery ID: {}", delivery.getRetryCount(), delivery.getId());
        }

        deliveryRepository.save(delivery);
    }
}
