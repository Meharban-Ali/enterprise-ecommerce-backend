# SECURITY & LOCKOUTS RUNBOOK

This document details security event troubleshooting and credential rotation workflows.

## 1. Authentication Lockouts
If users encounter login lockouts due to rate limits:
* **Wait Limit**: The client IP address rate limiting filter blocks excessive requests for 15 minutes.
* **Manual Reset**: Clear rate limiting counters from Redis for the blocked IP address:
  ```bash
  redis-cli DEL "rate:limit:<ip_address>"
  ```

---

## 2. JWT Key Rotation
To rotate the HMAC JWT secret key in production:
1. Generate a secure 256-bit key:
   ```bash
   openssl rand -base64 32
   ```
2. Update the `JWT_SECRET` key in the production `.env` variables list and restart the application instances sequentially.
