package com.redis.observability.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class QueueMetricsService {

    private final Map<String, QueueStats> queueStats = new ConcurrentHashMap<>();

    public void recordProcessing(String queueName, long waitTimeMs, long processingTimeMs, boolean success, boolean isRetry) {
        queueStats.putIfAbsent(queueName, new QueueStats());
        QueueStats stats = queueStats.get(queueName);
        
        stats.totalProcessed.incrementAndGet();
        stats.totalWaitTimeMs.addAndGet(waitTimeMs);
        stats.totalProcessingTimeMs.addAndGet(processingTimeMs);
        
        if (!success) {
            stats.failures.incrementAndGet();
        }
        if (isRetry) {
            stats.retries.incrementAndGet();
        }
    }
    
    public void recordDeadLetter(String queueName) {
        queueStats.putIfAbsent(queueName, new QueueStats());
        queueStats.get(queueName).deadLetterCount.incrementAndGet();
    }
    
    public void setQueueDepth(String queueName, long depth) {
        queueStats.putIfAbsent(queueName, new QueueStats());
        queueStats.get(queueName).currentDepth.set(depth);
    }
    
    public Map<String, QueueStats> getQueueStats() {
        return queueStats;
    }

    public static class QueueStats {
        public AtomicLong totalProcessed = new AtomicLong(0);
        public AtomicLong totalWaitTimeMs = new AtomicLong(0);
        public AtomicLong totalProcessingTimeMs = new AtomicLong(0);
        public AtomicLong failures = new AtomicLong(0);
        public AtomicLong retries = new AtomicLong(0);
        public AtomicLong deadLetterCount = new AtomicLong(0);
        public AtomicLong currentDepth = new AtomicLong(0);
        
        public double getFailureRate() {
            long total = totalProcessed.get();
            if (total == 0) return 0.0;
            return ((double) failures.get() / total) * 100.0;
        }
        
        public double getAverageWaitTimeMs() {
            long total = totalProcessed.get();
            if (total == 0) return 0.0;
            return (double) totalWaitTimeMs.get() / total;
        }
        
        public double getAverageProcessingTimeMs() {
            long total = totalProcessed.get();
            if (total == 0) return 0.0;
            return (double) totalProcessingTimeMs.get() / total;
        }
    }
}
