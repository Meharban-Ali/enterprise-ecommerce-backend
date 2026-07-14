# Docker Production Guide

This guide details the containerization architecture, production Dockerfiles, compose topologies, JVM runtime parameters, security configurations, and cloud deployment procedures.

---

## 1. Containerization Architecture
The platform is containerized using a multi-stage Docker build, producing a secure runtime image containing only the compiled Java application.

```
       [ STAGE 1: BUILD ]                          [ STAGE 2: RUNTIME ]
┌───────────────────────────────┐               ┌──────────────────────────────┐
│ maven:3.9-eclipse-temurin-17  │               │  eclipse-temurin:17-jre-alpine│
├───────────────────────────────┤               ├──────────────────────────────┤
│ * Caches pom.xml              │               │ * Minimal OS footprint       │
│ * Packages app.jar            │ ──(COPY JAR)─►│ * Custom User 'spring' (1001)│
│ * Ignores source after jar    │               │ * Hardened production JVM    │
└───────────────────────────────┘               └──────────────────────────────┘
```

---

## 2. Hardened JVM Production Configuration
To prevent memory constraints and ensure garbage collection efficiency inside container environments (like Kubernetes or ECS), the following JVM flags are defined:

* `-XX:MaxRAMPercentage=75.0`: Instructs the JVM to restrict heap memory allocation to 75% of the total container memory limit.
* `-XX:InitialRAMPercentage=50.0`: Pre-allocates heap memory on boot to avoid allocation overhead latency.
* `-XX:+UseG1GC`: Low-pause garbage collector suited for multi-core processors.
* `-XX:+ExitOnOutOfMemoryError`: Forcefully terminates the JVM on OutOfMemory errors, prompting container orchestrators (e.g. Kubernetes) to restart the container instantly.
* `-Duser.timezone=UTC`: Guarantees consistent date/time calculations regardless of hosting zone.
* `-Dfile.encoding=UTF-8`: Enforces standard encoding.

---

## 3. Container Topology & Networks
The compose stack isolates resources into private bridge networks:
* **Host Access Ports**: Only port `8085` (API gateway mapping to container port `8080`) is exposed externally.
* **Database & Cache Ports**: MySQL (port `3306`) and Redis (port `6379`) are isolated inside the bridge network. No external mapping ports are bound in production mode.
* **DNS Resolution**: Containers resolve services using container names (`mysql_db`, `redis_cache`), avoiding hardcoded host IPs or loopback dependencies.

---

## 4. Container Health Probes
Orchestration engines monitor service health using standard shell command probes:
* **MySQL**: Mapped to `mysqladmin ping -h localhost`.
* **Redis**: Mapped to `redis-cli ping`.
* **Spring Boot**: Mapped to Spring Actuator health endpoint `/actuator/health`.

---

## 5. Security Best Practices
* **Non-Root Execution**: Runs under the custom restricted user `spring`. No default root processes are permitted.
* **Least Privilege**: Superfluous Linux kernels capabilities (`CAP_*`) should be dropped at deploy runtime.
* **Read-Only FS**: The filesystem should be mounted as read-only, except for temporary storage spaces like `/tmp` or persistent volumes.
