package com.redis.monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.lang.management.*;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class JvmRuntimeMonitoringService {

    public Map<String, Object> getJvmMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memory.getHeapMemoryUsage();
        MemoryUsage nonHeap = memory.getNonHeapMemoryUsage();

        metrics.put("heapUsedBytes", heap.getUsed());
        metrics.put("heapMaxBytes", heap.getMax());
        metrics.put("heapCommittedBytes", heap.getCommitted());
        metrics.put("nonHeapUsedBytes", nonHeap.getUsed());

        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        metrics.put("threadCount", threads.getThreadCount());
        metrics.put("peakThreadCount", threads.getPeakThreadCount());
        metrics.put("daemonThreadCount", threads.getDaemonThreadCount());

        long[] threadIds = threads.getAllThreadIds();
        int runnable = 0, blocked = 0, waiting = 0, timedWaiting = 0;
        for (long id : threadIds) {
            ThreadInfo info = threads.getThreadInfo(id);
            if (info != null && info.getThreadState() != null) {
                switch (info.getThreadState()) {
                    case RUNNABLE -> runnable++;
                    case BLOCKED -> blocked++;
                    case WAITING -> waiting++;
                    case TIMED_WAITING -> timedWaiting++;
                }
            }
        }
        metrics.put("threadsRunnable", runnable);
        metrics.put("threadsBlocked", blocked);
        metrics.put("threadsWaiting", waiting);
        metrics.put("threadsTimedWaiting", timedWaiting);

        long gcCount = 0;
        long gcTimeMs = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            gcCount += Math.max(0, gc.getCollectionCount());
            gcTimeMs += Math.max(0, gc.getCollectionTime());
        }
        metrics.put("gcCollectionCount", gcCount);
        metrics.put("gcCollectionTimeMs", gcTimeMs);

        ClassLoadingMXBean classes = ManagementFactory.getClassLoadingMXBean();
        metrics.put("classesLoaded", classes.getLoadedClassCount());
        metrics.put("classesUnloaded", classes.getUnloadedClassCount());
        metrics.put("classesTotalLoaded", classes.getTotalLoadedClassCount());

        return metrics;
    }
}
