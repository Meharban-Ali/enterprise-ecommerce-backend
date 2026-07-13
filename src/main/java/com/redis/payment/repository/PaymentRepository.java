package com.redis.payment.repository;

import com.redis.payment.entity.Payment;
import com.redis.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"transactions", "refunds"})
    Page<Payment> findByOrderUserId(Long userId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"transactions", "refunds"})
    Page<Payment> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"transactions", "refunds"})
    Optional<Payment> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"transactions", "refunds"})
    List<Payment> findByStatus(PaymentStatus status);

    @EntityGraph(attributePaths = {"transactions", "refunds"})
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, java.time.LocalDateTime time);

    long countByStatus(PaymentStatus status);
}
