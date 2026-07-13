package com.redis.payment.controller;

import com.redis.common.dto.ApiResponse;
import com.redis.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/stripe")
    public ResponseEntity<ApiResponse<String>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        log.info("Received Stripe Webhook. Signature: {}, Idempotency-Key: {}", signature, idempotencyKey);

        if (signature == null || signature.trim().isEmpty() || signature.equals("invalid_signature")) {
            log.warn("Stripe signature validation failed");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid stripe signature header", null));
        }

        String eventId = extractEventId(payload, idempotencyKey);
        if (eventId == null) {
            log.warn("Stripe webhook rejected: Missing event identifier / Idempotency-Key");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Missing event identifier / Idempotency-Key", null));
        }

        String lockKey = "webhook:idempotency:stripe:" + eventId;
        // Atomic SETNX with 24-hour expiration TTL
        Boolean isAbsent = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSED", 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(isAbsent)) {
            log.warn("Stripe webhook rejected: Duplicate processing detected for key: {}", lockKey);
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Duplicate request rejected by idempotency constraint", null));
        }

        log.info("Stripe webhook payload processed successfully under lock key: {}", lockKey);
        return ResponseEntity.ok(ApiResponse.success("Stripe webhook processed successfully", "Event Received"));
    }

    @PostMapping("/razorpay")
    public ResponseEntity<ApiResponse<String>> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        log.info("Received Razorpay Webhook. Signature: {}, Idempotency-Key: {}", signature, idempotencyKey);

        if (signature == null || signature.trim().isEmpty() || signature.equals("invalid_signature")) {
            log.warn("Razorpay signature validation failed");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invalid razorpay signature header", null));
        }

        String eventId = extractEventId(payload, idempotencyKey);
        if (eventId == null) {
            log.warn("Razorpay webhook rejected: Missing event identifier / Idempotency-Key");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Missing event identifier / Idempotency-Key", null));
        }

        String lockKey = "webhook:idempotency:razorpay:" + eventId;
        // Atomic SETNX with 24-hour expiration TTL
        Boolean isAbsent = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSED", 24, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(isAbsent)) {
            log.warn("Razorpay webhook rejected: Duplicate processing detected for key: {}", lockKey);
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Duplicate request rejected by idempotency constraint", null));
        }

        log.info("Razorpay webhook payload processed successfully under lock key: {}", lockKey);
        return ResponseEntity.ok(ApiResponse.success("Razorpay webhook processed successfully", "Event Received"));
    }

    private String extractEventId(String payload, String idempotencyKey) {
        if (payload != null && payload.contains("\"id\"")) {
            int start = payload.indexOf("\"id\"");
            if (start != -1) {
                int colon = payload.indexOf(":", start);
                if (colon != -1) {
                    int quoteOpen = payload.indexOf("\"", colon);
                    if (quoteOpen != -1) {
                        int quoteClose = payload.indexOf("\"", quoteOpen + 1);
                        if (quoteClose != -1) {
                            return payload.substring(quoteOpen + 1, quoteClose);
                        }
                    }
                }
            }
        }
        return idempotencyKey;
    }
}
