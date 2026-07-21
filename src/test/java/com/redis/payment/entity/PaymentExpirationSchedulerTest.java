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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import com.redis.monitoring.service.SystemMonitoringService;
import com.redis.payment.service.PaymentService;

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

    @Mock
    private PaymentService paymentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ObjectProvider<SystemMonitoringService> monitoringServiceProvider;

    @InjectMocks
    private PaymentExpirationScheduler scheduler;

    @Test
    void testExpirePendingPaymentsSuccess() {
        Order expiredOrder = Order.builder()
                .id(100L)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        when(orderRepository.findByStatusAndOrderDateBefore(eq(OrderStatus.PENDING_PAYMENT), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(expiredOrder));

        scheduler.expirePendingPayments();

        verify(paymentService, times(1)).expirePendingPaymentForOrder(expiredOrder);
    }
}
