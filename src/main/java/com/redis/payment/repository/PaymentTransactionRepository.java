package com.redis.payment.repository;

import com.redis.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByPaymentId(Long paymentId);

    Optional<PaymentTransaction> findByGatewayReferenceId(String gatewayReferenceId);
}
