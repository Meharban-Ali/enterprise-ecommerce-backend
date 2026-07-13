# 07. Monitoring & Observability Guide

## 1. Actuator Indicators
* Health checks scraped on: `/actuator/health`.
* Database connection pool and heap metrics monitored on: `/actuator/metrics`.

## 2. MDC Observability
* Correlation Tracing: Requests filters inject trace variables into the MDC map.
* Aspect Tracing: Slow operations aspect prints warning logs on methods taking > 500ms.
