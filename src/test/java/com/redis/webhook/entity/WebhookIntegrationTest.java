package com.redis.webhook.entity;

import com.redis.webhook.repository.WebhookEndpointRepository;
import com.redis.webhook.service.WebhookMetricsService;
import com.redis.webhook.dto.response.WebhookResponse;
import com.redis.webhook.dto.response.WebhookDashboardResponse;
import com.redis.order.entity.Order;
import com.redis.common.entity.CircuitState;
import com.redis.order.entity.OrderStatus;
import com.redis.webhook.service.WebhookService;
import com.redis.common.entity.IntegrationEventType;
import com.redis.webhook.repository.WebhookDeliveryRepository;
import com.redis.webhook.dto.response.WebhookStatisticsResponse;
import com.redis.webhook.dto.request.WebhookRequest;
import com.redis.webhook.dto.response.WebhookSecretRotateResponse;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.common.util.HmacSignatureGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
@Transactional
public class WebhookIntegrationTest {

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private WebhookMetricsService metricsService;

    @Autowired
    private WebhookEndpointRepository endpointRepository;

    @Autowired
    private WebhookDeliveryRepository deliveryRepository;

    private WebhookEndpoint testEndpoint;

    @BeforeEach
    void setUp() {
        deliveryRepository.deleteAll();
        endpointRepository.deleteAll();

        testEndpoint = WebhookEndpoint.builder()
                .name("Integration Hook")
                .targetUrl("http://localhost:8080/webhook-receiver")
                .secretKey("supersecretkey123")
                .enabled(true)
                .timeoutMs(5000)
                .retryEnabled(true)
                .maxRetryCount(3)
                .subscribedEvents(new HashSet<>(Collections.singletonList(IntegrationEventType.ORDER_CREATED)))
                .webhookVersion("v1")
                .circuitState(CircuitState.CLOSED)
                .consecutiveFailures(0)
                .build();
        testEndpoint = endpointRepository.save(testEndpoint);
    }

    @Test
    void testRegisterAndUpdateWebhook() {
        WebhookRequest req = WebhookRequest.builder()
                .name("New Endpoint")
                .targetUrl("http://localhost:9090/callback")
                .secretKey("mysecret")
                .enabled(true)
                .timeoutMs(5000)
                .retryEnabled(true)
                .maxRetryCount(3)
                .subscribedEvents(new HashSet<>(Collections.singletonList(IntegrationEventType.PAYMENT_SUCCESS)))
                .webhookVersion("v2")
                .build();

        WebhookResponse res = webhookService.registerWebhook(req);
        assertNotNull(res);
        assertEquals("New Endpoint", res.getName());
        assertEquals("v2", res.getWebhookVersion());
        assertEquals("********************", res.getSecretKey()); // Secret masked!

        req.setName("Updated Endpoint Name");
        WebhookResponse updated = webhookService.updateWebhook(res.getId(), req);
        assertEquals("Updated Endpoint Name", updated.getName());
    }

    @Test
    void testSecretRotation() {
        WebhookSecretRotateResponse rotated = webhookService.rotateSecret(testEndpoint.getId());
        assertNotNull(rotated);
        assertNotEquals("supersecretkey123", rotated.getNewSecretKey());
        assertFalse(rotated.getNewSecretKey().isBlank());
    }

    @Test
    void testCircuitBreakerLifecycle() {
        // Trigger failure increments
        testEndpoint.setConsecutiveFailures(5);
        testEndpoint.setCircuitState(CircuitState.OPEN);
        testEndpoint.setLastFailureTime(java.time.LocalDateTime.now());
        endpointRepository.save(testEndpoint);

        // Fetch metrics to check circuit state
        WebhookDashboardResponse dash = metricsService.getDashboard();
        assertEquals(1, dash.getCircuitOpenCount());
    }

    @Test
    void testPayloadTransformer() {
        WebhookPayloadTransformerImpl transformer = new WebhookPayloadTransformerImpl(new com.fasterxml.jackson.databind.ObjectMapper());
        Order order = Order.builder()
                .id(999L)
                .totalAmount(new java.math.BigDecimal("150.00"))
                .status(OrderStatus.DELIVERED)
                .build();

        String json = transformer.transform(IntegrationEventType.ORDER_CREATED, order, "v1");
        assertNotNull(json);
        assertTrue(json.contains("150.00"));
        assertTrue(json.contains("DELIVERED"));
        assertFalse(json.contains("userPassword")); // strip internal fields
    }

    @Test
    void testHmacSignatureGeneration() {
        String payload = "{\"event\": \"test\"}";
        String secret = "secret";
        long timestamp = System.currentTimeMillis() / 1000;

        String signature = HmacSignatureGenerator.generateSignature(payload, secret, timestamp);
        assertNotNull(signature);
        assertTrue(HmacSignatureGenerator.verifySignature(payload, secret, timestamp, signature, 300));
    }

    @Test
    void testCompressionGzip() throws IOException {
        String payload = "Hello World Webhook Compression Test";
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(obj);
        gzip.write(payload.getBytes(StandardCharsets.UTF_8));
        gzip.close();
        byte[] compressed = obj.toByteArray();
        assertTrue(compressed.length > 0);
    }

    @Test
    void testHealthScoreCalculation() {
        WebhookDelivery d1 = WebhookDelivery.builder()
                .webhookEndpoint(testEndpoint)
                .eventType(IntegrationEventType.ORDER_CREATED)
                .payload("{}")
                .deliveryStatus(WebhookStatus.DELIVERED)
                .idempotencyKey("key1")
                .retryCount(0)
                .build();
        deliveryRepository.save(d1);

        WebhookStatisticsResponse stats = metricsService.getEndpointStats(testEndpoint.getId());
        assertEquals(100.0, stats.getHealthScore());
        assertEquals(1, stats.getTotalDeliveries());
    }
}
