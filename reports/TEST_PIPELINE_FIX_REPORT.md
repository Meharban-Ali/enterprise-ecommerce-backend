# Test Pipeline Fix Report

This report documents the root cause analysis, modifications made, and resolution validation of the Java Test Execution workflow failures in the GitHub Actions automation suite.

---

## 1. Root Cause Analysis
* **Failing Component**: `test` job inside `.github/workflows/test.yml` and `release` job inside `.github/workflows/release.yml`.
* **Technical Reason**:
  * The test execution profiles (`@ActiveProfiles("test")`) are configured in [application-test.properties](file:///D:/Meharban_Code/ecommerce/src/test/resources/application-test.properties) to run against an isolated **H2 in-memory database** (`jdbc:h2:mem:testdb;MODE=MySQL`) using the H2 driver (`org.h2.Driver`).
  * In the original GitHub Actions `test.yml` configuration, environment variables overriding target connections (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`) were passed to the shell.
  * In Spring Boot, host environment variables override property file values, forcing tests to load a MySQL connection schema (`jdbc:mysql://localhost:3306/redisdb`) while maintaining H2 drivers and settings, leading to driver resolution errors and connection timeouts.

---

## 2. Files Modified & Changes Made
We removed the conflicting database configurations to preserve the isolation of the active `test` profile:

* **[.github/workflows/test.yml](file:///D:/Meharban_Code/ecommerce/.github/workflows/test.yml)**:
  * Removed the `services` block (MySQL and Redis sidecars are not required since the test suite relies solely on in-memory H2 databases and mocks).
  * Removed `env` connection overrides from the `Run Test Suite` step.
* **[.github/workflows/release.yml](file:///D:/Meharban_Code/ecommerce/.github/workflows/release.yml)**:
  * Removed the `services` block and environment overrides from the test-running step.

---

## 3. Why the Fix is Correct
* Spring Boot integration tests are designed to execute in isolation using an in-memory MySQL-compatible H2 database with all external connection factories (like Redis connections and mail senders) stubbed out or mocked.
* Eliminating connection overrides allows the application to cleanly fallback to the test profile configurations defined in [application-test.properties](file:///D:/Meharban_Code/ecommerce/src/test/resources/application-test.properties).
* Deleting redundant MySQL and Redis containers inside the GitHub runner environment saves runner CPU resources, preventing timeouts and flaky pipeline execution.
