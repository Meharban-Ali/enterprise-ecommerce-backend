package com.redis.payment.entity;

import com.redis.order.service.OrderService;

import com.redis.order.entity.Order;
import com.redis.payment.entity.Payment;
import com.redis.order.entity.OrderStatus;
import com.redis.payment.entity.PaymentStatus;
import com.redis.order.repository.OrderRepository;
import com.redis.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentExpirationSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private PaymentExpirationScheduler scheduler;

    @Test
    void testExpirePendingPaymentsSuccess() {
        Order expiredOrder = Order.builder()
                .id(100L)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        Payment pendingPayment = Payment.builder()
                .id(500L)
                .order(expiredOrder)
                .status(PaymentStatus.PENDING)
                .build();

        when(orderRepository.findByStatusAndOrderDateBefore(eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(expiredOrder));

        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(pendingPayment));

        scheduler.expirePendingPayments();

        verify(orderService, times(1)).expireOrder(100L);
        verify(paymentRepository, times(1)).save(pendingPayment);
    }
}
