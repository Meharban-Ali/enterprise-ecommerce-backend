# ENGINEERING TIMELINE

This timeline outlines the roadmap for future sprints.

## 1. Timeline Roadmap

```
[Sprint 14: Manual QA Audit] ──> [Sprint 15: Fix Blocker Debts] ──> [Sprint 16: Staging Releases]
                                                                        │
[Sprint 19: microservices] <── [Sprint 18: Kafka integrations] <── [Sprint 17: Production Release]
```

* **Sprint 14: Manual QA Audit**: Execute Postman verification scripts, register test coverage results, and patch minor defects.
* **Sprint 15: Technical Debt Resolution**: Integrate Flyway database migrations and ShedLock distributed scheduler locking. Hardened Docker container execution roles.
* **Sprint 16: Staging Release**: Deploy version 1.0.0-RC1 to staging, verify log metrics, and check alerts.
* **Sprint 17: Production Release**: Deploy stable release candidate to production, enable CORS/HSTS, and monitor systems.
* **Sprint 18: Kafka Integrations**: Replace database outbox polling with event-driven Kafka message streaming.
* **Sprint 19: Microservices Extraction**: Decouple domain packages into separate microservice repositories.
