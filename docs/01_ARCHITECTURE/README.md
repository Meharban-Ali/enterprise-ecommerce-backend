# ENTERPRISE ARCHITECTURE REFERENCE MANUAL

Welcome to the internal engineering guide for the Spring Boot eCommerce Backend. This manual details the runtime subsystems, execution paths, transactional controls, and reliability frameworks.

## Directory & Reading Sequence

1. **[System Architecture](file:///D:/Meharban_Code/ecommerce/docs/01_ARCHITECTURE/SYSTEM_ARCHITECTURE.md)**
   * Overview of layer responsibilities, execution flows, and request lifecycles.
2. **[Security Architecture](file:///D:/Meharban_Code/ecommerce/docs/01_ARCHITECTURE/SECURITY_ARCHITECTURE.md)**
   * Deep dive into JWT lifecycle, security filters, rate limiting, and RBAC matrix.
3. **[Database Architecture](file:///D:/Meharban_Code/ecommerce/docs/01_ARCHITECTURE/DATABASE_ARCHITECTURE.md)**
   * Entity relationship diagrams, transaction isolation limits, and index strategies.
4. **[Redis Caching Topology](file:///D:/Meharban_Code/ecommerce/docs/01_ARCHITECTURE/REDIS_ARCHITECTURE.md)**
   * Cache strategies, timeout recovery, evictions, and connection recovery.
5. **[Transactional Notification Outbox](file:///D:/Meharban_Code/ecommerce/docs/01_ARCHITECTURE/NOTIFICATION_ARCHITECTURE.md)**
   * Asynchronous event dispatching, outbox schedulers, and failure recovery.
6. **[Webhook Delivery & Webhooks](file:///D:/Meharban_Code/ecommerce/docs/01_ARCHITECTURE/WEBHOOK_ARCHITECTURE.md)**
   * Integrations, HMAC signature validations, and idempotency guarantees.
7. **[Observability & Aspect Tracing](file:///D:/Meharban_Code/ecommerce/docs/01_ARCHITECTURE/OBSERVABILITY_ARCHITECTURE.md)**
   * MDC trace propagation, structured logging schemas, and slow operation warnings.
8. **[System Reliability & Recovery](file:///D:/Meharban_Code/ecommerce/docs/01_ARCHITECTURE/RELIABILITY_ARCHITECTURE.md)**
   * Feature flags, backup schedules, actuator metrics, and graceful tomcat shutdown.
