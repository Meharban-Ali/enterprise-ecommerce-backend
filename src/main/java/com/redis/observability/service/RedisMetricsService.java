package com.redis.observability.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RedisMetricsService {
    
    private final AtomicLong hits = new AtomicLong(0);
    private final AtomicLong misses = new AtomicLong(0);
    private final AtomicLong evictions = new AtomicLong(0);
    private final AtomicLong connectionFailures = new AtomicLong(0);
    
    private final AtomicLong totalLookupTimeMs = new AtomicLong(0);
    private final AtomicLong totalLookups = new AtomicLong(0);

    public void recordHit(long lookupTimeMs) {
        hits.incrementAndGet();
        totalLookups.incrementAndGet();
        totalLookupTimeMs.addAndGet(lookupTimeMs);
    }
    
    public void recordMiss(long lookupTimeMs) {
        misses.incrementAndGet();
        totalLookups.incrementAndGet();
        totalLookupTimeMs.addAndGet(lookupTimeMs);
    }
    
    public void recordEviction() {
        evictions.incrementAndGet();
    }
    
    public void recordConnectionFailure() {
        connectionFailures.incrementAndGet();
    }
    
    public Map<String, Object> getRedisStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        long totalH = hits.get();
        long totalM = misses.get();
        long totalOps = totalH + totalM;
        
        stats.put("hits", totalH);
        stats.put("misses", totalM);
        stats.put("hitRatio", totalOps > 0 ? ((double) totalH / totalOps) * 100 : 0.0);
        stats.put("missRatio", totalOps > 0 ? ((double) totalM / totalOps) * 100 : 0.0);
        stats.put("evictions", evictions.get());
        stats.put("connectionFailures", connectionFailures.get());
        
        long ops = totalLookups.get();
        stats.put("averageLookupTimeMs", ops > 0 ? totalLookupTimeMs.get() / ops : 0);
        
        return stats;
    }
}
