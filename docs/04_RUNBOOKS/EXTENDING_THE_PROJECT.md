# PROJECT EXTENSION GUIDE

This document explains coding conventions and extension guidelines for developers adding new features.

## 1. Domain Component Mapping
To add a new business module (e.g., `Coupon`):
1. **Entities**: Define mapping classes under `com.redis.coupon.entity`. Use `@Version` for concurrency control on critical resources.
2. **DTOs**: Define request/response payloads under `com.redis.coupon.dto`. Annotate fields with `@Valid` constraints.
3. **Repository**: Implement repository interfaces extending `JpaRepository` under `com.redis.coupon.repository`.
4. **Service**: Define interfaces and implementations under `com.redis.coupon.service`. Group database writes within `@Transactional` blocks.
5. **Controller**: Implement controllers mapping HTTP endpoints under `com.redis.coupon.controller`. Secure methods using `@PreAuthorize`.

---

## 2. Global Exception Handling Integration
To add custom exceptions:
1. Define exception classes extending `RuntimeException` under `com.redis.common.exception`.
2. Map exception responses to custom envelopes using `@ExceptionHandler` in `GlobalExceptionHandler.java`.
