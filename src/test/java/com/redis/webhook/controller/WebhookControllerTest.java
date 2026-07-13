package com.redis.webhook.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testStripeWebhookSuccess() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=123,v1=abc")
                        .header("Idempotency-Key", "key-stripe-123")
                        .content("{\"id\": \"evt_123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testStripeWebhookInvalidSignature() throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "invalid_signature")
                        .content("{\"id\": \"evt_123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testStripeWebhookMissingSignature() throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\": \"evt_123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testStripeWebhookDuplicateRejected() throws Exception {
        // First call returns true (absent, success), second returns false (exists, duplicate conflict)
        when(valueOperations.setIfAbsent(eq("webhook:idempotency:stripe:evt_789"), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true)
                .thenReturn(false);

        // First request succeeds
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=123,v1=abc")
                        .header("Idempotency-Key", "key-dup-stripe-789")
                        .content("{\"id\": \"evt_789\"}"))
                .andExpect(status().isOk());

        // Second duplicate request fails with 409 Conflict
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=123,v1=abc")
                        .header("Idempotency-Key", "key-dup-stripe-789")
                        .content("{\"id\": \"evt_789\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRazorpayWebhookSuccess() throws Exception {
        when(valueOperations.setIfAbsent(anyString(), any(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        mockMvc.perform(post("/api/webhooks/razorpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "rzp_sig_123")
                        .header("Idempotency-Key", "key-rzp-123")
                        .content("{\"id\": \"pay_123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
