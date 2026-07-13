# SYSTEM OPERATIONS GUIDE

This guide details the application runtime operations, startup scripts, and graceful shutdown settings.

## 1. Application Lifecycle Controls
* **Graceful Shutdown**: The application is configured to wait 15 seconds for active transactions to complete before shutting down:
  ```properties
  server.shutdown=graceful
  spring.lifecycle.timeout-per-shutdown-phase=15s
  ```
* **Process Termination**: SRE teams should send `SIGTERM` signals to allow active requests to finish processing before termination:
  ```bash
  kill -15 <pid>
  ```

---

## 2. Health Check Verifications
Confirm that health actuator checks pass before routing traffic:
* **Liveness Probe**: `GET /actuator/health/liveness` -> Expect status code `200 OK` with payload `{"status":"UP"}`.
* **Readiness Probe**: `GET /actuator/health/readiness` -> Expect status code `200 OK` with payload `{"status":"UP"}`.
