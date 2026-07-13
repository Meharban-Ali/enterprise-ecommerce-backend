# LOGGING STANDARDS & AUDITING

This guide explains logging standards, correlation tracing, and log retention policies.

## 1. Structured JSON Logging Schema
The application writes log statements in a structured JSON format to simplify log aggregation:
```json
{
  "timestamp": "2026-07-13T09:12:06Z",
  "level": "INFO",
  "traceId": "ada3d526-a875-...",
  "endpoint": "/api/orders",
  "durationMs": 14
}
```

---

## 2. Correlation ID Propagation
Every incoming HTTP request is intercepted to inject a unique `X-Correlation-ID` into the ThreadLocal MDC context. This trace ID is automatically propagated to all log statements generated during the request execution lifecycle.

---

## 3. Log Retention Policies
* **Staging Logs**: Retained for 7 days.
* **Production Logs**: Retained for 30 days in log aggregation tools before being archived to persistent storage.
