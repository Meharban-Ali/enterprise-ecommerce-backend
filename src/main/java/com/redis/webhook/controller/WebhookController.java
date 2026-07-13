package com.redis.webhook.controller;

import com.redis.webhook.dto.response.WebhookDeliveryResponse;
import com.redis.webhook.dto.response.WebhookResponse;
import com.redis.webhook.dto.response.WebhookDashboardResponse;
import com.redis.webhook.dto.response.WebhookTestResponse;
import com.redis.webhook.dto.request.WebhookRequest;
import com.redis.webhook.dto.response.WebhookSecretRotateResponse;

import com.redis.common.dto.ApiResponse;
import com.redis.webhook.service.WebhookService;
import com.redis.webhook.service.WebhookMetricsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/webhooks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class WebhookController {

    private final WebhookService webhookService;
    private final WebhookMetricsService metricsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WebhookResponse>>> getWebhooks() {
        List<WebhookResponse> endpoints = webhookService.getEndpoints();
        return ResponseEntity.ok(ApiResponse.success("Webhook endpoints retrieved successfully", endpoints));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WebhookResponse>> registerWebhook(@Valid @RequestBody WebhookRequest request) {
        WebhookResponse response = webhookService.registerWebhook(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Webhook endpoint registered successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WebhookResponse>> updateWebhook(
            @PathVariable Long id,
            @Valid @RequestBody WebhookRequest request
    ) {
        WebhookResponse response = webhookService.updateWebhook(id, request);
        return ResponseEntity.ok(ApiResponse.success("Webhook endpoint updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWebhook(@PathVariable Long id) {
        webhookService.deleteWebhook(id);
        return ResponseEntity.ok(ApiResponse.success("Webhook endpoint deleted successfully", null));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<WebhookResponse>> enableWebhook(@PathVariable Long id) {
        WebhookResponse response = webhookService.enableWebhook(id);
        return ResponseEntity.ok(ApiResponse.success("Webhook endpoint enabled successfully", response));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<WebhookResponse>> disableWebhook(@PathVariable Long id) {
        WebhookResponse response = webhookService.disableWebhook(id);
        return ResponseEntity.ok(ApiResponse.success("Webhook endpoint disabled successfully", response));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<ApiResponse<WebhookTestResponse>> testWebhook(@PathVariable Long id) {
        WebhookTestResponse response = webhookService.testWebhook(id);
        return ResponseEntity.ok(ApiResponse.success("Webhook endpoint test execution completed", response));
    }

    @PostMapping("/{id}/rotate-secret")
    public ResponseEntity<ApiResponse<WebhookSecretRotateResponse>> rotateSecret(@PathVariable Long id) {
        WebhookSecretRotateResponse response = webhookService.rotateSecret(id);
        return ResponseEntity.ok(ApiResponse.success("Webhook endpoint secret key rotated successfully", response));
    }

    @GetMapping("/deliveries")
    public ResponseEntity<ApiResponse<List<WebhookDeliveryResponse>>> getDeliveries() {
        List<WebhookDeliveryResponse> deliveries = webhookService.getDeliveries();
        return ResponseEntity.ok(ApiResponse.success("Webhook delivery log entries retrieved successfully", deliveries));
    }

    @GetMapping("/dead-letter")
    public ResponseEntity<ApiResponse<List<WebhookDeliveryResponse>>> getDeadLetters() {
        List<WebhookDeliveryResponse> deadLetters = webhookService.getDeadLetters();
        return ResponseEntity.ok(ApiResponse.success("Webhook dead letter queue entries retrieved successfully", deadLetters));
    }

    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<WebhookDashboardResponse>> getStatistics() {
        WebhookDashboardResponse dashboard = metricsService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success("Webhook integration statistics retrieved successfully", dashboard));
    }
}
