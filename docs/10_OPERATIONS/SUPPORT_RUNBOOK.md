# PRODUCTION SUPPORT RUNBOOK

This runbook helps support teams troubleshoot and resolve issues in production.

## 1. Trace ID Investigation Flow
When a customer reports an issue:
1. Retrieve the `X-Correlation-ID` header from the response or request logs.
2. Query the log aggregation tool (e.g., Elasticsearch, Splunk) for the matching trace ID to view all related log statements.

---

## 2. Dependency Connection Checks
* **MySQL Check**: Query table state to verify connection pool health:
  ```sql
  SELECT 1;
  ```
* **Redis Check**: Verify cache health using ping commands:
  ```bash
  redis-cli ping
  ```
  Expect response: `PONG`.

---

## 3. Webhook Delivery Checks
Verify webhook event processing logs:
```sql
SELECT event_type, delivery_status, response_status FROM webhook_deliveries ORDER BY created_at DESC LIMIT 10;
```
If failures occur, investigate signature headers and verify secret key configurations in `.env`.
