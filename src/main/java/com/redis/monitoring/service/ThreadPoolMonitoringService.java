package com.redis.monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
public class ThreadPoolMonitoringService {

    @Autowired
    private ApplicationContext applicationContext;

    public Map<String, Object> getExecutorMetrics(String beanName) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("activeThreads", 0);
        metrics.put("queueSize", 0);
        metrics.put("completedTasks", 0L);
        metrics.put("maxThreads", 0);
        metrics.put("utilizationPercent", 0.0);

        if (!applicationContext.containsBean(beanName)) {
            return metrics;
        }

        try {
            Object bean = applicationContext.getBean(beanName);
            ThreadPoolExecutor executor = null;

            if (bean instanceof ThreadPoolTaskExecutor) {
                executor = ((ThreadPoolTaskExecutor) bean).getThreadPoolExecutor();
            } else if (bean instanceof ThreadPoolExecutor) {
                executor = (ThreadPoolExecutor) bean;
            }

            if (executor != null) {
                int active = executor.getActiveCount();
                int max = executor.getMaximumPoolSize();
                int qSize = executor.getQueue().size();
                long completed = executor.getCompletedTaskCount();
                double util = max == 0 ? 0.0 : ((double) active / max) * 100.0;

                metrics.put("activeThreads", active);
                metrics.put("queueSize", qSize);
                metrics.put("completedTasks", completed);
                metrics.put("maxThreads", max);
                metrics.put("utilizationPercent", util);
            }
        } catch (Exception e) {
            // Ignore
        }

        return metrics;
    }

    public Map<String, Map<String, Object>> getAllExecutorMetrics() {
        Map<String, Map<String, Object>> all = new HashMap<>();
        List<String> list = Arrays.asList("notificationAsyncExecutor", "auditAsyncExecutor", "webhookExecutor", "schedulerExecutor");
        for (String name : list) {
            all.put(name, getExecutorMetrics(name));
        }
        return all;
    }
}
