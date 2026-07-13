# ARCHITECTURE EVOLUTION PLAN

This plan describes how the application's architecture should evolve over the next 1-3 years.

## 1. Evolution Progression

```
[Phase 1: Modular Monolith] ──> [Phase 2: Event-Driven Monolith] ──> [Phase 3: Microservices]
```

* **Modular Monolith (Sprint 14-16)**: Maintain a single deployment unit but strictly separate package scopes and enforce boundary limits between business components.
* **Event-Driven Monolith (Sprint 17-19)**: Decouple database polling by migrating the transactional outbox scheduler to an asynchronous message broker (e.g., Kafka or RabbitMQ).
* **Microservices Architecture (Sprint 20+)**: Extract bounded contexts into independent services (e.g., Catalog service, Orders service, Notifications service) with database-per-service patterns.

---

## 2. Evolution Trade-Offs

| Phase | Advantages | Disadvantages |
| :--- | :--- | :--- |
| **Monolith** | Low complexity, easy deployments, shared transactional database. | Scaling limits, single point of failure. |
| **Microservices**| Independent scaling, isolated deployments, high fault tolerance. | Increased complexity, eventual consistency, distributed tracing required. |
