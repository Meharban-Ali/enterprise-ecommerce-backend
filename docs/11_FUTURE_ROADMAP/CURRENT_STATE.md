# CURRENT STATE ASSESSMENT

This document describes the current architecture, technology stack, and limitations of the application.

## 1. Tech Stack Overview
* **Core Framework**: Spring Boot 3.x
* **Database**: MySQL 8.0 / Hibernate ORM
* **Caching**: Redis Cache / RedisTemplate
* **Security**: Spring Security / JWT (HS256)
* **Build Tool**: Maven

---

## 2. Strengths
* **Highly Optimized Database Queries**: Eager loading strategies prevent N+1 query problems.
* **Resilient Cache Layer**: Fallback error handlers catch Redis timeouts and fall back to SQL queries.
* **Optimistic Locking**: Prevents race conditions on concurrent stock updates.
* **Outbox Pattern**: Transactional outbox table ensures eventual consistency for email notifications.

---

## 3. Limitations
* **Single-Node Schedulers**: The background outbox scheduler lacks a distributed lock (ShedLock). Deploying multiple application replicas will cause duplicate scheduler runs.
* **Manual Schema Migrations**: The database relies on Hibernate's `ddl-auto=update` setting. It lacks versioned schema migration scripts (e.g., Flyway).
