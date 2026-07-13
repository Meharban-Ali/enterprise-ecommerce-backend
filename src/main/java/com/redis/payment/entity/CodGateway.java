package com.redis.payment.entity;

import com.redis.payment.service.gateway.PaymentGateway;

import com.redis.payment.entity.Payment;
import com.redis.payment.entity.PaymentStatus;
import com.redis.payment.dto.response.PaymentResponse;
import com.redis.payment.dto.response.PaymentTransactionResponse;
import com.redis.payment.dto.response.RefundResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

@Component
public class CodGateway implements PaymentGateway {

    @Override
    public PaymentResponse createPaymentSession(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(PaymentStatus.SUCCESS.name()) // COD completes authorization immediately
                .paymentMethod(payment.getPaymentMethod().name())
                .paymentGateway(getGatewayName().name())
                .transactions(Collections.singletonList(
                        PaymentTransactionResponse.builder()
                                .gatewayReferenceId("cod_placeholder_" + payment.getId())
                                .type("CAPTURE")
                                .amount(payment.getAmount())
                                .createdAt(LocalDateTime.now())
                                .build()
                ))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public PaymentResponse verifyPayment(Payment payment, String gatewayReferenceId) {
        return createPaymentSession(payment);
    }

    @Override
    public PaymentResponse cancelPayment(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .status(PaymentStatus.CANCELLED.name())
                .paymentGateway(getGatewayName().name())
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public PaymentResponse refundPayment(Payment payment, BigDecimal amount, String reason) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .status(PaymentStatus.REFUNDED.name())
                .paymentGateway(getGatewayName().name())
                .refunds(Collections.singletonList(
                        RefundResponse.builder()
                                .amount(amount)
                                .status("SUCCESS")
                                .reason(reason)
                                .gatewayReferenceId("cod_refund_placeholder_" + payment.getId())
                                .createdAt(LocalDateTime.now())
                                .build()
                ))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public com.redis.payment.entity.PaymentGateway getGatewayName() {
        return com.redis.payment.entity.PaymentGateway.NONE;
    }
}
