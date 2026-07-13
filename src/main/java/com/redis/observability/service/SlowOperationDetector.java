package com.redis.observability.service;

import com.redis.observability.entity.TraceSpan;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SlowOperationDetector {

    public void analyzeSpan(TraceSpan span) {
        long duration = span.getTotalTimeMs();
        boolean slow = false;

        switch (span.getType()) {
            case HTTP_REQUEST:
                slow = duration > 1000;
                break;
            case DATABASE:
                slow = duration > 500;
                break;
            case SERVICE:
                slow = duration > 1500;
                break;
            case QUEUE:
                slow = duration > 5000;
                break;
            case WEBHOOK:
                slow = duration > 3000;
                break;
            case SCHEDULER:
                slow = duration > 60000; // 1 minute
                break;
            case NOTIFICATION:
                slow = duration > 2000;
                break;
            case PAYMENT:
                slow = duration > 3000;
                break;
            case CACHE:
                slow = duration > 100;
                break;
            default:
                break;
        }

        if (slow) {
            log.warn("SLOW_OPERATION_DETECTED | Type={} | Operation={} | Duration={}ms | TraceId={}", 
                span.getType(), span.getOperationName(), duration, span.getTraceId());
            
            // Note: Incident integration will be bound to these logs or events directly.
        }
    }
}
