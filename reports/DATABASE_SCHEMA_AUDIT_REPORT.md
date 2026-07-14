# Database Schema Audit Report

This report documents the root cause analysis, table audit, and synchronization resolution of the database schema validation failure (`Schema-validation: missing table [alert_rules]`) encountered during automated test runs on Linux-based CI environments.

---

## 1. Root Cause Analysis
* **Failing Symptom**: `Schema-validation: missing table [alert_rules]` during the test context loading phase.
* **Component Affected**: Hibernate `SessionFactory` initialization under the `test` active profile.
* **Why the Issue Occurred**:
  * The database migration file [V1__Initial_Schema.sql](file:///D:/Meharban_Code/ecommerce/src/main/resources/db/migration/V1__Initial_Schema.sql#L380) correctly contains the `alert_rules` table definition.
  * During local development on Windows, file systems and database emulations are case-insensitive by default.
  * In the Linux (Ubuntu) environment used by GitHub Actions, the database emulated by H2 under `MODE=MySQL` acts case-sensitively. 
  * By default, H2 converts identifiers to uppercase (`ALERT_RULES`). Since Hibernate validates lowercase names (`alert_rules`) on a case-sensitive file system, a mismatch occurs, causing the schema validator to report a missing table.

---

## 2. Tables Audited & Mapping Verification
We verified that the Hibernate entities map to the corresponding tables created by Flyway:

| Entity Name | Target Table | Created in V1? | Verification Status |
| :--- | :--- | :---: | :---: |
| `AlertRule` | `alert_rules` | **YES** (L380) | **Synced** |
| `Incident` | `incidents` | **YES** (L400) | **Synced** |
| `User` | `users` | **YES** (L34) | **Synced** |
| `Product` | `products` | **YES** (L18) | **Synced** |

---

## 3. Configuration Fix
We modified the H2 database URL in the test properties to force case preservation and identifier lowercase conversions on all platforms:

* **File Modified**: [application-test.properties](file:///D:/Meharban_Code/ecommerce/src/test/resources/application-test.properties)
* **Change**:
  ```properties
  spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDER=HIGH
  ```

---

## 4. Why the Fix is Correct
* `DATABASE_TO_LOWER=TRUE` instructs H2 to convert all table identifiers and column names to lowercase.
* This ensures metadata checks queried by Hibernate validation match exactly on Linux runners, preserving cross-platform consistency without compromising production database safety profiles (`ddl-auto=validate`).
