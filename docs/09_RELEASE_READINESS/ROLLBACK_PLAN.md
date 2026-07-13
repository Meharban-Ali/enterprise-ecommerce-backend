# ROLLBACK PLAN

This guide details rollback triggers, database considerations, and cache clearing steps.

## 1. Rollback Triggers
Initiate the rollback plan if the following conditions occur after deployment:
* **Boot Failures**: Application fails to start or throws `BeanCreationException` loops.
* **Database Errors**: Connections fail or query response latency exceeds 5000ms.
* **Auth Outages**: JWT validation failures prevent all user logins.
* **HTTP Outages**: Rate limiting filters block valid customer traffic (HTTP 429).

---

## 2. Rollback Execution Steps
1. **Revert Version**: Redeploy the previous stable build image (e.g., `1.0.0-RC0`).
2. **Clear Cache**: Flush Redis keys to clear cached catalog items and reset rate limit counters:
   ```bash
   redis-cli FLUSHALL
   ```
3. **Verify Database**: Check database connections and verify tables consistency.
4. **Post-Rollback Verify**: Query the health check endpoint: `GET /actuator/health`. Ensure response is `{"status":"UP"}`.
