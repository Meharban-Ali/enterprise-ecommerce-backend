package com.redis.payment.entity;

import com.redis.payment.exception.UnsupportedPaymentMethodException;
import com.redis.payment.service.gateway.PaymentGateway;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class PaymentFactory {

    private final Map<com.redis.payment.entity.PaymentGateway, com.redis.payment.service.gateway.PaymentGateway> gatewayMap;

    public PaymentFactory(List<com.redis.payment.service.gateway.PaymentGateway> gateways) {
        this.gatewayMap = gateways.stream()
                .collect(Collectors.toMap(com.redis.payment.service.gateway.PaymentGateway::getGatewayName, g -> g));
    }

    public com.redis.payment.service.gateway.PaymentGateway getGateway(com.redis.payment.entity.PaymentGateway gateway) {
        com.redis.payment.service.gateway.PaymentGateway paymentGateway = gatewayMap.get(gateway);
        if (paymentGateway == null) {
            throw new UnsupportedPaymentMethodException("Payment gateway is not supported: " + gateway);
        }
        return paymentGateway;
    }
}
