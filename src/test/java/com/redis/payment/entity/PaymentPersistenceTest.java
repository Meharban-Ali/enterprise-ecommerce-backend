package com.redis.payment.entity;

import com.redis.payment.repository.PaymentTransactionRepository;
import com.redis.payment.service.gateway.PaymentGateway;
import com.redis.order.entity.Order;
import com.redis.payment.repository.PaymentRepository;
import com.redis.payment.repository.RefundRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentPersistenceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private RefundRepository refundRepository;

    private Order testOrder;
    private Payment testPayment;
    private PaymentTransaction testTransaction;
    private Refund testRefund;

    @BeforeEach
    void setUp() {
        testOrder = Order.builder()
                .id(1L)
                .totalAmount(new BigDecimal("199.99"))
                .build();

        testPayment = Payment.builder()
                .id(10L)
                .order(testOrder)
                .amount(new BigDecimal("199.99"))
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .paymentMethod(PaymentMethod.CARD)
                .paymentGateway(com.redis.payment.entity.PaymentGateway.STRIPE)
                .transactions(new ArrayList<>())
                .refunds(new ArrayList<>())
                .build();

        testTransaction = PaymentTransaction.builder()
                .id(20L)
                .payment(testPayment)
                .gatewayReferenceId("ch_stripe_123")
                .type(PaymentTransactionType.CAPTURE)
                .amount(new BigDecimal("199.99"))
                .build();

        testPayment.getTransactions().add(testTransaction);

        testRefund = Refund.builder()
                .id(30L)
                .payment(testPayment)
                .amount(new BigDecimal("50.00"))
                .status(RefundStatus.SUCCESS)
                .reason("Customer request")
                .build();

        testPayment.getRefunds().add(testRefund);
    }

    @Test
    void testEntityRelationsAndMappings() {
        assertNotNull(testPayment.getOrder());
        assertEquals(testOrder.getId(), testPayment.getOrder().getId());
        assertEquals(1, testPayment.getTransactions().size());
        assertEquals(testTransaction.getId(), testPayment.getTransactions().get(0).getId());
        assertEquals(1, testPayment.getRefunds().size());
        assertEquals(testRefund.getId(), testPayment.getRefunds().get(0).getId());
    }

    @Test
    void testEnumsAndValues() {
        assertEquals(PaymentStatus.PENDING, testPayment.getStatus());
        assertEquals(PaymentMethod.CARD, testPayment.getPaymentMethod());
        assertEquals(com.redis.payment.entity.PaymentGateway.STRIPE, testPayment.getPaymentGateway());
        assertEquals(PaymentTransactionType.CAPTURE, testTransaction.getType());
        assertEquals(RefundStatus.SUCCESS, testRefund.getStatus());
    }

    @Test
    void testPaymentRepositoryMocking() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(testPayment));
        Optional<Payment> result = paymentRepository.findByOrderId(1L);
        assertTrue(result.isPresent());
        assertEquals(testPayment.getId(), result.get().getId());
        verify(paymentRepository, times(1)).findByOrderId(1L);
    }

    @Test
    void testTransactionRepositoryMocking() {
        when(paymentTransactionRepository.findByGatewayReferenceId("ch_stripe_123")).thenReturn(Optional.of(testTransaction));
        Optional<PaymentTransaction> result = paymentTransactionRepository.findByGatewayReferenceId("ch_stripe_123");
        assertTrue(result.isPresent());
        assertEquals(testTransaction.getId(), result.get().getId());
        verify(paymentTransactionRepository, times(1)).findByGatewayReferenceId("ch_stripe_123");
    }

    @Test
    void testRefundRepositoryMocking() {
        when(refundRepository.findByPaymentId(10L)).thenReturn(Collections.singletonList(testRefund));
        List<Refund> result = refundRepository.findByPaymentId(10L);
        assertEquals(1, result.size());
        assertEquals(testRefund.getId(), result.get(0).getId());
        verify(refundRepository, times(1)).findByPaymentId(10L);
    }
}
