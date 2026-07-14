# Rollback Strategy

This document details the emergency rollback procedures to be executed in the event of critical issues post-deployment of version `v1.0.0`.

---

## 1. Rollback Scenarios & Triggers
A rollback must be initiated immediately if any of the following conditions occur post-release:
1. **Startup Failure**: The Spring Boot container crashes repeatedly (CrashLoopBackOff) due to hidden classpath or context configuration errors.
2. **Database Migration Deadlocks**: Flyway migration fails mid-flight or locks core tables, causing database timeouts.
3. **Severe Regression**: High HTTP 5xx error rates (> 1%) on core endpoints (Login, Register, Order Creation).
4. **Data Corruption**: Severe bugs corrupt order data or database integrity.

---

## 2. Step 1: Application Containers Rollback
To revert the Spring Boot application container back to the previous stable release version (e.g. `v0.9.0`):

1. Edit `.env` or `Docker-compose.yaml` to change the image tag back to the previous stable version:
   ```yaml
   image: ghcr.io/yourorg/ecommerce-backend:v0.9.0
   ```
2. Restart the container:
   ```bash
   docker compose up -d app
   ```
3. Verify that the previous container version starts cleanly and passes basic healthchecks:
   ```bash
   curl -f http://localhost:8085/actuator/health
   ```

---

## 3. Step 2: Database Schema Reversion (Rollback Flyway)
Because the production database is set to `ddl-auto=validate`, any schema reversion must be managed carefully.

### Scenario A: Backward-Compatible Schema Changes
If the schema changes introduced in `v1.0.0` are backward-compatible (e.g. new nullable columns, new tables), **do not roll back the database schema**. Reverting the application container to `v0.9.0` will work fine as it will simply ignore the new fields.

### Scenario B: Breaking Schema Changes (Requires Restore)
If the schema changes are breaking (e.g. column type changes or table drops) and cause the previous application container (`v0.9.0`) to fail validation on startup:
1. Put the application into maintenance mode to block user writes:
   ```bash
   # Bind maintenance properties or block public ports on the reverse proxy
   ```
2. Restore the database to the pre-deployment snapshot (see [Backup and Recovery Guide](file:///D:/Meharban_Code/ecommerce/docs/BACKUP_AND_RECOVERY_GUIDE.md)).
3. Re-deploy the previous version `v0.9.0` application containers.
4. Verify validation success and remove maintenance mode.

---

## 4. Step 3: Traffic Routing Rollback
If deploying behind a reverse proxy (e.g. Nginx, AWS ALBs, or Cloudflare):
1. Immediately switch the upstream pointer of the reverse proxy configuration back to the blue environment (running `v0.9.0`).
2. Reload Nginx without downtime:
   ```bash
   nginx -s reload
   ```
3. Check reverse proxy log files to verify all further requests are routed to the old stable container group.
