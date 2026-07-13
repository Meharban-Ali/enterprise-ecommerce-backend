package com.redis.observability.entity;

import java.util.concurrent.Executor;

public class TraceContextExecutorWrapper implements Executor {
    private final Executor delegate;

    public TraceContextExecutorWrapper(Executor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable command) {
        TraceContext context = TraceContextHolder.getContext();
        final TraceContext contextSnapshot = (context != null) ? context.cloneContext() : null;

        delegate.execute(() -> {
            try {
                TraceContextHolder.setContext(contextSnapshot);
                command.run();
            } finally {
                TraceContextHolder.clearContext();
            }
        });
    }
}
