# Configuration Guide

This guide details the Spring Boot eCommerce Backend configuration architecture, property file roles, profile activation, and environment variables mappings.

---

## 1. Configuration Architecture
The platform isolates environmental differences by separating configurations into property files and resolving secrets at runtime using OS environment variables.

```
                  ┌────────────────────────┐
                  │ application.properties │  (Encoding, JSON, Names)
                  └───────────┬────────────┘
                              │
             ┌────────────────┴────────────────┐
             ▼                                 ▼
┌──────────────────────────┐     ┌───────────────────────────┐
│  application-dev.properties │     │ application-prod.properties │
└────────────┬─────────────┘     └─────────────┬─────────────┘
             ▼                                 ▼
       Local Testing                     Production Deploy
    (SQL logs, Swagger)              (Validate, Restricted Actuator)
```

---

## 2. Property File Responsibilities

### A. [application.properties](file:///D:/Meharban_Code/ecommerce/src/main/resources/application.properties)
Holds common settings shared across development, staging, and production:
* Application name (`ecommerce`)
* Jackson date formatting serialization
* Tomcat servlet multipart file size thresholds (`2MB` limits)
* Swagger base mapping paths

### B. [application-dev.properties](file:///D:/Meharban_Code/ecommerce/src/main/resources/application-dev.properties)
Dedicated to developer sandbox usage:
* Hibernate schema set to `validate` mode.
* SQL logging enabled (`spring.jpa.show-sql=true`).
* Local mock SMTP bindings (host `localhost`, port `25`).
* Swagger UI and Swagger API docs enabled.

### C. [application-prod.properties](file:///D:/Meharban_Code/ecommerce/src/main/resources/application-prod.properties)
Locked down configuration for live staging and production:
* Database connection parameters mapped to environment variables.
* Connection pool sizing configured via HikariCP settings.
* Swagger disabled (`springdoc.swagger-ui.enabled=false`).
* Strict health probe triggers and rate limiting options enabled.

### D. [application-example.properties](file:///D:/Meharban_Code/ecommerce/src/main/resources/application-example.properties)
A reference template for new developers documenting variable parameters. Contains no passwords.

---

## 3. Environment Variable References

The following parameters must be injected during container startup:

| Variable Name | Property Mapping | Environment | Usage |
| :--- | :--- | :--- | :--- |
| `DB_URL` | `spring.datasource.url` | Production | Database connection path. |
| `DB_USERNAME` | `spring.datasource.username` | Production | Database user login. |
| `DB_PASSWORD` | `spring.datasource.password` | Production | Database password. |
| `REDIS_HOST` | `spring.data.redis.host` | Production | Redis server domain. |
| `SMTP_HOST` | `spring.mail.host` | Dev/Prod | SMTP server host domain. |
| `JWT_SECRET` | `app.jwt.secret` | Dev/Prod | Base64 token signing key. |
| `SUPER_ADMIN_PASSWORD` | - | Boot/Recovery | One-time Super Admin bootstrap password. |
| `APP_SECURITY_RATE_LIMIT_ENABLED` | `app.security.rate-limit-enabled` | Prod | Activates rate limit checks. |

---

## 4. Profile Activation
The active profile is controlled by Spring Boot property overrides:
* **Development**:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```
* **Production**:
  ```bash
  java -jar target/ecommerce-redis-1.0.0.jar --spring.profiles.active=prod
  ```
