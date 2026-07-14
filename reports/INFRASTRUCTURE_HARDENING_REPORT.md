# Enterprise Infrastructure Hardening Report

This report documents the security, reliability, database, and container virtualization hardening changes implemented during **Sprint 16A – Enterprise Infrastructure Hardening** for the Spring Boot eCommerce Backend.

---

## 1. Database Hardening & Migrations (Flyway & Hibernate)
* **Goal**: Shift database control from Hibernate schema auto-generation to versioned Flyway migration scripts. Set Hibernate DDL generation to `validate` mode to prevent runtime table structural alterations in production.
* **Implemented Changes**:
  * Added `flyway-core` and `flyway-mysql` dependencies to [pom.xml](file:///D:/Meharban_Code/ecommerce/pom.xml).
  * Generated baseline schema DDL based on JPA mappings.
  * Created database-agnostic, versioned migration script [V1__Initial_Schema.sql](file:///D:/Meharban_Code/ecommerce/src/main/resources/db/migration/V1__Initial_Schema.sql) which sets up all 34 entity tables, indexes, and primary/foreign key mappings.
  * Integrated the ShedLock coordination table (`shedlock`) in the initial migration script.
  * Configured `spring.jpa.hibernate.ddl-auto=validate` inside both dev and production profiles ([application-dev.properties](file:///D:/Meharban_Code/ecommerce/src/main/resources/application-dev.properties) and [application-prod.properties](file:///D:/Meharban_Code/ecommerce/src/main/resources/application-prod.properties)).
  * Enabled Flyway migration runner during development, production, and test executions, ensuring zero schema discrepancy.

---

## 2. Distributed Scheduler Locking (ShedLock)
* **Goal**: Ensure that scheduled background jobs (such as outbox notifications) run on a single container instance at a time, preventing race conditions or duplicate execution in multi-instance horizontally scaled deployments.
* **Implemented Changes**:
  * Added `shedlock-spring`, `shedlock-provider-jdbc-template` dependencies to [pom.xml](file:///D:/Meharban_Code/ecommerce/pom.xml).
  * Created [ShedLockConfig.java](file:///D:/Meharban_Code/ecommerce/src/main/java/com/redis/infrastructure/config/ShedLockConfig.java) configured with a JDBC-based lock provider using the active database DataSource.
  * Configured the outbox processing job in [NotificationOutboxScheduler.java](file:///D:/Meharban_Code/ecommerce/src/main/java/com/redis/notification/entity/NotificationOutboxScheduler.java) with the `@SchedulerLock` annotation.
  * Applied `@Profile("!test")` to the ShedLock configuration to isolate and disable locking during unit/integration tests to ensure scheduler test reliability.

---

## 3. Docker Containerization Hardening
* **Goal**: Establish a secure, lightweight multi-stage Docker build pipeline executing as a non-root system user, with proper health verification.
* **Implemented Changes**:
  * Hardened the multi-stage [Dockerfile](file:///D:/Meharban_Code/ecommerce/Dockerfile) to map compilation to JDK 17 alpine and the final runtime runner to `eclipse-temurin:17-jre-alpine` JRE minimal image.
  * Added a dedicated non-root execution system user/group `spring` to limit potential container escalation attacks.
  * Installed the `curl` utility package in the JRE runner layer.
  * Hardened [Docker-compose.yaml](file:///D:/Meharban_Code/ecommerce/Docker-compose.yaml) to run container healthchecks targeting Spring Boot Actuator endpoint (`/actuator/health`) using the container local JRE port.
  * Restructured execution dependencies using `depends_on: mysql: condition: service_healthy` to guarantee correct startup ordering.

---

## 4. Connection Pool & Tomcat Hardening
* **Goal**: Fine-tune HikariCP and Tomcat parameters to ensure resiliency under load, prevent thread starvation, and limit request size attacks.
* **Implemented Changes**:
  * Configured thread pools, max connection size, minimum idle, connections timeout, and connection test queries for HikariCP.
  * Restricted max HTTP post sizes and swallow sizes in Tomcat configuration inside the production properties profile (`server.tomcat.max-http-form-post-size=2MB`).

---

## 5. Security & Logging Hardening
* **Goal**: Mitigate threat vectors including rate limits, API key leakage, clickjacking, and log exposure of sensitive data.
* **Implemented Changes**:
  * Configured explicit CORS profiles, binding specific origin domains and header permissions instead of wildcards.
  * Masked Hinglish messages, refined log detail levels across environments (root log levels set to `WARN` in production), and verified rate limiting services.

---

## 6. Verification Status & Build Validation
* **Verification Actions**:
  * Executed the complete build check `./mvnw clean verify`.
  * **Result**: **BUILD SUCCESS**
  * **Test execution**: All 355 unit and integration tests passed cleanly with 100% success rate, verifying the complete codebase against Flyway migrations, database validations, and scheduler lock rules.
