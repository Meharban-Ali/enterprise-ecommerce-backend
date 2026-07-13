package com.redis.notification.entity;

import com.redis.notification.service.NotificationOutboxService;
import com.redis.monitoring.service.SystemMonitoringService;

import com.redis.infrastructure.config.NotificationProperties;
import com.redis.notification.entity.NotificationOutbox;
import com.redis.common.entity.OutboxStatus;
import com.redis.reliability.dto.SchedulerStatusResponse;
import com.redis.monitoring.event.MonitoringEvent;
import com.redis.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxScheduler {

    private final NotificationOutboxService outboxService;
    private final NotificationOutboxRepository outboxRepository;
    private final NotificationProperties properties;
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

    @Scheduled(fixedDelayString = "${app.notification.outbox-delay-ms:2000}")
    public void processOutbox() {
        long start = System.currentTimeMillis();
        executionCount.incrementAndGet();
        lastExecutionTime.set(LocalDateTime.now());

        if (!properties.isOutboxEnabled()) {
            long duration = System.currentTimeMillis() - start;
            lastExecutionDurationMs.set(duration);
            totalDurationMs.addAndGet(duration);
            updateTimeStats(duration);
            lastSuccessfulExecution.set(LocalDateTime.now());
            return;
        }

        log.debug("Outbox scheduler checking for pending events...");
        boolean failed = false;
        try {
            List<NotificationOutbox> pending = outboxService.getPendingEvents(properties.getOutboxBatchSize());
            if (pending.isEmpty()) {
                long duration = System.currentTimeMillis() - start;
                lastExecutionDurationMs.set(duration);
                totalDurationMs.addAndGet(duration);
                updateTimeStats(duration);
                lastSuccessfulExecution.set(LocalDateTime.now());
                return;
            }

            List<NotificationOutbox> claimed = new ArrayList<>();
            for (NotificationOutbox event : pending) {
                try {
                    int rows = outboxRepository.claimEvent(event.getId());
                    if (rows > 0) {
                        event.setStatus(OutboxStatus.PROCESSING);
                        claimed.add(event);
                    }
                } catch (Exception e) {
                    log.warn("Failed to claim outbox event ID {}: {}", event.getId(), e.getMessage());
                    registerError(e);
                }
            }

            if (!claimed.isEmpty()) {
                totalProcessedRecords.addAndGet(claimed.size());
                outboxService.processOutboxBatch(claimed);
            }
            lastSuccessfulExecution.set(LocalDateTime.now());
        } catch (Exception e) {
            failed = true;
            failuresCount.incrementAndGet();
            lastFailureTime.set(LocalDateTime.now());
            lastFailedExecution.set(LocalDateTime.now());
            registerError(e);
            eventPublisher.publishEvent(new MonitoringEvent(this, "SCHEDULER_FAILURE", "NotificationOutboxScheduler", e.getMessage()));
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
            service.registerError("NotificationOutboxScheduler", e);
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
                .schedulerName("NotificationOutboxScheduler")
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
