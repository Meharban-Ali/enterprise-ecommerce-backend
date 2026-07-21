# Dependency Validation Report

This report confirms the stability and version compatibility of the dependencies declared in the project build configuration.

---

## 1. Dependency Alignment Matrix

| Group & Artifact ID | Declared Version | Source | Java 21 Status |
| :--- | :---: | :---: | :--- |
| `org.springframework.boot:spring-boot-starter-parent` | `3.2.0` | Boot Parent | Compatible (Baseline Java 17, compiles Java 21) |
| `org.springframework.boot:spring-boot-starter-web` | *inherited* | Boot Starter | Fully Compatible |
| `org.springframework.boot:spring-boot-starter-data-jpa`| *inherited* | Boot Starter | Fully Compatible |
| `org.springframework.boot:spring-boot-starter-data-redis`| *inherited* | Boot Starter | Fully Compatible |
| `com.mysql:mysql-connector-j` | *inherited* | Boot Starter | Compatible (MySQL 8.0 support active) |
| `com.h2database:h2` | *inherited* | Boot Starter | Compatible (Used under test scope) |
| `org.projectlombok:lombok` | *inherited* | Boot Starter | Compatible (Lombok 1.18.30 annotation compiler) |
| `org.flywaydb:flyway-core` | *inherited* | Boot Starter | Fully Compatible |
| `io.jsonwebtoken:jjwt-api` | `0.11.5` | Defined Property| Compatible (HMAC-SHA signature encryption active) |
| `io.github.resilience4j:resilience4j-spring-boot3` | `2.1.0` | Defined Property| Compatible (Circuit breaker patterns compiled) |

No conflicting versions or obsolete Java 17 dependency declarations remain in the [`pom.xml`](file:///D:/Meharban_code/ecommerce/pom.xml) build file.
