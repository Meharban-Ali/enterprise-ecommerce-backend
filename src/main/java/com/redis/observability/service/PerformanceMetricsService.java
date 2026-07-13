package com.redis.observability.service;

import com.redis.observability.entity.PerformanceMetric;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PerformanceMetricsService {

    // MetricName -> List of recent latencies
    private final Map<String, Queue<Long>> recentLatencies = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> executionCounts = new ConcurrentHashMap<>();
    
    // Baselines (simple rolling average for demonstration)
    private final Map<String, Long> baselineAverages = new ConcurrentHashMap<>();
    
    // Cached Snapshots for O(1) Dashboard reads
    private volatile Map<String, Map<String, Object>> metricsSnapshotCache = new HashMap<>();

    public void recordMetric(PerformanceMetric metric) {
        String key = metric.getMetricName();
        recentLatencies.putIfAbsent(key, new ConcurrentLinkedQueue<>());
        executionCounts.putIfAbsent(key, new AtomicLong(0));
        
        Queue<Long> q = recentLatencies.get(key);
        q.offer(metric.getDurationMs());
        if (q.size() > 1000) {
            q.poll(); // Keep last 1000 points
        }
        
        executionCounts.get(key).incrementAndGet();
        
        checkBaselineDeviation(key, metric.getDurationMs());
    }

    private void checkBaselineDeviation(String key, long currentDuration) {
        Long baseline = baselineAverages.get(key);
        if (baseline != null && baseline > 0) {
            if (currentDuration > baseline * 3) {
                // 300% slower than baseline
                log.warn("PERFORMANCE_WARNING | Metric={} | Duration={}ms | Baseline={}ms | Deviation > 300%", key, currentDuration, baseline);
            }
        }
    }

    // Called by a background scheduler to compute percentiles periodically
    public void computeSnapshots() {
        Map<String, Map<String, Object>> newSnapshot = new HashMap<>();
        
        recentLatencies.forEach((key, queue) -> {
            List<Long> values = new ArrayList<>(queue);
            Collections.sort(values);
            int size = values.size();
            if (size > 0) {
                long max = values.get(size - 1);
                long p99 = values.get((int) (size * 0.99));
                long p95 = values.get((int) (size * 0.95));
                double avg = values.stream().mapToLong(v -> v).average().orElse(0.0);
                
                // Update baseline
                baselineAverages.put(key, (long) avg);

                Map<String, Object> stats = new HashMap<>();
                stats.put("average", avg);
                stats.put("p95", p95);
                stats.put("p99", p99);
                stats.put("max", max);
                stats.put("count", executionCounts.get(key).get());
                
                newSnapshot.put(key, stats);
            }
        });
        
        metricsSnapshotCache = newSnapshot;
    }

    public Map<String, Map<String, Object>> getMetricsSnapshot() {
        return metricsSnapshotCache;
    }
}
