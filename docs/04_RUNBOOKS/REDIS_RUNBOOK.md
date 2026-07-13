# REDIS CACHE OPERATIONS

This document details Redis cache verification, flushing, and fallback test procedures.

## 1. Connection Verification
Verify the local Redis cache is running and responding:
```bash
redis-cli ping
```
Expect response: `PONG`.

---

## 2. Flush Caches
To clear cached product listings:
```bash
redis-cli FLUSHALL
```

---

## 3. Fallback Resiliency Verification
To verify that the application falls back to SQL queries if Redis goes down:
1. Stop the local Redis server.
2. Query the product catalog API: `GET /api/products`.
3. Check the application logs. You should see warning alerts indicating the cache connection timed out, but the API should successfully return data fetched directly from the database.
