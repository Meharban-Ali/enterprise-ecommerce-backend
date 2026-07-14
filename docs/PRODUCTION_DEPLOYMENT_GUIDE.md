# Production Deployment Guide

This guide describes the step-by-step procedure for deploying version `v1.0.0` of the Spring Boot eCommerce Backend to production environments.

---

## 1. Prerequisites
* **Runtime Environment**: Docker (v20.10+), Docker Compose (v2.0+)
* **Database**: MySQL (v8.0)
* **Cache**: Redis (v7.0)
* **Mail Server**: SMTP mail server or external provider (e.g. SendGrid)
* **Resource Requirements**:
  * Minimum: 2 Core CPU, 4GB RAM
  * Recommended: 4 Core CPU, 8GB RAM

---

## 2. Production Secret & Environment Setup
All production secrets must be supplied via a secure `.env` file or direct environment variable bindings in the deployment context. **Never check this file into Git.**

Create a `.env` file at the root folder:
```bash
# Database Settings
MYSQL_ROOT_PASSWORD=your_secure_root_password
MYSQL_DATABASE=ecommerce
MYSQL_USER=ecommerce_user
MYSQL_PASSWORD=your_secure_db_password

# JWT Token Settings
JWT_SECRET=your_base64_encoded_secure_jwt_signing_key
JWT_EXPIRATION_MS=900000          # 15 minutes
JWT_REFRESH_EXPIRATION_MS=604800000 # 7 days

# Mail SMTP Settings
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=your_sendgrid_api_key

# CORS Security
CORS_ALLOWED_ORIGINS=https://ecommerce.com

# Actuator & Security Overrides
APP_SECURITY_RATE_LIMIT_ENABLED=true
MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true
```

---

## 3. Database Initial Migration
1. Ensure the MySQL database is running and accessible.
2. The application is configured with `spring.jpa.hibernate.ddl-auto=validate` and `spring.flyway.enabled=true`.
3. On application startup, Flyway will detect the empty schema and automatically execute [V1__Initial_Schema.sql](file:///D:/Meharban_Code/ecommerce/src/main/resources/db/migration/V1__Initial_Schema.sql) to set up all 34 tables, indexes, and constraints.
4. If schemas need pre-populating, execute out-of-band SQL seeders (never run `app.data-initializer.enabled=true` in production).

---

## 4. CI/CD Deployment Pipeline (GitHub Actions)
The recommended GitHub Actions workflow to build, package, and release the backend Docker image:

```yaml
name: Production Deployment CI/CD

on:
  push:
    tags:
      - 'v*'

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v3

      - name: Setup JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: 'maven'

      - name: Run Clean Verify Build
        run: ./mvnw clean verify

  docker-release:
    needs: build-and-test
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v3

      - name: Log in to Docker Registry
        uses: docker/login-action@v2
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and Push Docker Image
        uses: docker/build-push-action@v4
        with:
          context: .
          push: true
          tags: |
            ghcr.io/yourorg/ecommerce-backend:latest
            ghcr.io/yourorg/ecommerce-backend:${{ github.ref_name }}
```

---

## 5. Docker Compose Startup
Run the following command to deploy and start the multi-container production group in detached mode:
```bash
docker compose -f Docker-compose.yaml up -d
```

Verify service states and health status:
```bash
docker compose ps
```
The output must show `Up (healthy)` for the MySQL, Redis, and Spring Boot containers.
