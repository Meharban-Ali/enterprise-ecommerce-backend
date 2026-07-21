# Enterprise Spring Boot eCommerce Backend Platform

[![Build Status](https://img.shields.io/badge/Build-Success-brightgreen)](https://github.com/Meharban-Ali/eCommerce-Application)
[![Java Version](https://img.shields.io/badge/Java-17-blue)](https://openjdk.org/projects/jdk/21)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-red)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/MySQL-8.0-orange)](https://www.mysql.com/)
[![Caching](https://img.shields.io/badge/Redis-7.0-darkred)](https://redis.io/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A production-grade, highly secure, and optimized eCommerce REST API platform built with Spring Boot 3, Redis, and MySQL. The system is designed following clean architecture principles and domain-driven design boundary practices. It provides high-performance, stateless authentication, safe concurrent checkouts, outbox-pattern notification logs, rate limiting, and out-of-the-box observability features.

---

## 1. Architecture Overview

The system architecture utilizes a layered REST model with asynchronous event dispatches and caching boundaries:

```text
+-----------------------------------------------------------------------+
|                              CLIENT                                   |
+----------------------------------+------------------------------------+
                                   | HTTP Request
                                   v
+----------------------------------+------------------------------------+
|                      SPRING SECURITY FILTER CHAIN                     |
|  - RateLimitingFilter (Redis Buckets)                                 |
|  - JwtAuthenticationFilter (Token Signature Check)                    |
|  - ApiKeyAuthenticationFilter (Admin X-API-Key SHA-256 Hash)          |
+----------------------------------+------------------------------------+
                                   | Authenticated Context
                                   v
+----------------------------------+------------------------------------+
|                       CONTROLLERS LAYER                               |
|  - DTO Jakarta Validations  - Global Exceptions Sanitizer             |
+----------------------------------+------------------------------------+
                                   | Service Method Call
                                   v
+----------------------------------+------------------------------------+
|                         SERVICES LAYER                                |
|  - Business Rules  - Transaction Boundaries  - Idempotency Locks     |
+----------------------------------+------------------------------------+
                                   | Data Operations
                                   v
+----------------------------------+------------------------------------+
|                      REPOSITORIES LAYER                               |
|  - JPA/Hibernate 6 ORM  - Version-based Optimistic Locking           |
+---------------------+------------+------------+-----------------------+
                      |                         |
                      v SQL                     v Redis command
+---------------------+------------+  +---------+-----------------------+
|  PERSISTENCE STORAGE (MySQL 8)   |  | HIGH-SPEED CACHE (Redis)      |
|  - Users, Orders, Webhooks tables|  | - Token Blacklist             |
|  - Outbox Notification Logs      |  | - Products & API Keys Cache   |
+----------------------------------+  +-------------------------------+
```

For comprehensive architectural specifications and module layouts, refer to the [PROJECT_DOCUMENTATION.md](docs/PROJECT_DOCUMENTATION.md).

---

## 2. Key Features

* **Secure Authentication**: Stateless session authentication utilizing HMAC-SHA512 JWTs, database-stored Refresh Tokens, and high-speed Redis-based logout blacklists.
* **Optimistic Locking**: Hibernate version-based concurrency checks on the `Product` entity (`@Version` column) to guarantee zero stock oversells during traffic spikes.
* **Transactional Outbox Pattern**: Order placements and email/SMS notification dispatches are recorded in a single transaction block. Schedulers process and dispatch outbox notifications asynchronously.
* **API Hardening**: Custom IP/User rate limiters, `@Lob` payload mapping removals, idempotency status check filters, and SHA-256 hashed API keys for admin integrations.
* **Flyway Migrations**: Production-ready schema versions fully integrated and tracked.
* **Dockerized Composition**: Private network bridge configurations separating database instances, cache stores, and container JRE alpine environments.

---

## 3. Tech Stack

* **Core Framework**: Spring Boot 3.2.0, Spring Security 6.2, Spring Data JPA
* **Persistent DB**: MySQL 8.0 (H2 in-memory compatibility mode used in testing)
* **High-Speed Cache**: Redis 7.x
* **Database Migrations**: Flyway DB Migration 9.22
* **Build Wrapper**: Maven 3.8+ (mvnw wrapper included)
* **Testing Libraries**: JUnit 5, Mockito, AssertJ
* **Documentation**: OpenAPI 3.0 / Swagger UI

---

## 4. Environment Variables Required

Create a `.env` file in the root directory (based on [`.env.example`](.env.example)):

```properties
SPRING_PROFILES_ACTIVE=dev

# MySQL Database Parameters
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/redisdb
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_mysql_password

# Redis Parameters
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# JavaMailSender SMTP Parameters
SMTP_HOST=smtp.yourprovider.com
SMTP_PORT=587
SMTP_USERNAME=supportecommerces@gmail.com
SMTP_PASSWORD=your_smtp_app_password

# Cryptographic Keys
JWT_SECRET=your_base64_encoded_512_bit_jwt_signing_key
SUPER_ADMIN_PASSWORD=your_complex_bootstrap_password
```

---

## 5. Local Setup & Execution

### Prerequisites
* Java 17 JDK installed
* MySQL 8.0 running (with database `redisdb` created)
* Redis server running

### Steps
1. Clone the repository.
2. Configure your local settings inside `.env`.
3. Compile and verify the project (executes all 360 tests):
   ```bash
   ./mvnw clean verify
   ```
4. Boot the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   The backend will be available at `http://localhost:8090`.

---

## 6. Running with Docker Compose

To boot the entire multi-service stack (Spring Boot app + MySQL + Redis) instantly:

1. Build the multi-stage Docker image:
   ```bash
   docker compose build
   ```
2. Start the services:
   ```bash
   docker compose up -d
   ```
   * MySQL exposes port `3307` externally to prevent local port conflicts.
   * Redis runs inside the container network on port `6379`.
   * The Spring Boot app exposes Tomcat externally on port `8085`.

---

## 7. API Reference Documentation

Complete endpoint documentation (request payloads, headers, auth status, status codes, and error formats) is cataloged inside the [POSTMAN_API_GUIDE.md](docs/POSTMAN_API_GUIDE.md).

---

## 8. Screenshots Section (Placeholder)

> [!NOTE]
> System UI mockups, database schemas, and metrics dashboards will be uploaded here.

---

## 9. Future Roadmap

1. **Distributed Schedulers (ShedLock)**: Implement cluster-level locks for task schedulers to support safe multi-node replica scaling.
2. **SSO Keycloak Integration**: Replace standard JWT generation with Keycloak OAuth2 servers.
3. **Apache Kafka Decoupling**: Transition outbox event publishing to an external Apache Kafka messaging topic.

---

## 10. Contributing & License

* **Contributing**: Contributions are welcome. Please refer to [CONTRIBUTING.md](CONTRIBUTING.md) for code submission formats.
* **License**: This project is licensed under the Apache License 2.0. See [LICENSE](LICENSE) for details.
