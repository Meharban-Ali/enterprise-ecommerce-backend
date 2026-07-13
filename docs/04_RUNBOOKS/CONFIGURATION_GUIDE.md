# PROPERTIES CONFIGURATION GUIDE

This document explains configuration loading priorities and environment profile management.

## 1. Property Resolution Precedence
The Spring Boot application resolves properties in the following order:

```
[1. Command line arguments] ──> [2. Environment Variables (.env)] ──> [3. application.properties]
```

## 2. Active Profiles
The active profile is defined by setting `spring.profiles.active` (e.g., `dev`, `prod`, `local-h2`).
* **dev profile**: Configures debug logging and enables SQL parameter logging.
* **prod profile**: Configures maximum Hikari connection pools and restricts Actuator endpoints.
* **local-h2 profile**: Configures H2 in-memory databases and simple in-memory caching.
