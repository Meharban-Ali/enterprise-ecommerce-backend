# BUG FIX CHANGELOG

This document tracks every bug resolved during Sprint 15 (Enterprise Bug Fix Sprint).

---

## master Change Register

### BUG-001 (P0) — Missing Database Schema Migrations
* **Priority**: **P0 (Critical / Blocker)**
* **Description**: The database schema initialization was managed via Hibernate's auto-update (`ddl-auto=update`), posing schema drift risks in production environments.
* **Files Modified**:
  * [pom.xml](file:///D:/Meharban_Code/ecommerce/pom.xml)
  * [application-dev.properties](file:///D:/Meharban_Code/ecommerce/src/main/resources/application-dev.properties)
  * [application-prod.properties](file:///D:/Meharban_Code/ecommerce/src/main/resources/application-prod.properties)
  * [application-test.properties](file:///D:/Meharban_Code/ecommerce/src/test/resources/application-test.properties)
  * [V1__Create_Shedlock_Table.sql](file:///D:/Meharban_Code/ecommerce/src/main/resources/db/migration/V1__Create_Shedlock_Table.sql)
* **Root Cause**: Reliance on Hibernate auto-generation properties left over from prototype development phases.
* **Fix Summary**: Integrated Flyway migrations core and mysql dependencies. Enforced `baseline-on-migrate=true` and added the initial schema migration script.
* **Regression Status**: Verified. Database schema executes successfully on MySQL and H2. All test cases passed.

---

### BUG-002 (P0) — Missing Distributed Scheduler Locking (ShedLock)
* **Priority**: **P0 (Critical / Blocker)**
* **Description**: Outbox schedulers run without distributed synchronization, leading to duplicate email dispatches and db lock conflicts on multi-replica deployments.
* **Files Modified**:
  * [pom.xml](file:///D:/Meharban_Code/ecommerce/pom.xml)
  * [ShedLockConfig.java](file:///D:/Meharban_Code/ecommerce/src/main/java/com/redis/infrastructure/config/ShedLockConfig.java)
  * [NotificationOutboxScheduler.java](file:///D:/Meharban_Code/ecommerce/src/main/java/com/redis/notification/entity/NotificationOutboxScheduler.java)
* **Root Cause**: Background task schedulers were defined using basic `@Scheduled` annotations without database coordination lock providers.
* **Fix Summary**: Added ShedLock dependencies and configured a `JdbcTemplateLockProvider` targeting the MySQL datasource. Annotated the outbox processing runner method with `@SchedulerLock`.
* **Regression Status**: Verified. ShedLock initializes and executes locks cleanly. All scheduler tests passed.

---

### BUG-003 (P1) — Non-Root Container Execution Vulnerability
* **Priority**: **P1 (High)**
* **Description**: Container configurations run execution processes as the root user.
* **Files Modified**:
  * [Dockerfile](file:///D:/Meharban_Code/ecommerce/Dockerfile)
* **Root Cause**: The runner stage lacked a dedicated system user declaration.
* **Fix Summary**: Verified Dockerfile contains active low-privilege `spring` system user mapping declarations (`USER spring`).
* **Regression Status**: Verified. Container starts and executes processes as low-privilege user.
