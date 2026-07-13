# DATA VERIFICATION GUIDE

Follow these steps to verify database state, caching, and logs consistency during testing.

## 1. Database State Verification
Verify database state by querying tables directly:
* **Verify User Role**:
  ```sql
  SELECT email, role FROM users WHERE email = 'tester@example.com';
  ```
* **Verify Order Status**:
  ```sql
  SELECT id, status, total_amount FROM orders WHERE id = 1;
  ```

---

## 2. Redis Caching Verification
* **Check Redis Cache Keys**:
  ```bash
  redis-cli KEYS "products::*"
  ```
* **Verify Cache Expiry (TTL)**:
  ```bash
  redis-cli TTL "products::1"
  ```
  Ensure the returned value is greater than 0, indicating the cache key has a valid expiration.

---

## 3. Transactional Outbox Logs Verification
Confirm that notification outbox events are logged and processed:
* **Verify Log Entry**:
  ```sql
  SELECT id, recipient, state, retry_count FROM notification_outbox WHERE recipient = 'tester@example.com';
  ```
  Ensure the state updates to `SENT` after the outbox scheduler runs.
