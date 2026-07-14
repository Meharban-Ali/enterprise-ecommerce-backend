# Release Checklist

This checklist documents the sequence of checks required before promoting code to production.

---

## 1. Phase 1: Pre-Release Validation
- [ ] **Dependency Scan**: Verify no vulnerable dependencies exist (`mvn dependency-check:check` or GitHub Dependabot warnings checked).
- [ ] **Secrets Scan**: Confirm no API keys or database credentials exist in the committed code.
- [ ] **Changelog Updated**: Check that [CHANGELOG.md](file:///D:/Meharban_Code/ecommerce/CHANGELOG.md) contains all changes for the release version.

---

## 2. Phase 2: Build & Test Verification
- [ ] **Clean Compilation**: Execute `./mvnw clean compile` to ensure no syntax errors exist.
- [ ] **Unit & Integration Tests**: Run `./mvnw test` and confirm 100% success rate across all 360 tests.
- [ ] **Package Creation**: Execute `./mvnw package -DskipTests=true` to build the final executable JAR file.

---

## 3. Phase 3: Configuration & Environment Verification
- [ ] **Properties Mode**: Verify `spring.jpa.hibernate.ddl-auto` is set to `validate` in `application-prod.properties`.
- [ ] **Probes Flag**: Ensure `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED` is defined in the container environment.
- [ ] **Rate Limiter**: Ensure `APP_SECURITY_RATE_LIMIT_ENABLED` is set to `true`.
- [ ] **Bootstrap Credentials**: Confirm `SUPER_ADMIN_NAME`, `SUPER_ADMIN_EMAIL`, `SUPER_ADMIN_PASSWORD`, and `SUPER_ADMIN_PHONE` environment variables are loaded in the deploy settings.

---

## 4. Phase 4: Backup & Deployment
- [ ] **Database snapshot**: Trigger a full manual snapshot of the production database before container update.
- [ ] **Docker Pull**: Pull the tagged image from the GitHub Container Registry (`ghcr.io`).
- [ ] **Compose Launch**: Deploy container:
  ```bash
  docker compose -f Docker-compose.yaml up -d
  ```

---

## 5. Phase 5: Post-Release Verification (Smoke Testing)
- [ ] **Liveness Probe**: Call `GET http://localhost:8085/actuator/health/liveness` and check status = `UP`.
- [ ] **Readiness Probe**: Call `GET http://localhost:8085/actuator/health/readiness` and check status = `UP`.
- [ ] **Bootstrap Seeder Logs**: Check logs to verify identity seeder completed successfully.
- [ ] **First Login check**: Log in with temporary Super Admin credentials and verify that the 403 password-change-required lockout is active.
- [ ] **Password change check**: Reset password and confirm regular operational endpoints are now accessible.
