package com.redis.infrastructure.security;

import com.redis.observability.service.TracingService;

import com.redis.observability.entity.TraceContext;
import com.redis.observability.entity.TraceContextHolder;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceContextFilter implements Filter {

    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String SPAN_ID_HEADER = "X-Span-ID";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
            if (traceId == null || traceId.trim().isEmpty()) {
                traceId = UUID.randomUUID().toString();
            }

            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = traceId; // By default, use traceId as correlationId if missing
            }
            
            String parentSpanId = httpRequest.getHeader(SPAN_ID_HEADER);
            // We'll generate a root spanId later via TracingService, but filter establishes context
            String spanId = UUID.randomUUID().toString();

            TraceContext context = TraceContext.builder()
                    .traceId(traceId)
                    .spanId(spanId)
                    .parentSpanId(parentSpanId)
                    .correlationId(correlationId)
                    .endpoint(httpRequest.getRequestURI())
                    .module("API")
                    .build();

            TraceContextHolder.setContext(context);

            httpResponse.setHeader(TRACE_ID_HEADER, traceId);
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);
            httpResponse.setHeader(SPAN_ID_HEADER, spanId);
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TraceContextHolder.clearContext();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}