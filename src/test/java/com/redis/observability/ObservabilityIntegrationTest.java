package com.redis.observability;

import com.redis.observability.service.TracingService;

import com.redis.observability.service.QueueMetricsService;
import com.redis.observability.service.RuntimeDiagnosticsService;
import com.redis.observability.entity.TraceSpan;
import com.redis.observability.entity.PerformanceMetric;
import com.redis.observability.entity.SpanType;
import com.redis.observability.entity.TraceContextHolder;
import com.redis.observability.entity.TraceContext;
import com.redis.observability.service.PerformanceMetricsService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ObservabilityIntegrationTest {

    @Autowired
    private TracingService tracingService;
    
    @Autowired
    private PerformanceMetricsService metricsService;
    
    @Autowired
    private RuntimeDiagnosticsService runtimeDiagnosticsService;
    
    @Autowired
    private QueueMetricsService queueMetricsService;

    @Test
    public void testTraceContextAndSpanLifecycle() {
        TraceContextHolder.setContext(TraceContext.builder().traceId("trace-123").build());
        
        TraceSpan span = tracingService.startSpan("TestOperation", SpanType.SERVICE, "TestModule");
        assertNotNull(span.getSpanId());
        assertEquals("trace-123", span.getTraceId());
        assertEquals("STARTED", span.getStatus());
        
        TraceSpan childSpan = tracingService.startSpan("ChildOperation", SpanType.DATABASE, "TestModule");
        assertEquals(span.getSpanId(), childSpan.getParentSpanId());
        
        tracingService.finishSpan(childSpan);
        assertTrue(childSpan.isFinished());
        
        tracingService.finishSpan(span);
        assertTrue(span.isFinished());
        
        TraceContextHolder.clearContext();
    }
    
    @Test
    public void testPerformanceMetricsService() {
        metricsService.recordMetric(PerformanceMetric.builder()
                .metricName("test.metric")
                .durationMs(150)
                .build());
                
        metricsService.computeSnapshots();
        assertNotNull(metricsService.getMetricsSnapshot().get("test.metric"));
    }
    
    @Test
    public void testRuntimeDiagnosticsService() {
        assertNotNull(runtimeDiagnosticsService.getDiagnosticsSnapshot());
    }
    
    @Test
    public void testQueueMetricsService() {
        queueMetricsService.recordProcessing("testQueue", 10, 50, true, false);
        assertEquals(1, queueMetricsService.getQueueStats().get("testQueue").totalProcessed.get());
    }
}