# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.0] - 2026-07-14

### Added
* Secure one-time Super Admin bootstrap seeder logic resolving environment parameters (`SUPER_ADMIN_NAME`, `SUPER_ADMIN_EMAIL`, etc.).
* Dynamic bootstrap lock stored in `system_settings` table to prevent repeated initialization attempts.
* First-login password change required flow, blocking all non-auth REST requests with HTTP 403.
* Flyway schema database migration integration.
* Distributed scheduler locks via ShedLock.
* MDC JSON logging layout and sensitive log masker regex rules.
* Resilience4J circuit breakers, retries, and fallback methods.
* Complete validation test suite with 360 unit and integration tests.

### Changed
* Properties files standardized to exactly four files: `application.properties`, `application-dev.properties`, `application-prod.properties`, and `application-example.properties`.
* Actuator health probes and public rate limiting configured as environment variables.
* Tomcat file post size and Hikari connection limits tuned.
