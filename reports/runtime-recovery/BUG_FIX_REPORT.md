# Bug Fix Report

This report catalogs all modifications applied to code and configurations during this sprint.

---

## 1. Catalog of Modified Files

### A. Java 21 Migration Changes

#### 1. [`pom.xml`](file:///D:/Meharban_code/ecommerce/pom.xml)
* **Changes**: Updated `<java.version>` property from `17` to `21`.
* **Rationale**: Reconfigures the Maven compiler release target to Java 21 LTS.

#### 2. [`Dockerfile`](file:///D:/Meharban_code/ecommerce/Dockerfile)
* **Changes**:
  * Phase 1 (Build): `maven:3.9-eclipse-temurin-21 AS build`
  * Phase 2 (Runtime): `eclipse-temurin:21-jre-alpine`
* **Rationale**: Upgrades compilation and execution runtime container JDK/JRE versions to Java 21.

#### 3. GitHub Actions Workflows
* **Target Files**:
  * [`.github/workflows/build.yml`](file:///D:/Meharban_code/ecommerce/.github/workflows/build.yml)
  * [`.github/workflows/test.yml`](file:///D:/Meharban_code/ecommerce/.github/workflows/test.yml)
  * [`.github/workflows/release.yml`](file:///D:/Meharban_code/ecommerce/.github/workflows/release.yml)
* **Changes**: Updated actions JDK setup block `java-version: '17'` to `java-version: '21'`.
* **Rationale**: Ensures CI pipelines execute verification checks using the Java 21 runtime.

---

### B. Security & System HARDENING Changes

#### 1. [`JwtProperties.java`](file:///D:/Meharban_code/ecommerce/src/main/java/com/redis/infrastructure/config/JwtProperties.java)
* **Changes**: Removed fallback string assignment from `private String secret`.
* **Rationale**: Removes exposed keys, forcing variable injection.

#### 2. [`application.properties`](file:///D:/Meharban_code/ecommerce/src/main/resources/application.properties)
* **Changes**: Removed duplicate H2 datasource connection configurations.
* **Rationale**: Decouples baseline profile database configurations, preventing production boot blocks.

#### 3. [`docker-compose.yml`](file:///D:/Meharban_code/ecommerce/docker-compose.yml)
* **Changes**: Replaced non-standard default variable format `${VAR:default}` with standard format `${VAR:-default}`.
* **Rationale**: Resolves invalid interpolation errors and enables clean parsing by Docker compose engines.
