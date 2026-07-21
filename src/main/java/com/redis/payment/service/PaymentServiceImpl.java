package com.redis.payment.service;

import com.redis.order.entity.Order;
import com.redis.payment.entity.PaymentTransaction;
import com.redis.order.repository.OrderRepository;
import com.redis.audit.entity.AuditStatus;
import com.redis.inventory.service.InventoryReservationService;
import com.redis.notification.event.NotificationEventPublisher;
import com.redis.payment.entity.RefundStatus;
import com.redis.payment.entity.PaymentStatus;
import com.redis.payment.exception.InvalidPaymentStateException;
import com.redis.order.exception.InvalidOrderStateException;
import com.redis.payment.entity.Payment;
import com.redis.payment.exception.UnsupportedPaymentMethodException;
import com.redis.payment.entity.PaymentFactory;
import com.redis.common.entity.ResourceType;
import com.redis.audit.event.AuditEventPublisher;
import com.redis.order.service.OrderService;
import com.redis.payment.exception.PaymentNotFoundException;
import com.redis.payment.entity.Refund;
import com.redis.payment.entity.PaymentMethod;
import com.redis.payment.service.gateway.PaymentGateway;
import com.redis.payment.entity.PaymentTransactionType;
import com.redis.payment.repository.PaymentRepository;
import com.redis.order.entity.OrderStatus;
import com.redis.audit.entity.AuditActionType;
import com.redis.reliability.service.PlatformResilienceService;

import com.redis.payment.dto.request.CreatePaymentRequest;
import com.redis.payment.dto.response.PaymentResponse;
import com.redis.payment.dto.response.PaymentTransactionResponse;
import com.redis.payment.dto.response.RefundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentFactory paymentFactory;
    private final OrderService orderService;
    private final InventoryReservationService inventoryReservationService;
    private final NotificationEventPublisher notificationEventPublisher;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.redis.audit.event.AuditEventPublisher auditEventPublisher;

    private final PlatformResilienceService resilienceService;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            PaymentFactory paymentFactory,
            @Lazy OrderService orderService,
            InventoryReservationService inventoryReservationService,
            NotificationEventPublisher notificationEventPublisher,
            PlatformResilienceService resilienceService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentFactory = paymentFactory;
        this.orderService = orderService;
        this.inventoryReservationService = inventoryReservationService;
        this.notificationEventPublisher = notificationEventPublisher;
        this.resilienceService = resilienceService;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(Long userId, CreatePaymentRequest request) {
        log.info("Initiating payment session for order ID: {} and user ID: {}", request.getOrderId(), userId);

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + request.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied — order ownership mismatch");
        }

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new InvalidOrderStateException("Cannot pay for order in state: " + order.getStatus());
        }

        com.redis.payment.entity.PaymentGateway gatewayEnum;
        PaymentMethod methodEnum;
        try {
            gatewayEnum = com.redis.payment.entity.PaymentGateway.valueOf(request.getPaymentGateway().toUpperCase());
            methodEnum = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new UnsupportedPaymentMethodException("Unsupported method or gateway values: " + request.getPaymentMethod() + " / " + request.getPaymentGateway());
        }

        com.redis.payment.service.gateway.PaymentGateway gateway = paymentFactory.getGateway(gatewayEnum);

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .currency("USD")
                .status(PaymentStatus.PENDING)
                .paymentMethod(methodEnum)
                .paymentGateway(gatewayEnum)
                .transactions(new ArrayList<>())
                .refunds(new ArrayList<>())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        PaymentResponse gatewayResponse = resilienceService.execute("paymentGateway",
                () -> gateway.createPaymentSession(savedPayment),
                () -> PaymentResponse.builder()
                        .id(savedPayment.getId())
                        .amount(savedPayment.getAmount())
                        .status(PaymentStatus.PENDING.name())
                        .transactions(java.util.Collections.singletonList(
                                PaymentTransactionResponse.builder()
                                        .gatewayReferenceId("MOCK-TXN-" + java.util.UUID.randomUUID().toString())
                                        .amount(savedPayment.getAmount())
                                        .build()
                        ))
                        .build()
        );

        // Record the transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .payment(savedPayment)
                .gatewayReferenceId(gatewayResponse.getTransactions().get(0).getGatewayReferenceId())
                .type(PaymentTransactionType.AUTHORIZE)
                .amount(savedPayment.getAmount())
                .build();
        transaction.setCreatedAt(LocalDateTime.now());

        savedPayment.getTransactions().add(transaction);
        paymentRepository.save(savedPayment);

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(userId, order.getUser().getEmail(), com.redis.audit.entity.AuditActionType.PAYMENT_CREATED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.PAYMENT, String.valueOf(savedPayment.getId()), "Payment session initiated successfully");
        }

        return toResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(Long userId, Long paymentId, String gatewayReferenceId) {
        log.info("Verifying payment ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied — order ownership mismatch");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException("Payment already in state: " + payment.getStatus());
        }

        try {
            com.redis.payment.service.gateway.PaymentGateway gateway = paymentFactory.getGateway(payment.getPaymentGateway());
            PaymentResponse response = resilienceService.execute("paymentGateway",
                    () -> gateway.verifyPayment(payment, gatewayReferenceId),
                    () -> PaymentResponse.builder()
                            .id(payment.getId())
                            .amount(payment.getAmount())
                            .status(PaymentStatus.FAILED.name())
                            .build()
            );

            payment.setStatus(PaymentStatus.SUCCESS);

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .payment(payment)
                    .gatewayReferenceId(gatewayReferenceId)
                    .type(PaymentTransactionType.CAPTURE)
                    .amount(payment.getAmount())
                    .build();
            transaction.setCreatedAt(LocalDateTime.now());

            payment.getTransactions().add(transaction);
            Payment saved = paymentRepository.save(payment);

            // Transition order status and commit reservation
            orderService.completeOrderPayment(payment.getOrder().getId());

            if (auditEventPublisher != null) {
                auditEventPublisher.publish(userId, saved.getOrder().getUser().getEmail(), com.redis.audit.entity.AuditActionType.PAYMENT_SUCCESS, com.redis.audit.entity.AuditStatus.SUCCESS,
                        com.redis.common.entity.ResourceType.PAYMENT, String.valueOf(saved.getId()), "Payment captured successfully");
            }

            try {
                notificationEventPublisher.publishPaymentSuccess(saved.getOrder().getUser().getId(), saved.getOrder().getId(), saved.getAmount());
            } catch (Exception ne) {
                log.error("Failed to publish payment success notification", ne);
            }

            return toResponse(saved);
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            PaymentTransaction transaction = PaymentTransaction.builder()
                    .payment(payment)
                    .gatewayReferenceId(gatewayReferenceId)
                    .type(PaymentTransactionType.CAPTURE)
                    .amount(payment.getAmount())
                    .failureReason(e.getMessage())
                    .build();
            transaction.setCreatedAt(LocalDateTime.now());
            payment.getTransactions().add(transaction);
            Payment saved = paymentRepository.save(payment);
            orderService.failOrderPayment(payment.getOrder().getId());

            if (auditEventPublisher != null) {
                auditEventPublisher.publish(userId, saved.getOrder().getUser().getEmail(), com.redis.audit.entity.AuditActionType.PAYMENT_FAILED, com.redis.audit.entity.AuditStatus.FAILED,
                        com.redis.common.entity.ResourceType.PAYMENT, String.valueOf(saved.getId()), "Payment verification failed: " + e.getMessage());
            }

            try {
                notificationEventPublisher.publishPaymentFailed(saved.getOrder().getUser().getId(), saved.getOrder().getId(), saved.getAmount());
            } catch (Exception ne) {
                log.error("Failed to publish payment failed notification", ne);
            }

            throw e;
        }
    }

    @Override
    @Transactional
    public PaymentResponse cancelPayment(Long userId, Long paymentId) {
        log.info("Cancelling payment ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (!payment.getOrder().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied — order ownership mismatch");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStateException("Cannot cancel payment in state: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        Payment saved = paymentRepository.save(payment);

        // Fail/cancel the corresponding order
        orderService.failOrderPayment(payment.getOrder().getId());

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(userId, saved.getOrder().getUser().getEmail(), com.redis.audit.entity.AuditActionType.PAYMENT_CANCELLED, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.PAYMENT, String.valueOf(saved.getId()), "Payment cancelled successfully");
        }

        try {
            notificationEventPublisher.publishPaymentCancelled(saved.getOrder().getUser().getId(), saved.getOrder().getId());
        } catch (Exception ne) {
            log.error("Failed to publish payment cancelled notification", ne);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse refundPayment(Long userId, Long paymentId, BigDecimal amount, String reason, boolean isAdmin) {
        log.info("Refunding payment ID: {} for amount: {}", paymentId, amount);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (!isAdmin && !payment.getOrder().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied — order ownership mismatch");
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new InvalidPaymentStateException("Cannot refund payment in state: " + payment.getStatus());
        }

        BigDecimal alreadyRefunded = payment.getRefunds().stream()
                .filter(r -> r.getStatus() == RefundStatus.SUCCESS)
                .map(Refund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remainingRefundable = payment.getAmount().subtract(alreadyRefunded);
        if (amount.compareTo(remainingRefundable) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds remaining capture amount: " + remainingRefundable);
        }

        com.redis.payment.service.gateway.PaymentGateway gateway = paymentFactory.getGateway(payment.getPaymentGateway());
        PaymentResponse response = gateway.refundPayment(payment, amount, reason);

        Refund refund = Refund.builder()
                .payment(payment)
                .amount(amount)
                .status(RefundStatus.SUCCESS)
                .reason(reason)
                .gatewayReferenceId(response.getRefunds().get(0).getGatewayReferenceId())
                .build();

        payment.getRefunds().add(refund);

        BigDecimal newRefundTotal = alreadyRefunded.add(amount);
        if (newRefundTotal.compareTo(payment.getAmount()) >= 0) {
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
        }

        if (auditEventPublisher != null) {
            auditEventPublisher.publish(userId, payment.getOrder().getUser().getEmail(), com.redis.audit.entity.AuditActionType.PAYMENT_REFUND, com.redis.audit.entity.AuditStatus.SUCCESS,
                    com.redis.common.entity.ResourceType.PAYMENT, String.valueOf(payment.getId()), "Refund successful. Amount: " + amount + ", Reason: " + reason);
        }

        Payment saved = paymentRepository.save(payment);

        try {
            notificationEventPublisher.publishRefundCompleted(saved.getOrder().getUser().getId(), saved.getId(), amount);
        } catch (Exception ne) {
            log.error("Failed to publish refund completed notification", ne);
        }

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long userId, Long paymentId, boolean isAdmin) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        if (!isAdmin && !payment.getOrder().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied — payment ownership mismatch");
        }

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long userId, Long orderId, boolean isAdmin) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order ID: " + orderId));

        if (!isAdmin && !payment.getOrder().getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Access denied — payment ownership mismatch");
        }

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getMyPayments(Long userId, Pageable pageable) {
        return paymentRepository.findByOrderUserId(userId, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public PaymentResponse retryPayment(Long orderId) {
        log.info("Retrying payment for order ID: {}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        OrderStatus orderStatus = order.getStatus();
        if (orderStatus != OrderStatus.PENDING_PAYMENT && orderStatus != OrderStatus.PAYMENT_FAILED && orderStatus != OrderStatus.EXPIRED) {
            throw new InvalidOrderStateException("Retry not allowed in order state: " + orderStatus);
        }

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment session not found for order ID: " + orderId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStateException("Payment already completed successfully");
        }

        // Re-reserve inventory if previously released (when transition was PAYMENT_FAILED or EXPIRED)
        if (orderStatus == OrderStatus.PAYMENT_FAILED || orderStatus == OrderStatus.EXPIRED) {
            inventoryReservationService.reserveInventory(order);
        }

        // Reset order status back to PENDING_PAYMENT
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        orderRepository.save(order);

        // Reset payment status back to PENDING
        payment.setStatus(PaymentStatus.PENDING);

        com.redis.payment.service.gateway.PaymentGateway gateway = paymentFactory.getGateway(payment.getPaymentGateway());
        PaymentResponse gatewayResponse = gateway.createPaymentSession(payment);

        // Record a new transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .payment(payment)
                .gatewayReferenceId(gatewayResponse.getTransactions().get(0).getGatewayReferenceId())
                .type(PaymentTransactionType.AUTHORIZE)
                .amount(payment.getAmount())
                .build();
        transaction.setCreatedAt(LocalDateTime.now());

        payment.getTransactions().add(transaction);
        Payment saved = paymentRepository.save(payment);

        try {
            notificationEventPublisher.publishRetryPaymentInitiated(saved.getOrder().getUser().getId(), saved.getOrder().getId());
        } catch (Exception ne) {
            log.error("Failed to publish retry payment notification", ne);
        }

        log.info("Payment retry session instantiated successfully for order ID: {}", orderId);
        return toResponse(saved);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .paymentMethod(payment.getPaymentMethod().name())
                .paymentGateway(payment.getPaymentGateway().name())
                .transactions(payment.getTransactions().stream()
                        .map(t -> PaymentTransactionResponse.builder()
                                .id(t.getId())
                                .gatewayReferenceId(t.getGatewayReferenceId())
                                .type(t.getType().name())
                                .amount(t.getAmount())
                                .failureReason(t.getFailureReason())
                                .createdAt(t.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .refunds(payment.getRefunds().stream()
                        .map(r -> RefundResponse.builder()
                                .id(r.getId())
                                .amount(r.getAmount())
                                .status(r.getStatus().name())
                                .reason(r.getReason())
                                .gatewayReferenceId(r.getGatewayReferenceId())
                                .createdAt(r.getCreatedAt())
                                .updatedAt(r.getUpdatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void expirePendingPaymentForOrder(Order order) {
        if (order == null) return;
        orderService.expireOrder(order);
        java.util.Optional<Payment> paymentOpt = paymentRepository.findByOrderId(order.getId());
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.info("Payment associated with order ID: {} marked as FAILED.", order.getId());
            }
        }
    }
}
