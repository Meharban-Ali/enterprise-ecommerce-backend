# Go-Live Checklist

This checklist defines the operational verification items required immediately before and after deployment of version `v1.0.0` to production.

---

## 1. Pre-Deployment Configuration Verification

| Item Description | Verification Action | Status | Owner |
| :--- | :--- | :---: | :--- |
| **Secrets Protection** | Confirm no secrets exist in Git repository history or properties. | **PASS** | DevSecOps |
| **JWT Secrets** | Ensure JWT secret is injected via environment variable (not defaults). | **PASS** | SRE |
| **Active Profile** | Verify that `spring.profiles.active` is set to `prod` in Docker Compose environment. | **PASS** | Release Mgr |
| **Hibernate Validation** | Verify `spring.jpa.hibernate.ddl-auto` is set to `validate` in [application-prod.properties](file:///D:/Meharban_Code/ecommerce/src/main/resources/application-prod.properties). | **PASS** | Architect |
| **ShedLock Database** | Verify `shedlock` table definition exists in [V1__Initial_Schema.sql](file:///D:/Meharban_Code/ecommerce/src/main/resources/db/migration/V1__Initial_Schema.sql). | **PASS** | Database Arch |

---

## 2. Infrastructure & Container Validation

| Item Description | Verification Action | Status | Owner |
| :--- | :--- | :---: | :--- |
| **Non-root Execution** | Confirm container runs under the non-root `spring` user. | **PASS** | DevSecOps |
| **Healthcheck script** | Ensure curl is installed in Alpine JRE layer and healthcheck executes. | **PASS** | SRE |
| **Volumes persistence** | Verify host directory mounting mapping persistent database storage. | **PASS** | SRE |
| **Network isolation** | Verify MySQL and Redis are only accessible internally via private bridge network. | **PASS** | SRE |
| **Memory limits** | Check that container resource limits (CPU/Memory) are applied in compose. | **PASS** | Performance Eng |

---

## 3. Operational, Monitoring, and Safety Checks

| Item Description | Verification Action | Status | Owner |
| :--- | :--- | :---: | :--- |
| **Rate Limit Enabled** | Verify `APP_SECURITY_RATE_LIMIT_ENABLED` is set to `true` in environment. | **PASS** | DevSecOps |
| **Actuator Probes** | Verify `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED` is set to `true`. | **PASS** | SRE |
| **Reliability Schedulers** | Ensure no horizontal duplicate backups/purges occur on same target disk. | **WARNING** | SRE |
| **Structured Logs** | Verify logs print in JSON and sensitive data regex mask rules are active. | **PASS** | DevSecOps |

---

## 4. Post-Deployment Verification Steps (Smoke Tests)
Immediately after deployment, perform the following validation commands to ensure operational sanity:

1. **Service Startup Check**:
   ```bash
   docker logs springboot_app
   ```
   *Expected Outcome*: Log output contains `Started EcommerceApplication in X seconds` and zero stack traces or BeanInitializationExceptions.

2. **Liveness & Readiness Probes**:
   ```bash
   curl -f http://localhost:8085/actuator/health/liveness
   curl -f http://localhost:8085/actuator/health/readiness
   ```
   *Expected Outcome*: Both endpoints return HTTP 200 with `{"status":"UP"}`.

3. **Verify DB Migration**:
   Connect to the database and query:
   ```sql
   SELECT * FROM flyway_schema_history;
   ```
   *Expected Outcome*: Contains version `1` (Initial Schema) marked as `SUCCESS`.

4. **Verify Outbox Scheduler**:
   Confirm that the ShedLock coordination table contains lock logs:
   ```sql
   SELECT * FROM shedlock;
   ```
   *Expected Outcome*: Contains a row named `NotificationOutboxScheduler_processOutbox` showing active locking records.
