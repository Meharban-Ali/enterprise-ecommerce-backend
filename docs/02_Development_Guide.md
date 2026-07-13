# 02. Development Guide

## 1. Onboarding & Workspace Setup
* **JDK Version**: Java 17.
* **Maven Version**: Apache Maven 3.8+.
* **Local In-Memory Profile**:
  ```bash
  mvn clean spring-boot:run -Dspring.profiles.active=local-h2
  ```
* **Dev Profile (Requires Docker Compose)**:
  ```bash
  docker-compose up -d
  mvn clean spring-boot:run -Dspring.profiles.active=dev
  ```

## 2. Coding Standards
* **Immutability**: Use Java Records for DTOs; inject dependencies via constructor injection (`@RequiredArgsConstructor`).
* **Null Handling**: Return `Optional<T>` instead of returning null for potentially missing variables.
