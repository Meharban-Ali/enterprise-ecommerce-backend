# MICROSERVICES STRATEGY

This document outlines the strategy for migrating from a modular monolith to a microservices architecture.

## 1. Bounded Contexts Extrications
The application can be separated into the following microservices:
* **Identity Service**: Handles user registration, logins, and JWT token management.
* **Catalog Service**: Handles products catalog listings, categories, and inventory stock updates.
* **Order Service**: Handles shopping carts, checkouts, and order status transitions.
* **Notification Service**: Listens for domain events and dispatches customer emails.

---

## 2. Decoupling Rules & Communication
* **Database per Service**: Each service must own its relational database instance. Services can only access other domains data using REST APIs or event streams.
* **Event-Driven Communication**: Use Kafka to broadcast events (e.g., `OrderPlacedEvent`) asynchronously.
* **API Gateway**: Use Spring Cloud Gateway as a single entry point to handle client routing and rate limiting.
