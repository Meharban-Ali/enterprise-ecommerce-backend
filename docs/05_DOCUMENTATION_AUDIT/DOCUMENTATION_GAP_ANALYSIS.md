# DOCUMENTATION GAP ANALYSIS

This document identifies gaps in the current documentation and provides recommendations for future updates.

## 1. Missing Features & Future Documentation
The following planned features are not yet implemented in the codebase and should be documented once developed:
* **Flyway Migrations**: Document schema SQL versioning scripts when Hibernate `ddl-auto=update` is replaced.
* **ShedLock Scheduler Locks**: Document ShedLock table mappings and lock annotations.
* **Kafka/RabbitMQ Broker**: Document event-driven messaging topologies when transactional outboxes are replaced.

---

## 2. Redundancy Review
All duplicate legacy documentation directories—including `api-docs/`, `developer-guide/`, `testing/`, and `operations/`—have been removed from the repository root.

The folder structure under `docs/` is now the single source of truth, with no duplicate or conflicting documents.
