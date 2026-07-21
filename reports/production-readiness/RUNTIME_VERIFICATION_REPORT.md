# Runtime Verification Report

This report records the Spring Boot application bootstrap diagnostics and port initializations.

---

## 1. Application Startup Diagnostics

* **Startup Log**: Tomcat server started on port `8090` (http) in **24.777 seconds**.
* **Startup Status**: **`Started EcommerceApplication`** printed successfully, confirming zero context boot blockers.
* **Process Threading Mode**: running under **Java 21 Virtual Threads** (Tomcat request executors leverage native Loom virtual threads).

---

## 2. API Smoke Testing Verification

We verified the availability of core endpoints under active port bindings:
* **Registration** (`/api/auth/register`): **`PASS`** (201 Created).
* **Login** (`/api/auth/login`): **`PASS`** (200 OK — returns tokens).
* **Refresh Token** (`/api/auth/refresh`): **`PASS`** (200 OK — rotates accessToken).
* **Products Catalog** (`/api/products`): **`PASS`** (200 OK under Bearer authorization).
* **Logout** (`/api/auth/logout`): **`PASS`** (200 OK — blacklists token).
* **Health Actuator** (`/actuator/health`): **`PASS`** (returns full component status details).
