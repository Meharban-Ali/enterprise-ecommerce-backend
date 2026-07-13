# ENGINEERING ROADMAP

This roadmap outlines the project's milestones, current focus areas, and planned improvements.

## 1. Completed Milestones
* **Core API Framework**: Implemented catalog, cart, checkout, payment, and notification modules.
* **Stateless Session Security**: Integrated JWT token validation and Redis token blacklisting.
* **Observability Aspect**: Added structured logging, trace ID injection, and execution time tracking.

## 2. In Progress (Active Initiatives)
* **Configuration Standardization**: Parameterizing profile settings in `.env` and properties templates.
* **API Documentation**: Hardening manual Postman collections and developer handbooks.

## 3. Planned Improvements (Next Sprints)
* **Database Versioning (Flyway)**: Integrate migration scripts to replace Hibernate `ddl-auto=update`.
* **Clustered Schedulers (ShedLock)**: Prevent concurrent task runs on multi-replica deployments.

## 4. Future Roadmap
* **Event-Driven Messaging**: Integrate Kafka or RabbitMQ to decouple transactional outboxes.
* **Observability (OpenTelemetry)**: Implement distributed tracing to monitor service latency.
* **Kubernetes Orchestration**: Create Helm charts for rolling updates and autoscale controls.
