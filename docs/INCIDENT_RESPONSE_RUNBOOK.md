# Incident Response Runbook

This document defines response plays and procedures for critical platform incidents.

---

## 1. Incident Management Overview
The backend includes a dedicated incident response helper [PlatformIncidentHelper.java](file:///D:/Meharban_Code/ecommerce/src/main/java/com/redis/incident/entity/PlatformIncidentHelper.java).
* **Incident Lifecycle**:
  ```mermaid
  graph TD
      A[Anomaly/Error] --> B[Alert Triggered]
      B --> C[Incident Auto-Created OPEN]
      C --> D[Operator Ack ACKNOWLEDGED]
      D --> E[Root Cause Analysis]
      E --> F[Resolution RESOLVED]
      F --> G[Close Incident CLOSED]
  ```
* **SLAs by Severity**:
  * **CRITICAL**: 15 minutes to Acknowledge, 2 hours to Resolve.
  * **HIGH / MEDIUM**: 30 minutes to Acknowledge, 4 hours to Resolve.

---

## 2. Play 1: Configuration Corruption (`CONFIG_CORRUPT`)
* **Trigger**: Scheduled check detects checksum mismatch on `application.properties` (logged as `CONFIGURATION_CHANGED | Corruption detected!`).
* **Symptom**: Auto-created incident with code `CONFIG_CORRUPT` in `incidents` table.
* **Response Steps**:
  1. Inspect the running application container files to check if files were modified:
     ```bash
     docker exec -it springboot_app sha256sum /app/resources/application.properties
     ```
  2. If unauthorized changes were made, immediately isolate the container (possible intrusion).
  3. Rebuild the container group from the clean registry image to restore the original configuration state:
     ```bash
     docker compose up -d --force-recreate app
     ```
  4. Verify checksum matching:
     ```bash
     curl -f http://localhost:8085/api/admin/reliability/dashboard
     ```
  5. Acknowledge and resolve the incident.

---

## 3. Play 2: Rate Limit Flood (`RATE_LIMIT_EXCEEDED`)
* **Trigger**: Severe burst of requests causes `RateLimitingFilter` to reject queries with HTTP 429.
* **Symptom**: High volume of log messages containing `Rate limit exceeded` and `X-API-Key` values.
* **Response Steps**:
  1. Identify the source IP or API Key causing the flood from the structured logs:
     ```bash
     docker logs springboot_app | grep "Rate limit exceeded"
     ```
  2. If the request volume is a Distributed Denial of Service (DDoS) attack targeting anonymous paths:
     * Enable Cloudflare/reverse proxy Under Attack mode.
     * Configure firewall rules to block the offending IPs.
  3. If it is an active API Key:
     * Revoke the key temporarily to stop processing overhead:
       * Call API Key revocation endpoint or set `revoked=true` in `api_keys` table.
     * Coordinate with the consumer to throttle request dispatches.

---

## 4. Play 3: Database Connection Failure
* **Trigger**: HikariCP throws `Connection is not available` exceptions.
* **Symptom**: Actuator dashboard shows databaseStatus as `DOWN`.
* **Response Steps**:
  1. Verify the MySQL database container status:
     ```bash
     docker compose ps mysql
     ```
  2. If the database container is stopped, inspect logs for out-of-memory (OOM) or disk crashes:
     ```bash
     docker logs mysql_db
     ```
  3. Restart the MySQL container:
     ```bash
     docker compose start mysql
     ```
  4. If the database is running but network routing failed:
     * Ping the database host from the app container:
       ```bash
       docker exec -it springboot_app ping mysql
       ```
     * Restart the private bridge network:
       ```bash
       docker network disconnect app-network springboot_app
       docker network connect app-network springboot_app
       ```
