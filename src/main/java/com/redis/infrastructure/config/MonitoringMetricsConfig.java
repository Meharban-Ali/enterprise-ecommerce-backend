package com.redis.infrastructure.config;

import com.redis.notification.entity.NotificationStatus;
import com.redis.payment.entity.PaymentStatus;
import com.redis.audit.repository.AuditLogRepository;
import com.redis.order.repository.OrderRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class MonitoringMetricsConfig {

    private final MeterRegistry meterRegistry;
    private final NotificationRepository notificationRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final AuditLogRepository auditLogRepository;

    @PostConstruct
    public void initMetrics() {
        Gauge.builder("notification.sent.count", notificationRepository,
                repo -> repo.countByStatus(NotificationStatus.SENT)).register(meterRegistry);

        Gauge.builder("notification.failed.count", notificationRepository,
                repo -> repo.countByStatus(NotificationStatus.FAILED)).register(meterRegistry);

        Gauge.builder("notification.retry.count", notificationRepository,
                repo -> repo.countByStatus(NotificationStatus.PENDING)).register(meterRegistry);

        Gauge.builder("notification.deadletter.count", notificationRepository,
                repo -> repo.countByStatus(NotificationStatus.DEAD_LETTER)).register(meterRegistry);

        Gauge.builder("payment.success.count", paymentRepository,
                repo -> repo.countByStatus(PaymentStatus.SUCCESS)).register(meterRegistry);

        Gauge.builder("payment.failure.count", paymentRepository,
                repo -> repo.countByStatus(PaymentStatus.FAILED)).register(meterRegistry);

        Gauge.builder("order.created.count", orderRepository,
                repo -> repo.count()).register(meterRegistry);

        Gauge.builder("audit.events.count", auditLogRepository,
                repo -> repo.count()).register(meterRegistry);
    }
}
