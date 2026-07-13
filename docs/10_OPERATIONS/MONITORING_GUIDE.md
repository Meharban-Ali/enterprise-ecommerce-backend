# SYSTEM MONITORING GUIDE

This guide explains how to monitor application health, database connections, and cache metrics.

## 1. Spring Actuator Checkpoints
Monitor application state using these actuator endpoints:
* **System Health**: `GET /actuator/health` (Returns database and Redis connection health).
* **Application Metrics**: `GET /actuator/metrics` (Exposes JVM, thread pool, and database connection pool metrics).

---

## 2. Prometheus Integration
Actuator metrics are formatted for Prometheus scrapers at:
```
GET /actuator/prometheus
```

---

## 3. Slow Operations Alerting
An AOP aspect intercepts execution threads and generates warning alerts in application logs for service operations taking longer than 500ms.
