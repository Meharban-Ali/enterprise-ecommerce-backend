package com.redis.payment.service.gateway;

import com.redis.payment.entity.Payment;
import com.redis.payment.dto.response.PaymentResponse;

import java.math.BigDecimal;

public interface PaymentGateway {
    PaymentResponse createPaymentSession(Payment payment);
    PaymentResponse verifyPayment(Payment payment, String gatewayReferenceId);
    PaymentResponse cancelPayment(Payment payment);
    PaymentResponse refundPayment(Payment payment, BigDecimal amount, String reason);
    com.redis.payment.entity.PaymentGateway getGatewayName();
}
