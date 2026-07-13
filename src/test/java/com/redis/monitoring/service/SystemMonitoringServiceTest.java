package com.redis.monitoring.service;

import com.redis.observability.dto.response.JvmMetricsResponse;
import com.redis.observability.dto.response.SystemMetricsResponse;
import com.redis.reliability.dto.RecentSystemErrorResponse;
import com.redis.reliability.dto.SystemInfoResponse;

import com.redis.infrastructure.config.TestRedisConfig;
import com.redis.infrastructure.config.MonitoringProperties;
import com.redis.monitoring.entity.DatabaseHealthIndicator;
import com.redis.monitoring.service.HealthIndicatorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public class SystemMonitoringServiceTest {

    @Autowired
    private SystemMonitoringService monitoringService;

    @Autowired
    private MonitoringProperties monitoringProperties;

    @Test
    void testSystemInfoResponse() {
        SystemInfoResponse info = monitoringService.getSystemInfo();
        assertNotNull(info);
        assertEquals("E-Commerce Application", info.getApplicationName());
        assertEquals("1.0.0", info.getApplicationVersion());
        assertTrue(info.getAvailableCpuCores() > 0);
        assertTrue(info.getApplicationUptimeSeconds() >= 0);
    }

    @Test
    void testJvmMetrics() {
        JvmMetricsResponse jvm = monitoringService.getJvmMetrics();
        assertNotNull(jvm);
        assertTrue(jvm.getHeapMaxBytes() > 0);
        assertTrue(jvm.getHeapUsedBytes() > 0);
        assertTrue(jvm.getThreadCount() > 0);
    }

    @Test
    void testMonitoringProperties() {
        assertNotNull(monitoringProperties);
        assertEquals(30, monitoringProperties.getDashboardCacheSeconds());
        assertEquals(30, monitoringProperties.getHealthCacheSeconds());
        assertEquals(2000, monitoringProperties.getMonitoringTimeoutMs());
    }

    @Test
    void testMetricsCaching() {
        // First retrieval
        SystemMetricsResponse metrics1 = monitoringService.getSystemMetrics();
        assertNotNull(metrics1);

        // Second retrieval should fetch from cache
        SystemMetricsResponse metrics2 = monitoringService.getSystemMetrics();
        assertSame(metrics1, metrics2); // Cached reference matches
    }

    @Test
    void testRecentErrorsBuffer() {
        RuntimeException ex = new RuntimeException("Test failure message");
        monitoringService.registerError("TestScheduler", ex);

        List<RecentSystemErrorResponse> errors = monitoringService.getRecentErrors();
        assertFalse(errors.isEmpty());
        RecentSystemErrorResponse recent = errors.get(0);
        assertEquals("TestScheduler", recent.getComponent());
        assertEquals("RuntimeException", recent.getErrorType());
        assertEquals("Test failure message", recent.getMessage());
    }
}
