# Railway Environment Guide

This guide documents all environment variables required to deploy and run the Spring Boot eCommerce Backend successfully in Railway.

---

## 1. Railway System Variables

| Variable Name | Description | Constraints / Default |
| :--- | :--- | :--- |
| `PORT` | Dyno port assigned by Railway. Spring Boot binds to this port dynamically. | Assigned automatically (default: `8090`). |
| `SPRING_PROFILES_ACTIVE` | The active configuration profile to load. | Must be set to `prod` in cloud environments. |

---

## 2. Infrastructure Mappings

| Variable Name | Category | Description | Required? |
| :--- | :---: | :--- | :---: |
| `DB_URL` | Database | JDBC connection string to Railway MySQL database. | **YES** |
| `DB_USERNAME` | Database | Database administrator username. | **YES** |
| `DB_PASSWORD` | Database | Database administrator password. | **YES** |
| `REDIS_HOST` | Cache | Host domain of Railway Redis cache instance. | **YES** |
| `REDIS_PORT` | Cache | Connection port of Railway Redis instance (usually `6379`).| **YES** |

---

## 3. Operations & Safety Variables

| Variable Name | Description | Default / Example | Required? |
| :--- | :--- | :--- | :---: |
| `JWT_SECRET` | Base64 encoded 256-bit token signing key. | `your_secret` | **YES** |
| `SUPER_ADMIN_PASSWORD` | One-time Super Admin bootstrap password. | `SecurePassword@123` | **YES** |
| `APP_SECURITY_RATE_LIMIT_ENABLED`| Set to `true` to enable rate limiter checks. | `true` | **YES** |
| `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED`| Exposes liveness/readiness check Actuator probes. | `true` | **YES** |
