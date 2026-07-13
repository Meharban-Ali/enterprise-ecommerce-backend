# ARCHITECTURAL DECISION RECORDS (ADR)

This document records the major architectural decisions and trade-offs.

---

## ADR 01: Layered Architecture
* **Status**: **ACCEPTED**
* **Context**: The application requires clear separation of concerns to support rapid development and clean code.
* **Decision**: Adopt a layered architecture pattern: Controllers -> Services -> Repositories -> Entities.
* **Alternatives Considered**: Clean / Hexagonal Architecture (deferred due to increased code overhead).
* **Advantages**: Simple, familiar pattern, easy to onboard developers.
* **Disadvantages**: Can lead to database-centric designs.

---

## ADR 02: Transactional Outbox Pattern for Notifications
* **Status**: **ACCEPTED**
* **Context**: Sending emails directly during checkout requests blocks database transactions and introduces network failure risks.
* **Decision**: Write notification events to a local outbox table within the checkout transaction block, then dispatch emails asynchronously using a background scheduler.
* **Alternatives Considered**: Direct SMTP API dispatch (rejected due to blocking risks).
* **Advantages**: High checkout reliability, eventual consistency.
* **Disadvantages**: Introduces database outbox polling overhead.
