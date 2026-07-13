package com.redis.payment.controller;

import com.redis.payment.entity.Payment;
import com.redis.payment.entity.Refund;

import com.redis.payment.dto.request.CreatePaymentRequest;
import com.redis.payment.dto.request.RefundRequest;
import com.redis.common.dto.ApiResponse;
import com.redis.payment.dto.response.PaymentResponse;
import com.redis.user.entity.User;
import com.redis.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreatePaymentRequest request) {
        log.info("API POST /api/payments/create — Create payment for order: {}", request.getOrderId());
        PaymentResponse response = paymentService.createPayment(user.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment session created successfully", response));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @AuthenticationPrincipal User user,
            @PathVariable Long paymentId) {
        log.info("API GET /api/payments/{} — Fetch payment", paymentId);
        boolean isAdmin = user.getRole().name().equals("ROLE_ADMIN") || user.getRole().name().equals("ROLE_SUPER_ADMIN");
        PaymentResponse response = paymentService.getPaymentById(user.getId(), paymentId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", response));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByOrderId(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        log.info("API GET /api/payments/order/{} — Fetch payment for order", orderId);
        boolean isAdmin = user.getRole().name().equals("ROLE_ADMIN") || user.getRole().name().equals("ROLE_SUPER_ADMIN");
        PaymentResponse response = paymentService.getPaymentByOrderId(user.getId(), orderId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getMyPayments(
            @AuthenticationPrincipal User user,
            Pageable pageable) {
        log.info("API GET /api/payments/my — Fetch my payments");
        Page<PaymentResponse> response = paymentService.getMyPayments(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Payments retrieved successfully", response));
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long paymentId) {
        log.info("API POST /api/payments/{}/cancel — Cancel payment", paymentId);
        PaymentResponse response = paymentService.cancelPayment(user.getId(), paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment cancelled successfully", response));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long paymentId,
            @Valid @RequestBody RefundRequest request) {
        log.info("API POST /api/payments/{}/refund — Request refund", paymentId);
        boolean isAdmin = user.getRole().name().equals("ROLE_ADMIN") || user.getRole().name().equals("ROLE_SUPER_ADMIN");
        PaymentResponse response = paymentService.refundPayment(user.getId(), paymentId, request.getAmount(), request.getReason(), isAdmin);
        return ResponseEntity.ok(ApiResponse.success("Refund processed successfully", response));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getAllPayments(Pageable pageable) {
        log.info("API GET /api/payments/admin — Fetch all payments");
        Page<PaymentResponse> response = paymentService.getAllPayments(pageable);
        return ResponseEntity.ok(ApiResponse.success("All payments retrieved successfully", response));
    }

    @PostMapping("/order/{orderId}/retry")
    public ResponseEntity<ApiResponse<PaymentResponse>> retryPayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        log.info("API POST /api/payments/order/{}/retry — Retry payment", orderId);
        PaymentResponse response = paymentService.retryPayment(orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment retry session created successfully", response));
    }
}
