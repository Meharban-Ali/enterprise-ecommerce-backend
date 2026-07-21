# Startup Timeline

This document catalogs the execution timeline and duration of key Spring Boot bootstrap milestones recorded during the fresh runtime verification.

---

## 1. Milestone Timeline (dev profile)

| Relative Time | Timestamp | Lifecycle Event | Log Source / Details |
| :--- | :--- | :--- | :--- |
| **0.000 s** | `22:42:59.086` | **Bootstrap Start** | `Starting EcommerceApplication using Java 21.0.10 with PID 18440` |
| **1.215 s** | `22:43:00.301` | **Repository Scan** | `Bootstrapping Spring Data JPA repositories in DEFAULT mode.` |
| **5.272 s** | `22:43:04.337` | **Web Server Init** | `Tomcat initialized with port 8090 (http)` |
| **11.272 s** | `22:43:10.365` | **Datasource Connect**| `HikariPool-1 - Starting...` |
| **12.420 s** | `22:43:11.514` | **Flyway Migration**  | `Schema redisdb is up to date. No migration necessary.` |
| **18.434 s** | `22:43:17.521` | **JPA Context Loaded**| `Initialized JPA EntityManagerFactory for persistence unit default` |
| **40.434 s** | `22:43:39.512` | **Filter Mappings**   | `Will secure any request with [DisableEncodeUrlFilter, rateLimit, Jwt...]` |
| **45.285 s** | `22:44:44.371` | **Tomcat Binds Port** | `Tomcat started on port 8090 (http) with context path ''` |
| **45.339 s** | `22:44:44.425` | **Bootstrap Complete**| `Started EcommerceApplication in 42.614 seconds` |
| **45.660 s** | `22:44:44.725` | **Seeders Trigger**   | `DataInitializer running: Checking system initial requirements...` |
| **45.920 s** | `22:44:44.973` | **Readiness Checks**  | `All Platform Readiness Checks passed successfully.` |
| **45.922 s** | `22:44:44.978` | **Application Ready** | `Ecommerce redis application started..` |
