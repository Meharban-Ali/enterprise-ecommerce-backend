package com.redis.webhook.service;

import com.redis.notification.entity.NotificationChannel;
import com.redis.notification.entity.Notification;
import com.redis.webhook.entity.WebhookDelivery;
import com.redis.order.entity.Order;
import com.redis.common.entity.CircuitState;
import com.redis.incident.entity.Incident;
import com.redis.common.entity.IntegrationEventType;
import com.redis.webhook.repository.WebhookDeliveryRepository;
import com.redis.user.entity.User;
import com.redis.webhook.dto.request.WebhookRequest;
import com.redis.audit.entity.AuditStatus;
import com.redis.notification.entity.NotificationType;
import com.redis.user.repository.UserRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.webhook.entity.WebhookPayloadTransformer;
import com.redis.webhook.dto.response.WebhookSecretRotateResponse;
import com.redis.webhook.repository.WebhookEndpointRepository;
import com.redis.payment.entity.Payment;
import com.redis.webhook.dto.response.WebhookResponse;
import com.redis.notification.entity.NotificationStatus;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.webhook.entity.WebhookStatus;
import com.redis.webhook.entity.WebhookEndpoint;
import com.redis.webhook.dto.response.WebhookDeliveryResponse;
import com.redis.webhook.dto.response.WebhookTestResponse;
import com.redis.audit.entity.AuditActionType;
import com.redis.notification.entity.NotificationPriority;
import com.redis.reliability.service.PlatformResilienceService;
import com.redis.notification.service.NotificationQueueService;

import com.redis.common.util.HmacSignatureGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookServiceImpl implements WebhookService {

    private final WebhookEndpointRepository endpointRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebhookPayloadTransformer payloadTransformer;
    private final NotificationRepository notificationRepository;
    private final NotificationQueueService queueService;
    private final UserRepository userRepository;
    
    @Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    @Autowired(required = false)
    private PlatformResilienceService resilienceService;

    private final RestTemplate restTemplate = new RestTemplate(createRequestFactory());

    private static SimpleClientHttpRequestFactory createRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return factory;
    }

    private String getUsername() {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "anonymousUser";
    }

    private void audit(AuditActionType action, AuditStatus status, String resourceId, String desc) {
        if (auditEventPublisher != null) {
            try {
                String email = getUsername();
                User user = userRepository.findByEmail(email).orElse(null);
                Long userId = user != null ? user.getId() : null;
                auditEventPublisher.publish(userId, email, action, status, ResourceType.ALERT_RULE, resourceId, desc);
            } catch (Exception e) {
                log.error("Failed to publish audit event for webhook operation", e);
            }
        }
    }

    @Override
    @Transactional
    public WebhookResponse registerWebhook(WebhookRequest request) {
        if (endpointRepository.existsByTargetUrl(request.getTargetUrl())) {
            throw new IllegalArgumentException("Endpoint with target URL already exists: " + request.getTargetUrl());
        }

        WebhookEndpoint endpoint = WebhookEndpoint.builder()
                .name(request.getName())
                .targetUrl(request.getTargetUrl())
                .secretKey(request.getSecretKey())
                .enabled(request.isEnabled())
                .timeoutMs(request.getTimeoutMs() > 0 ? request.getTimeoutMs() : 5000)
                .retryEnabled(request.isRetryEnabled())
                .maxRetryCount(request.getMaxRetryCount())
                .subscribedEvents(request.getSubscribedEvents())
                .webhookVersion(request.getWebhookVersion() != null ? request.getWebhookVersion() : "v1")
                .filterPriority(request.getFilterPriority())
                .filterChannel(request.getFilterChannel())
                .filterSeverity(request.getFilterSeverity())
                .requestsPerMinute(request.getRequestsPerMinute())
                .requestsPerHour(request.getRequestsPerHour())
                .batchEnabled(request.isBatchEnabled())
                .batchSize(request.getBatchSize())
                .batchIntervalSeconds(request.getBatchIntervalSeconds())
                .compressionEnabled(request.isCompressionEnabled())
                .circuitState(CircuitState.CLOSED)
                .consecutiveFailures(0)
                .build();

        endpoint = endpointRepository.save(endpoint);
        audit(AuditActionType.valueOf("WEBHOOK_CREATED"), AuditStatus.SUCCESS, String.valueOf(endpoint.getId()), "Registered webhook: " + endpoint.getName());
        return mapToResponse(endpoint);
    }

    @Override
    @Transactional
    public WebhookResponse updateWebhook(Long id, WebhookRequest request) {
        WebhookEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook endpoint not found with ID: " + id));

        endpoint.setName(request.getName());
        endpoint.setTargetUrl(request.getTargetUrl());
        endpoint.setSecretKey(request.getSecretKey());
        endpoint.setEnabled(request.isEnabled());
        endpoint.setTimeoutMs(request.getTimeoutMs() > 0 ? request.getTimeoutMs() : 5000);
        endpoint.setRetryEnabled(request.isRetryEnabled());
        endpoint.setMaxRetryCount(request.getMaxRetryCount());
        endpoint.setSubscribedEvents(request.getSubscribedEvents());
        endpoint.setWebhookVersion(request.getWebhookVersion());
        endpoint.setFilterPriority(request.getFilterPriority());
        endpoint.setFilterChannel(request.getFilterChannel());
        endpoint.setFilterSeverity(request.getFilterSeverity());
        endpoint.setRequestsPerMinute(request.getRequestsPerMinute());
        endpoint.setRequestsPerHour(request.getRequestsPerHour());
        endpoint.setBatchEnabled(request.isBatchEnabled());
        endpoint.setBatchSize(request.getBatchSize());
        endpoint.setBatchIntervalSeconds(request.getBatchIntervalSeconds());
        endpoint.setCompressionEnabled(request.isCompressionEnabled());

        endpoint = endpointRepository.save(endpoint);
        audit(AuditActionType.valueOf("WEBHOOK_UPDATED"), AuditStatus.SUCCESS, String.valueOf(endpoint.getId()), "Updated webhook: " + endpoint.getName());
        return mapToResponse(endpoint);
    }

    @Override
    @Transactional
    public void deleteWebhook(Long id) {
        WebhookEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook endpoint not found with ID: " + id));
        endpointRepository.delete(endpoint);
        audit(AuditActionType.valueOf("WEBHOOK_DELETED"), AuditStatus.SUCCESS, String.valueOf(id), "Deleted webhook: " + endpoint.getName());
    }

    @Override
    @Transactional
    public WebhookResponse enableWebhook(Long id) {
        WebhookEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook endpoint not found with ID: " + id));
        endpoint.setEnabled(true);
        endpoint = endpointRepository.save(endpoint);
        audit(AuditActionType.valueOf("WEBHOOK_ENABLED"), AuditStatus.SUCCESS, String.valueOf(id), "Enabled webhook: " + endpoint.getName());
        return mapToResponse(endpoint);
    }

    @Override
    @Transactional
    public WebhookResponse disableWebhook(Long id) {
        WebhookEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook endpoint not found with ID: " + id));
        endpoint.setEnabled(false);
        endpoint = endpointRepository.save(endpoint);
        audit(AuditActionType.valueOf("WEBHOOK_DISABLED"), AuditStatus.SUCCESS, String.valueOf(id), "Disabled webhook: " + endpoint.getName());
        return mapToResponse(endpoint);
    }

    @Override
    @Transactional
    public WebhookSecretRotateResponse rotateSecret(Long id) {
        WebhookEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook endpoint not found with ID: " + id));

        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        String newSecret = Base64.getEncoder().encodeToString(randomBytes);
        endpoint.setSecretKey(newSecret);
        endpointRepository.save(endpoint);

        audit(AuditActionType.valueOf("WEBHOOK_SECRET_ROTATED"), AuditStatus.SUCCESS, String.valueOf(id), "Rotated secret for webhook: " + endpoint.getName());

        return WebhookSecretRotateResponse.builder()
                .endpointId(id)
                .name(endpoint.getName())
                .newSecretKey(newSecret)
                .build();
    }

    @Override
    @Transactional
    public WebhookTestResponse testWebhook(Long id) {
        WebhookEndpoint endpoint = endpointRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Webhook endpoint not found with ID: " + id));

        String payload = "{\"test\": true, \"message\": \"Ping event from E-Commerce framework\"}";
        long startTime = System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Version", endpoint.getWebhookVersion());
        long timestamp = System.currentTimeMillis() / 1000;
        headers.set("X-Signature", HmacSignatureGenerator.generateSignature(payload, endpoint.getSecretKey(), timestamp));
        headers.set("X-Timestamp", String.valueOf(timestamp));
        headers.set("X-Idempotency-Key", UUID.randomUUID().toString());

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response;
            if (resilienceService != null) {
                response = resilienceService.execute("webhooks",
                        () -> restTemplate.postForEntity(endpoint.getTargetUrl(), entity, String.class),
                        () -> {
                            throw new RuntimeException("Webhook target unavailable (resilience fallback)");
                        }
                );
            } else {
                response = restTemplate.postForEntity(endpoint.getTargetUrl(), entity, String.class);
            }
            long latency = System.currentTimeMillis() - startTime;
            audit(AuditActionType.valueOf("WEBHOOK_TESTED"), AuditStatus.SUCCESS, String.valueOf(id), "Tested webhook target successfully: " + endpoint.getName());
            return WebhookTestResponse.builder()
                    .success(response.getStatusCode().is2xxSuccessful())
                    .responseStatus(response.getStatusCode().value())
                    .responseBody(response.getBody())
                    .executionTimeMs(latency)
                    .build();
        } catch (Exception e) {
            audit(AuditActionType.valueOf("WEBHOOK_TESTED"), AuditStatus.FAILED, String.valueOf(id), "Tested webhook target failed: " + e.getMessage());
            return WebhookTestResponse.builder()
                    .success(false)
                    .responseStatus(500)
                    .error(e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    @Transactional
    public void publish(IntegrationEventType eventType, Object payload) {
        log.debug("Publishing Integration Event: {}", eventType);
        List<WebhookEndpoint> endpoints = endpointRepository.findByEventType(eventType);

        if (endpoints.isEmpty()) {
            return;
        }

        // Extract aggregate details for sequential processing order
        String aggType = payload != null ? payload.getClass().getSimpleName() : "SYSTEM";
        String aggId = "0";
        if (payload instanceof Order) aggId = String.valueOf(((Order) payload).getId());
        else if (payload instanceof Payment) aggId = String.valueOf(((Payment) payload).getId());
        else if (payload instanceof Incident) aggId = String.valueOf(((Incident) payload).getId());
        else if (payload instanceof Notification) aggId = String.valueOf(((Notification) payload).getId());

        for (WebhookEndpoint endpoint : endpoints) {
            // Apply event filters
            if (payload instanceof Notification) {
                Notification n = (Notification) payload;
                if (endpoint.getFilterPriority() != null && n.getPriority() != endpoint.getFilterPriority()) continue;
                if (endpoint.getFilterChannel() != null && n.getChannel() != endpoint.getFilterChannel()) continue;
            } else if (payload instanceof Incident) {
                Incident inc = (Incident) payload;
                if (endpoint.getFilterSeverity() != null && inc.getSeverity() != endpoint.getFilterSeverity()) continue;
            }

            try {
                String transformedPayload = payloadTransformer.transform(eventType, payload, endpoint.getWebhookVersion());
                String idempotencyKey = UUID.nameUUIDFromBytes((endpoint.getId() + ":" + eventType.name() + ":" + aggType + ":" + aggId + ":" + System.nanoTime()).getBytes(StandardCharsets.UTF_8)).toString();
                String corrId = MDC.get("CorrelationId");
                if (corrId == null) corrId = UUID.randomUUID().toString();

                WebhookDelivery delivery = WebhookDelivery.builder()
                        .webhookEndpoint(endpoint)
                        .eventType(eventType)
                        .payload(transformedPayload)
                        .deliveryStatus(WebhookStatus.PENDING)
                        .retryCount(0)
                        .correlationId(corrId)
                        .idempotencyKey(idempotencyKey)
                        .aggregateType(aggType)
                        .aggregateId(aggId)
                        .build();

                delivery = deliveryRepository.save(delivery);

                // Create a matching Notification with channel=WEBHOOK
                // Using a dummy user since webhook dispatches to external systems
                User systemUser = userRepository.findByEmail("superadmin@ecommerce.local").orElse(null);
                if (systemUser == null) {
                    // Fallback to first user
                    systemUser = userRepository.findAll().stream().findFirst().orElse(null);
                }

                if (systemUser != null) {
                    Notification n = Notification.builder()
                            .user(systemUser)
                            .title("Webhook: " + eventType.name())
                            .message(String.valueOf(delivery.getId())) // Store deliveryId in message
                            .type(NotificationType.SYSTEM)
                            .channel(NotificationChannel.WEBHOOK)
                            .priority(NotificationPriority.MEDIUM)
                            .status(NotificationStatus.PENDING)
                            .build();

                    n = notificationRepository.save(n);
                    queueService.enqueue(n.getId());
                }

            } catch (Exception e) {
                log.error("Failed to enqueue webhook delivery for endpoint: {}", endpoint.getName(), e);
            }
        }
    }

    @Override
    @Transactional
    public void executeDelivery(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null || notification.getChannel() != NotificationChannel.WEBHOOK) {
            return;
        }

        Long deliveryId;
        try {
            deliveryId = Long.parseLong(notification.getMessage());
        } catch (NumberFormatException e) {
            log.error("Invalid delivery ID stored in notification message: {}", notification.getMessage());
            return;
        }

        WebhookDelivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null) {
            return;
        }

        WebhookEndpoint endpoint = delivery.getWebhookEndpoint();
        
        // 1. Circuit Breaker validation
        if (endpoint.getCircuitState() == CircuitState.OPEN) {
            LocalDateTime lastFail = endpoint.getLastFailureTime();
            if (lastFail != null && Duration.between(lastFail, LocalDateTime.now()).toSeconds() < 30) {
                failDelivery(delivery, "Circuit Breaker is OPEN for this endpoint");
                return;
            } else {
                endpoint.setCircuitState(CircuitState.HALF_OPEN);
                endpointRepository.save(endpoint);
                log.info("Circuit breaker transitioned to HALF_OPEN for endpoint: {}", endpoint.getName());
            }
        }

        // 2. Aggregate Delivery Ordering Check
        if (delivery.getAggregateType() != null && !delivery.getAggregateId().equals("0")) {
            List<WebhookDelivery> pendings = deliveryRepository.findPendingDeliveriesForAggregate(delivery.getAggregateType(), delivery.getAggregateId());
            if (!pendings.isEmpty() && !pendings.get(0).getId().equals(delivery.getId())) {
                // Not the first event, skip and retry later to maintain order
                log.info("Deferring delivery {} to preserve aggregate ordering for {}:{}", delivery.getId(), delivery.getAggregateType(), delivery.getAggregateId());
                deferDelivery(delivery);
                return;
            }
        }

        delivery.setDeliveryStatus(WebhookStatus.PROCESSING);
        deliveryRepository.save(delivery);

        MDC.put("CorrelationId", delivery.getCorrelationId());
        MDC.put("WebhookId", String.valueOf(endpoint.getId()));
        MDC.put("DeliveryId", String.valueOf(delivery.getId()));
        MDC.put("EventType", delivery.getEventType().name());

        long startTime = System.currentTimeMillis();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Webhook-Version", endpoint.getWebhookVersion());
        
        long timestamp = System.currentTimeMillis() / 1000;
        headers.set("X-Signature", HmacSignatureGenerator.generateSignature(delivery.getPayload(), endpoint.getSecretKey(), timestamp));
        headers.set("X-Timestamp", String.valueOf(timestamp));
        headers.set("X-Idempotency-Key", delivery.getIdempotencyKey());

        byte[] requestBodyBytes = delivery.getPayload().getBytes(StandardCharsets.UTF_8);

        // Compression check
        if (endpoint.isCompressionEnabled()) {
            headers.set("Content-Encoding", "gzip");
            try {
                ByteArrayOutputStream obj = new ByteArrayOutputStream();
                GZIPOutputStream gzip = new GZIPOutputStream(obj);
                gzip.write(requestBodyBytes);
                gzip.close();
                requestBodyBytes = obj.toByteArray();
            } catch (IOException e) {
                log.error("Failed to compress payload", e);
            }
        }

        HttpEntity<byte[]> entity = new HttpEntity<>(requestBodyBytes, headers);

        try {
            ResponseEntity<String> response;
            if (resilienceService != null) {
                response = resilienceService.execute("webhooks",
                        () -> restTemplate.postForEntity(endpoint.getTargetUrl(), entity, String.class),
                        () -> {
                            throw new RuntimeException("Webhook target unavailable (resilience fallback)");
                        }
                );
            } else {
                response = restTemplate.postForEntity(endpoint.getTargetUrl(), entity, String.class);
            }
            long latency = System.currentTimeMillis() - startTime;

            delivery.setResponseStatus(response.getStatusCode().value());
            delivery.setResponseBody(response.getBody());
            delivery.setExecutionTimeMs(latency);
            delivery.setDeliveredAt(LocalDateTime.now());
            delivery.setDeliveryStatus(WebhookStatus.DELIVERED);
            deliveryRepository.save(delivery);

            // Reset circuit state on success
            if (endpoint.getCircuitState() != CircuitState.CLOSED) {
                endpoint.setCircuitState(CircuitState.CLOSED);
                endpoint.setConsecutiveFailures(0);
                endpointRepository.save(endpoint);
                audit(AuditActionType.valueOf("WEBHOOK_CIRCUIT_CLOSED"), AuditStatus.SUCCESS, String.valueOf(endpoint.getId()), "Circuit closed for endpoint: " + endpoint.getName());
            }

            audit(AuditActionType.valueOf("WEBHOOK_DELIVERED"), AuditStatus.SUCCESS, String.valueOf(delivery.getId()), "Delivered webhook event successfully");
            
            log.info("WEBHOOK_EVENT | CorrelationId={} | Endpoint={} | EventType={} | ExecutionTime={}ms | RetryCount={} | Status=DELIVERED | ResponseCode={} | Timestamp={}",
                    delivery.getCorrelationId(), endpoint.getName(), delivery.getEventType(), latency, delivery.getRetryCount(), response.getStatusCode().value(), LocalDateTime.now());

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            delivery.setExecutionTimeMs(latency);
            delivery.setFailureReason(e.getMessage());
            
            // Record failure on circuit breaker
            endpoint.setConsecutiveFailures(endpoint.getConsecutiveFailures() + 1);
            endpoint.setLastFailureTime(LocalDateTime.now());
            if (endpoint.getConsecutiveFailures() >= 5) {
                endpoint.setCircuitState(CircuitState.OPEN);
                endpointRepository.save(endpoint);
                audit(AuditActionType.valueOf("WEBHOOK_CIRCUIT_OPEN"), AuditStatus.SUCCESS, String.valueOf(endpoint.getId()), "Circuit opened for endpoint: " + endpoint.getName());
            } else {
                endpointRepository.save(endpoint);
            }

            failDelivery(delivery, e.getMessage());
        } finally {
            MDC.clear();
        }
    }

    private void failDelivery(WebhookDelivery delivery, String reason) {
        delivery.setDeliveryStatus(WebhookStatus.FAILED);
        delivery.setFailureReason(reason);
        deliveryRepository.save(delivery);

        audit(AuditActionType.valueOf("WEBHOOK_FAILED"), AuditStatus.FAILED, String.valueOf(delivery.getId()), "Failed webhook delivery attempt: " + reason);
        log.error("Failed webhook delivery ID: {}. Reason: {}", delivery.getId(), reason);

        // Schedule retry (via Retry Service/Scheduler)
    }

    private void deferDelivery(WebhookDelivery delivery) {
        delivery.setDeliveryStatus(WebhookStatus.PENDING);
        deliveryRepository.save(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookResponse> getEndpoints() {
        return endpointRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookDeliveryResponse> getDeliveries() {
        return deliveryRepository.findAll().stream()
                .map(this::mapToDeliveryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookDeliveryResponse> getDeadLetters() {
        return deliveryRepository.findDeadLetters().stream()
                .map(this::mapToDeliveryResponse)
                .collect(Collectors.toList());
    }

    private WebhookResponse mapToResponse(WebhookEndpoint endpoint) {
        return WebhookResponse.builder()
                .id(endpoint.getId())
                .name(endpoint.getName())
                .targetUrl(endpoint.getTargetUrl())
                .secretKey("********************") // Secure masking
                .enabled(endpoint.isEnabled())
                .timeoutMs(endpoint.getTimeoutMs())
                .retryEnabled(endpoint.isRetryEnabled())
                .maxRetryCount(endpoint.getMaxRetryCount())
                .subscribedEvents(endpoint.getSubscribedEvents())
                .webhookVersion(endpoint.getWebhookVersion())
                .filterPriority(endpoint.getFilterPriority())
                .filterChannel(endpoint.getFilterChannel())
                .filterSeverity(endpoint.getFilterSeverity())
                .requestsPerMinute(endpoint.getRequestsPerMinute())
                .requestsPerHour(endpoint.getRequestsPerHour())
                .batchEnabled(endpoint.isBatchEnabled())
                .batchSize(endpoint.getBatchSize())
                .batchIntervalSeconds(endpoint.getBatchIntervalSeconds())
                .compressionEnabled(endpoint.isCompressionEnabled())
                .circuitState(endpoint.getCircuitState())
                .consecutiveFailures(endpoint.getConsecutiveFailures())
                .lastFailureTime(endpoint.getLastFailureTime())
                .version(endpoint.getVersion() != null ? endpoint.getVersion().longValue() : null)
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }

    private WebhookDeliveryResponse mapToDeliveryResponse(WebhookDelivery d) {
        return WebhookDeliveryResponse.builder()
                .id(d.getId())
                .webhookEndpointId(d.getWebhookEndpoint().getId())
                .webhookEndpointName(d.getWebhookEndpoint().getName())
                .targetUrl(d.getWebhookEndpoint().getTargetUrl())
                .eventType(d.getEventType())
                .payload(d.getPayload())
                .requestHeaders(d.getRequestHeaders())
                .responseStatus(d.getResponseStatus())
                .responseBody(d.getResponseBody())
                .deliveryStatus(d.getDeliveryStatus())
                .retryCount(d.getRetryCount())
                .executionTimeMs(d.getExecutionTimeMs())
                .correlationId(d.getCorrelationId())
                .idempotencyKey(d.getIdempotencyKey())
                .failureReason(d.getFailureReason())
                .createdAt(d.getCreatedAt())
                .deliveredAt(d.getDeliveredAt())
                .build();
    }
}
