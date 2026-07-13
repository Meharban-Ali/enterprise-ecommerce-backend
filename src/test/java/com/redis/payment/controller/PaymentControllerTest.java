package com.redis.payment.controller;

import com.redis.product.entity.Product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redis.payment.dto.request.CreatePaymentRequest;
import com.redis.payment.dto.request.RefundRequest;
import com.redis.payment.dto.response.PaymentResponse;
import com.redis.user.entity.User;
import com.redis.user.entity.Role;
import com.redis.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private User testUser;
    private User testAdmin;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .role(Role.ROLE_USER)
                .build();

        testAdmin = User.builder()
                .id(2L)
                .email("admin@example.com")
                .role(Role.ROLE_ADMIN)
                .build();
    }

    @Test
    void testCreatePaymentSuccess() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(100L)
                .paymentMethod("CARD")
                .paymentGateway("STRIPE")
                .build();

        PaymentResponse mockResponse = PaymentResponse.builder()
                .id(500L)
                .amount(new BigDecimal("99.99"))
                .status("PENDING")
                .build();

        when(paymentService.createPayment(eq(1L), any(CreatePaymentRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/create")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(500L));
    }

    @Test
    void testCreatePaymentValidationError() throws Exception {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(-50L) // Invalid positive constraint
                .paymentMethod("") // Blank method
                .paymentGateway("") // Blank gateway
                .build();

        mockMvc.perform(post("/api/payments/create")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testGetPaymentByIdSuccess() throws Exception {
        PaymentResponse mockResponse = PaymentResponse.builder()
                .id(500L)
                .amount(new BigDecimal("99.99"))
                .status("PENDING")
                .build();

        when(paymentService.getPaymentById(eq(1L), eq(500L), eq(false))).thenReturn(mockResponse);

        mockMvc.perform(get("/api/payments/500")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testRefundPaymentSuccess() throws Exception {
        RefundRequest request = RefundRequest.builder()
                .amount(new BigDecimal("40.00"))
                .reason("Product damaged")
                .build();

        PaymentResponse mockResponse = PaymentResponse.builder()
                .id(500L)
                .amount(new BigDecimal("99.99"))
                .status("PARTIALLY_REFUNDED")
                .build();

        when(paymentService.refundPayment(eq(1L), eq(500L), any(BigDecimal.class), eq("Product damaged"), eq(false)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/500/refund")
                        .with(user(testUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testGetAdminPaymentsForbiddenForUser() throws Exception {
        mockMvc.perform(get("/api/payments/admin")
                        .with(user(testUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetAdminPaymentsSuccessForAdmin() throws Exception {
        mockMvc.perform(get("/api/payments/admin")
                        .with(user(testAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void testRetryPaymentSuccess() throws Exception {
        PaymentResponse mockResponse = PaymentResponse.builder()
                .id(500L)
                .amount(new BigDecimal("99.99"))
                .status("PENDING")
                .build();

        when(paymentService.retryPayment(eq(100L))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/payments/order/100/retry")
                        .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(500L));
    }
}
