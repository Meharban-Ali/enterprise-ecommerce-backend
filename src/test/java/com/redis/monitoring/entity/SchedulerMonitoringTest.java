package com.redis.monitoring.entity;

import com.redis.notification.entity.NotificationRetryScheduler;
import com.redis.payment.entity.PaymentExpirationScheduler;
import com.redis.notification.entity.NotificationOutboxScheduler;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.reliability.dto.SchedulerStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class SchedulerMonitoringTest {

    @Autowired
    private PaymentExpirationScheduler paymentExpirationScheduler;

    @Autowired
    private NotificationRetryScheduler notificationRetryScheduler;

    @Autowired
    private NotificationOutboxScheduler notificationOutboxScheduler;

    @Test
    void testSchedulersCollectExecutionMetrics() {
        // Trigger payment scan manually to verify run counts
        paymentExpirationScheduler.expirePendingPayments();

        SchedulerStatusResponse paymentStatus = paymentExpirationScheduler.getStatusDetails();
        assertNotNull(paymentStatus);
        assertEquals("PaymentExpirationScheduler", paymentStatus.getSchedulerName());
        assertTrue(paymentStatus.getExecutionCount() >= 1);
        assertEquals(100.0, paymentStatus.getSuccessRate());

        // Trigger notification retry manually
        notificationRetryScheduler.processPendingRetries();

        SchedulerStatusResponse retryStatus = notificationRetryScheduler.getStatusDetails();
        assertNotNull(retryStatus);
        assertEquals("NotificationRetryScheduler", retryStatus.getSchedulerName());
        assertTrue(retryStatus.getExecutionCount() >= 1);

        // Trigger outbox manually
        notificationOutboxScheduler.processOutbox();

        SchedulerStatusResponse outboxStatus = notificationOutboxScheduler.getStatusDetails();
        assertNotNull(outboxStatus);
        assertEquals("NotificationOutboxScheduler", outboxStatus.getSchedulerName());
        assertTrue(outboxStatus.getExecutionCount() >= 1);
    }
}
