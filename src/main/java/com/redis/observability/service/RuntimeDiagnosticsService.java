package com.redis.observability.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class RuntimeDiagnosticsService {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    
    private final AtomicInteger consecutiveHighMemoryChecks = new AtomicInteger(0);

    @Scheduled(fixedRate = 60000)
    public void diagnoseRuntime() {
        // Thread Diagnostics
        int threadCount = threadMXBean.getThreadCount();
        int peakThreadCount = threadMXBean.getPeakThreadCount();
        
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        if (deadlockedThreads != null && deadlockedThreads.length > 0) {
            log.error("CRITICAL_INCIDENT | DEADLOCK_DETECTED | Threads involved: {}", deadlockedThreads.length);
        }

        int blockedCount = 0;
        int waitingCount = 0;
        for (ThreadInfo info : threadMXBean.dumpAllThreads(false, false)) {
            if (info.getThreadState() == Thread.State.BLOCKED) blockedCount++;
            if (info.getThreadState() == Thread.State.WAITING || info.getThreadState() == Thread.State.TIMED_WAITING) waitingCount++;
        }
        
        if (blockedCount > 50) {
            log.warn("WARNING_INCIDENT | THREAD_WARNING | Blocked threads exceeded threshold: {}", blockedCount);
        }

        // Memory Leak Detection (Basic)
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        double usedPercentage = (double) heapUsage.getUsed() / heapUsage.getMax();
        
        if (usedPercentage > 0.85) {
            int count = consecutiveHighMemoryChecks.incrementAndGet();
            if (count > 5) {
                log.error("CRITICAL_INCIDENT | HIGH_MEMORY_LEAK_WARNING | Heap usage constantly > 85% for 5+ checks.");
            }
        } else {
            consecutiveHighMemoryChecks.set(0);
        }
    }

    public Map<String, Object> getDiagnosticsSnapshot() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("threadCount", threadMXBean.getThreadCount());
        stats.put("peakThreadCount", threadMXBean.getPeakThreadCount());
        
        int blocked = 0;
        int waiting = 0;
        for (ThreadInfo info : threadMXBean.dumpAllThreads(false, false)) {
            if (info.getThreadState() == Thread.State.BLOCKED) blocked++;
            if (info.getThreadState() == Thread.State.WAITING || info.getThreadState() == Thread.State.TIMED_WAITING) waiting++;
        }
        
        stats.put("blockedThreads", blocked);
        stats.put("waitingThreads", waiting);
        
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        stats.put("heapUsedMb", heap.getUsed() / (1024 * 1024));
        stats.put("heapMaxMb", heap.getMax() / (1024 * 1024));
        
        return stats;
    }
}
