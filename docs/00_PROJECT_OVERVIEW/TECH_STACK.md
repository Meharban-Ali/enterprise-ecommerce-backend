# TECHNOLOGY STACK ANALYSIS

This document details the technologies used in the project, their purpose, and their runtime implementation.

## Core Runtime Engine

### Java 17 & Spring Boot 3.2.0
* **Purpose**: Core application runtime and configuration framework.
* **Why Chosen**: Java 17 record structures, pattern matching, and Spring Boot's autoconfiguration simplify development.
* **Usage**: Core business services, scheduling tasks, and REST controllers.

### Spring Security & JWT
* **Purpose**: Stateless request authorization and authentication.
* **Why Chosen**: Allows stateless scaling without session replication.
* **Usage**: Extends security filters to intercept requests, extract bearer tokens, and validate signatures.

### Spring Data JPA & Hibernate
* **Purpose**: Object-relational database mapping and repository queries.
* **Why Chosen**: Simplifies data access and database operations.
* **Usage**: Maps database entities to Java models using lazy loading to optimize database access.

### Redis Cache
* **Purpose**: Key-value data cache and rate limiting database.
* **Why Chosen**: High-performance caching reduces database load.
* **Usage**: Caches product catalog pages and tracks IP rate limit counters.

### MySQL & H2
* **Purpose**: Persistent storage engines.
* **Why Chosen**: Relational database integrity for transactional data.
* **Usage**: MySQL serves as the production database, while H2 runs in-memory for testing.
