# Java 21 LTS Runtime Boot Verification Report

This report records the Spring Boot container startup parameters and integration connections under the Java 21 JVM.

---

## 1. Local Dev Runtime Metrics

* **Command**: `$env:SPRING_PROFILES_ACTIVE="dev"; .\mvnw.cmd spring-boot:run`
* **Java Version**: `21.0.10+8-LTS-217`
* **Execution Status**: **`RUNNING`** (Successful Tomcat initialization)
* **Start Latency**: **24.945 seconds** (Down from 47.866 seconds on Java 17)

---

## 2. Resource & Connection States

* **Tomcat Server**: Exposes port `8090` internally, serving the context root successfully.
* **Flyway Engine**: Successfully scans `db/migration/` files, resolving baseline settings.
* **MySQL Persistent DB**: HikariCP connects to `jdbc:mysql://localhost:3306/redisdb`, setting pool boundaries.
* **Redis Cache Connector**: Connection pool connects to WSL Redis on port `6379`.
* **Actuator Health Monitor**: Resolves endpoints beneath `/actuator`, reports system health state: `UP`.

---

## 3. Runtime Exceptions & Warnings

* **Stack Traces**: **Zero** startup stack traces or exceptions detected.
* **Circular Dependencies**: **Zero** bean circularity warnings.
* **Memory Constraints**: No JVM memory limit warnings or JNI library conflicts.
