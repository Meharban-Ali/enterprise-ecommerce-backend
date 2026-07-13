# TROUBLESHOOTING KNOWLEDGE BASE

Use this reference guide to resolve common runtime issues.

---

### Issue 1: Port 8080 Conflict
* **Symptoms**: Application fails to start with error: `Web server failed to start. Port 8080 was already in use.`
* **Root Cause**: Another service is running on port 8080.
* **Resolution**:
  1. Find and terminate the process using port 8080 (Windows):
     ```powershell
     Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force
     ```
  2. Alternatively, configure the application to use a different port in `.env`:
     ```properties
     server.port=9090
     ```

---

### Issue 2: DB Connection Failure
* **Symptoms**: Application fails to boot, throwing `java.net.ConnectException: Connection refused`.
* **Root Cause**: MySQL server is stopped or port configs are mismatched.
* **Resolution**:
  1. Verify the MySQL service is running.
  2. Confirm credentials configurations match `.env`.
