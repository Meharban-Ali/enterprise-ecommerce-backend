package com.redis.payment.service;

import com.redis.payment.dto.request.CreatePaymentRequest;
import com.redis.payment.dto.response.PaymentResponse;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentResponse createPayment(Long userId, CreatePaymentRequest request);
    PaymentResponse verifyPayment(Long userId, Long paymentId, String gatewayReferenceId);
    PaymentResponse cancelPayment(Long userId, Long paymentId);
    PaymentResponse refundPayment(Long userId, Long paymentId, BigDecimal amount, String reason, boolean isAdmin);
    PaymentResponse getPaymentById(Long userId, Long paymentId, boolean isAdmin);
    PaymentResponse getPaymentByOrderId(Long userId, Long orderId, boolean isAdmin);
    Page<PaymentResponse> getMyPayments(Long userId, Pageable pageable);
    Page<PaymentResponse> getAllPayments(Pageable pageable);
    PaymentResponse retryPayment(Long orderId);
    void expirePendingPaymentForOrder(com.redis.order.entity.Order order);
}
