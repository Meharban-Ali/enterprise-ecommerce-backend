package com.redis.payment.service;

import com.redis.payment.entity.Payment;
import com.redis.product.entity.Product;
import com.redis.order.entity.Order;
import com.redis.inventory.service.InventoryReservationService;
import com.redis.payment.repository.PaymentRepository;
import com.redis.order.entity.OrderStatus;
import com.redis.payment.entity.PaymentFactory;
import com.redis.payment.entity.PaymentStatus;
import com.redis.order.repository.OrderRepository;
import com.redis.order.service.OrderService;
import com.redis.user.entity.User;
import com.redis.payment.entity.PaymentMethod;

import com.redis.payment.dto.request.CreatePaymentRequest;
import com.redis.payment.dto.response.PaymentResponse;
import com.redis.payment.dto.response.PaymentTransactionResponse;
import com.redis.payment.dto.response.RefundResponse;
import com.redis.payment.service.gateway.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redis.reliability.service.PlatformResilienceService;
import java.util.function.Supplier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentFactory paymentFactory;
    @Mock private PaymentGateway mockGateway;
    @Mock private OrderService orderService;
    @Mock private InventoryReservationService inventoryReservationService;
    @Mock private PlatformResilienceService resilienceService;

    @InjectMocks private PaymentServiceImpl paymentService;

    private User testUser;
    private Order testOrder;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        lenient().when(resilienceService.execute(anyString(), any(Supplier.class), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(1);
                    return supplier.get();
                });

        testUser = User.builder().id(1L).email("user@example.com").build();
        testOrder = Order.builder()
                .id(100L)
                .user(testUser)
                .status(OrderStatus.PENDING_PAYMENT)
                .totalAmount(new BigDecimal("99.99"))
                .build();

        testPayment = Payment.builder()
                .id(500L)
                .order(testOrder)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.CARD)
                .paymentGateway(com.redis.payment.entity.PaymentGateway.STRIPE)
                .transactions(new ArrayList<>())
                .refunds(new ArrayList<>())
                .build();
    }

    @Test
    void testCreatePaymentSuccess() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(100L)
                .paymentMethod("CARD")
                .paymentGateway("STRIPE")
                .build();

        PaymentResponse mockGatewayResp = PaymentResponse.builder()
                .id(500L)
                .transactions(Collections.singletonList(
                        PaymentTransactionResponse.builder()
                                .gatewayReferenceId("stripe_ref_123")
                                .type("AUTHORIZE")
                                .amount(new BigDecimal("99.99"))
                                .build()
                ))
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));
        when(paymentFactory.getGateway(com.redis.payment.entity.PaymentGateway.STRIPE)).thenReturn(mockGateway);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);
        when(mockGateway.createPaymentSession(any(Payment.class))).thenReturn(mockGatewayResp);

        PaymentResponse response = paymentService.createPayment(1L, request);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    void testCreatePaymentDeniedForOtherUser() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .orderId(100L)
                .paymentMethod("CARD")
                .paymentGateway("STRIPE")
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalArgumentException.class, () -> paymentService.createPayment(2L, request));
    }

    @Test
    void testVerifyPaymentSuccess() {
        when(paymentRepository.findById(500L)).thenReturn(Optional.of(testPayment));
        when(paymentFactory.getGateway(com.redis.payment.entity.PaymentGateway.STRIPE)).thenReturn(mockGateway);

        PaymentResponse mockGatewayResp = PaymentResponse.builder()
                .status("SUCCESS")
                .build();
        when(mockGateway.verifyPayment(any(Payment.class), eq("stripe_ref_123"))).thenReturn(mockGatewayResp);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        PaymentResponse response = paymentService.verifyPayment(1L, 500L, "stripe_ref_123");
        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void testCancelPaymentSuccess() {
        when(paymentRepository.findById(500L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        PaymentResponse response = paymentService.cancelPayment(1L, 500L);
        assertNotNull(response);
        assertEquals("CANCELLED", response.getStatus());
    }

    @Test
    void testRefundPaymentSuccess() {
        testPayment.setStatus(PaymentStatus.SUCCESS);
        when(paymentRepository.findById(500L)).thenReturn(Optional.of(testPayment));
        when(paymentFactory.getGateway(com.redis.payment.entity.PaymentGateway.STRIPE)).thenReturn(mockGateway);

        PaymentResponse mockGatewayResp = PaymentResponse.builder()
                .refunds(Collections.singletonList(
                        RefundResponse.builder()
                                .gatewayReferenceId("stripe_refund_789")
                                .build()
                ))
                .build();
        when(mockGateway.refundPayment(any(Payment.class), any(BigDecimal.class), anyString())).thenReturn(mockGatewayResp);
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        PaymentResponse response = paymentService.refundPayment(1L, 500L, new BigDecimal("40.00"), "Product damage", false);
        assertNotNull(response);
        assertEquals("PARTIALLY_REFUNDED", response.getStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }
}
