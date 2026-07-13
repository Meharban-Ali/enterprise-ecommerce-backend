package com.redis.payment.entity;

import com.redis.monitoring.service.SystemMonitoringService;
import com.redis.order.service.OrderService;

import com.redis.order.entity.Order;
import com.redis.payment.entity.Payment;
import com.redis.order.entity.OrderStatus;
import com.redis.payment.entity.PaymentStatus;
import com.redis.reliability.dto.SchedulerStatusResponse;
import com.redis.monitoring.event.MonitoringEvent;
import com.redis.order.repository.OrderRepository;
import com.redis.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpirationScheduler {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    private ObjectProvider<SystemMonitoringService> monitoringServiceProvider;

    private final AtomicLong executionCount = new AtomicLong(0);
    private final AtomicLong failuresCount = new AtomicLong(0);
    private final AtomicLong totalDurationMs = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastExecutionTime = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> lastFailureTime = new AtomicReference<>();
    private final AtomicLong lastExecutionDurationMs = new AtomicLong(0);

    // Sprint 9.1 metrics
    private final AtomicLong minExecutionTimeMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxExecutionTimeMs = new AtomicLong(0);
    private final AtomicReference<LocalDateTime> lastSuccessfulExecution = new AtomicReference<>();
    private final AtomicReference<LocalDateTime> lastFailedExecution = new AtomicReference<>();
    private final AtomicLong totalProcessedRecords = new AtomicLong(0);

    @Scheduled(cron = "0 */1 * * * *")
    @Transactional
    public void expirePendingPayments() {
        long start = System.currentTimeMillis();
        executionCount.incrementAndGet();
        lastExecutionTime.set(LocalDateTime.now());
        log.info("Running PaymentExpirationScheduler scan...");

        boolean failed = false;
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(15);
            List<Order> expiredOrders = orderRepository.findByStatusAndOrderDateBefore(
                    OrderStatus.PENDING_PAYMENT, threshold);

            if (expiredOrders.isEmpty()) {
                log.info("No expired pending-payment orders found.");
                lastExecutionDurationMs.set(System.currentTimeMillis() - start);
                totalDurationMs.addAndGet(lastExecutionDurationMs.get());
                updateTimeStats(lastExecutionDurationMs.get());
                lastSuccessfulExecution.set(LocalDateTime.now());
                return;
            }

            log.info("Found {} expired pending-payment orders. Processing expiration...", expiredOrders.size());
            totalProcessedRecords.addAndGet(expiredOrders.size());

            for (Order order : expiredOrders) {
                try {
                    orderService.expireOrder(order.getId());
                    Optional<Payment> paymentOpt = paymentRepository.findByOrderId(order.getId());
                    if (paymentOpt.isPresent()) {
                        Payment payment = paymentOpt.get();
                        if (payment.getStatus() == PaymentStatus.PENDING) {
                            payment.setStatus(PaymentStatus.FAILED);
                            paymentRepository.save(payment);
                        }
                    }
                    log.info("Expired order ID {} successfully processed.", order.getId());
                } catch (Exception e) {
                    log.error("Error processing expiration for order ID {}: {}", order.getId(), e.getMessage(), e);
                    registerError(e);
                }
            }
            lastSuccessfulExecution.set(LocalDateTime.now());
        } catch (Exception e) {
            failed = true;
            failuresCount.incrementAndGet();
            lastFailureTime.set(LocalDateTime.now());
            lastFailedExecution.set(LocalDateTime.now());
            registerError(e);
            eventPublisher.publishEvent(new MonitoringEvent(this, "SCHEDULER_FAILURE", "PaymentExpirationScheduler", e.getMessage()));
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            lastExecutionDurationMs.set(duration);
            totalDurationMs.addAndGet(duration);
            updateTimeStats(duration);
            if (!failed) {
                lastSuccessfulExecution.set(LocalDateTime.now());
            }
        }
    }

    private void updateTimeStats(long duration) {
        minExecutionTimeMs.updateAndGet(val -> val == Long.MAX_VALUE ? duration : Math.min(val, duration));
        maxExecutionTimeMs.updateAndGet(val -> Math.max(val, duration));
    }

    private void registerError(Throwable e) {
        SystemMonitoringService service = monitoringServiceProvider.getIfAvailable();
        if (service != null) {
            service.registerError("PaymentExpirationScheduler", e);
        }
    }

    public SchedulerStatusResponse getStatusDetails() {
        long runs = executionCount.get();
        long fails = failuresCount.get();
        double successRate = runs > 0 ? ((double) (runs - fails) / runs) * 100.0 : 100.0;
        double avgTime = runs > 0 ? (double) totalDurationMs.get() / runs : 0.0;

        String status = "IDLE";
        if (lastExecutionTime.get() != null) {
            if (lastExecutionTime.get().isAfter(LocalDateTime.now().minusMinutes(5))) {
                status = "UP";
            }
        }
        if (fails > 0 && lastFailureTime.get() != null && lastFailureTime.get().isAfter(LocalDateTime.now().minusMinutes(5))) {
            status = "WARNING";
        }

        long minVal = minExecutionTimeMs.get();
        return SchedulerStatusResponse.builder()
                .schedulerName("PaymentExpirationScheduler")
                .lastExecutionTime(lastExecutionTime.get())
                .lastExecutionDurationMs(lastExecutionDurationMs.get())
                .averageExecutionTimeMs(avgTime)
                .executionCount(runs)
                .failuresCount(fails)
                .successRate(successRate)
                .lastFailureTime(lastFailureTime.get())
                .status(status)
                .minExecutionTimeMs(minVal == Long.MAX_VALUE ? 0L : minVal)
                .maxExecutionTimeMs(maxExecutionTimeMs.get())
                .lastSuccessfulExecution(lastSuccessfulExecution.get())
                .lastFailedExecution(lastFailedExecution.get())
                .successPercentage(successRate)
                .failurePercentage(runs > 0 ? ((double) fails / runs) * 100.0 : 0.0)
                .totalProcessedRecords(totalProcessedRecords.get())
                .build();
    }
}
