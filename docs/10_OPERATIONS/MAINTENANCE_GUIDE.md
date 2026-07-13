# ROUTINE MAINTENANCE GUIDE

This guide details routine maintenance tasks for the eCommerce platform.

## 1. Secrets Rotation Guidelines
* **JWT Secret Rotation**: Regenerate the HMAC secret key every 90 days.
  ```bash
  openssl rand -base64 32
  ```
  Update `JWT_SECRET` in `.env` and restart the application instances sequentially to prevent downtime.

---

## 2. Cache Maintenance
* **Flush Cache Keys**: Clear cached catalog items to force database refreshes during promotions:
  ```bash
  redis-cli FLUSHALL
  ```

---

## 3. Database Maintenance
* **Data Cleanup**: Periodically purge expired refresh tokens to optimize storage space:
  ```sql
  DELETE FROM refresh_tokens WHERE expiry < NOW();
  ```
