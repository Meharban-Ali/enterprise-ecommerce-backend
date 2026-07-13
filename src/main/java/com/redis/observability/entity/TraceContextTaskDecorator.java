package com.redis.observability.entity;

import org.springframework.core.task.TaskDecorator;
import org.springframework.lang.NonNull;

public class TraceContextTaskDecorator implements TaskDecorator {
    @Override
    @NonNull
    public Runnable decorate(@NonNull Runnable runnable) {
        TraceContext context = TraceContextHolder.getContext();
        // Capture context at submission time, clone it if exists
        final TraceContext contextSnapshot = (context != null) ? context.cloneContext() : null;

        return () -> {
            try {
                TraceContextHolder.setContext(contextSnapshot);
                runnable.run();
            } finally {
                TraceContextHolder.clearContext();
            }
        };
    }
}
