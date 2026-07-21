# E-Commerce Project Master Documentation

This document serves as the single source of truth for the project overview, architecture, security flows, configurations, local setup, and deployment processes.

---

## 1. Project Overview & Tech Stack
The E-Commerce Spring Boot Backend is a high-performance, secure REST API service designed using Spring Boot 3, Redis (for token blacklisting, caching, and rate limiting), and MySQL 8.0 (for persistent storage). It is fully containerized, integrates DB migrations via Flyway, and includes comprehensive automated testing and observability logs.

### Technical Stack
* **Language**: Java 17
* **Framework**: Spring Boot 3.2.0, Spring Security 6.2
* **ORM & JPA**: Hibernate 6.3.1.Final, Spring Data JPA
* **Cache & Memory Store**: Redis (via Spring Data Redis / Lettuce driver)
* **Persistent Database**: MySQL 8.0 (H2 used in MySQL compatibility mode for testing)
* **Migrations**: Flyway DB Migration 9.22
* **Build System**: Maven (via `./mvnw` wrapper)
* **Containerization**: Docker & Docker Compose
* **API Documentation**: Springdoc OpenAPI / Swagger UI

---

## 2. System Architecture

The application adopts a standard layered architecture with clean separation of concerns:

```text
+---------------------------------------------------------+
|                  REST Controllers Layer                 |
|             (Endpoint mappings, Request DTOs)           |
+---------------------------+-----------------------------+
                            |
+---------------------------v-----------------------------+
|                  Service & Logic Layer                  |
|             (Business rules, Transaction bounds)        |
+---------------------------+-----------------------------+
                            |
+---------------------------v-----------------------------+
|                  Repository & Data Access               |
|            (Spring Data JPA repositories, Caching)       |
+---------------------------+-------------+---------------+
                            |             |
+---------------------------v---+     +---v---------------+
|        Persistent DB (MySQL)  |     |   Cache (Redis)   |
+-------------------------------+     +-------------------+
```

---

## 3. Directory Structure

```text
ecommerce/
├── src/
│   ├── main/
│   │   ├── java/com/redis/              # Main Java source package
│   │   │   ├── analytics/               # Analytics & Metrics logic
│   │   │   ├── audit/                   # Audit logging & event listening
│   │   │   ├── auth/                    # Session authentication, OTP, JWTs
│   │   │   ├── cart/                    # Cart management
│   │   │   ├── common/                  # Global exceptions, filters, utilities
│   │   │   ├── incident/                # Alert monitoring & system incidents
│   │   │   ├── infrastructure/          # Security filters & configuration beans
│   │   │   ├── monitoring/              # Health indicators & schedulers
│   │   │   ├── notification/            # In-app, SMS, email & push notification engines
│   │   │   ├── order/                   # Order placement & fulfillment
│   │   │   ├── payment/                 # Stripe/Razorpay integrations
│   │   │   ├── product/                 # Catalog & Inventory management
│   │   │   ├── security/                # Idempotency, rate limiting, and API keys
│   │   │   ├── user/                    # User models, profiles & seeder
│   │   │   └── webhook/                 # Outbound webhook delivery system
│   │   └── resources/
│   │       ├── db/migration/            # Flyway SQL migration scripts
│   │       ├── templates/               # Thymeleaf mail templates
│   │       ├── application.properties   # Shared baseline configurations
│   │       ├── application-dev.properties # Development environment settings
│   │       └── application-prod.properties # Production environment settings
│   └── test/                            # Comprehensive unit & integration tests
├── docs/                                # Project documentation folder
│   ├── PROJECT_DOCUMENTATION.md         # Unified project architecture documentation
│   └── POSTMAN_API_GUIDE.md             # API endpoint collection reference
├── .github/workflows/                   # GitHub Actions validation pipelines
├── .gitignore                           # Excludes target/, .idea/, .env* files
├── .env.example                         # Environment configuration templates
├── Dockerfile                           # Multi-stage JRE Alpine container setup
├── docker-compose.yml                   # Multi-service stack composition
└── pom.xml                              # Maven project dependency configuration
```

---

## 4. Key Features & Implementations

### A. Authentication & Authorization Flow
* **JWT Flow**: Authentication is stateless. On successful login, the client receives an Access Token (15-minute TTL) and a Refresh Token. The Access Token is signed using HMAC-SHA512.
* **Refresh Token Flow**: Refresh tokens are stored in the database. When the Access Token expires, the client calls `/api/auth/refresh` with the Refresh Token to retrieve a new Access Token.
* **Logout Blacklisting**: When a user logs out, the access token is added to a Redis blacklist with a TTL equal to the token's remaining validity duration. The request filter checks Redis on every incoming request.
* **Password Hashing**: Done via `BCryptPasswordEncoder` (strength 12).
* **Super Admin Bootstrap Lock**: On initial boot, the seeder checks `system_settings` for `bootstrap.completed`. If absent, it bootstraps the Super Admin account and locks the flag to prevent reuse.

### B. Caching & Persistence
* **MySQL 8.0**: Persistent relational storage mapping JPA entities.
* **Flyway**: Handles versioned migrations (stored under `src/main/resources/db/migration/`). All tables are baselined and validated.
* **Redis**: Acts as the high-speed cache store for rate limits, API keys validation caching, and token revoking.

### C. Webhooks & Messaging
* **Outbound Webhooks**: Integrators register URL endpoints. The system queues event payloads and signs posts using HMAC-SHA256. Webhooks implement circuit breakers and retry limits.

---

## 5. Security Features

* **Rate Limiting**: Implements IP-based and user-based bucket rate limiters in Redis.
* **Idempotency Key validation**: Post requests accept `Idempotency-Key` headers. The request/response payloads are cached in Redis/Database to prevent double-processing.
* **Jakarta Bean Validation**: Strict input constraints (`@NotBlank`, `@Size`, `@Email`) applied to all incoming DTO requests.
* **Global Error Sanitization**: Global exception handlers capture all exceptions and mask structural stack traces, returning sanitized JSON payloads.
* **Logger Masking**: Core attributes (`password`, `secret`, `jwt`) are matched via regex patterns in `SensitiveDataMasker.java` and masked before writing to file systems.

---

## 6. Environment Configurations

Below are the variables required to run the application (refer to `.env.example` for details):

| Variable Name | Description | Default / Example |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring environment profile | `dev` / `prod` |
| `DB_URL` | Production database connection URL | `jdbc:mysql://host:3306/db` |
| `DB_USERNAME` | Production database user name | `admin` |
| `DB_PASSWORD` | Production database password | `ProdSecurePassword!` |
| `REDIS_HOST` | Redis host address | `localhost` / `redis` |
| `REDIS_PORT` | Redis port number | `6379` |
| `SMTP_USERNAME` | SMTP email dispatch username | `supportecommerces@gmail.com` |
| `SMTP_PASSWORD` | SMTP app token password | `your_smtp_app_password` |
| `JWT_SECRET` | 512-bit Base64 access token key | (Newly generated Base64 key) |

---

## 7. Setup & Run Runbook

### Local Workspace Setup
1. **Prerequisites**: Ensure Java 17 JDK, Maven 3+, MySQL 8.0, and Redis are installed.
2. **Database Setup**: Create a database named `redisdb` in MySQL.
3. **Environment**: Copy `.env.example` to `.env` and fill in local credentials.
4. **Boot App**:
   ```bash
   ./mvnw clean compile
   ./mvnw spring-boot:run
   ```

### Docker Setup
1. **Build Container**:
   ```bash
   docker compose build
   ```
2. **Launch Services**:
   ```bash
   docker compose up -d
   ```
   This initializes MySQL (on host port 3307), Redis (on port 6379), and the Spring Boot application (on port 8085).

---

## 8. Future Architecture Improvements
1. **Dynamic Webhook Signatures**: Introduce public/private key pairs for webhooks verification.
2. **Spring Actuator Hardening**: Configure Spring Security to restrict actuator monitoring endpoints to admin-only IPs.
3. **Short-Lived Refresh Tokens**: Implement refresh token rotation (RTR) to invalidate previous refresh tokens upon reuse.
