# TESTING RESET PLAYBOOK

Follow these steps to reset the environment to a clean testing state.

## 1. Database Truncate Script
Run the following SQL script to clear test transactions and reset tables:
```sql
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE cart_items;
TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE webhook_deliveries;
TRUNCATE TABLE notification_outbox;
SET FOREIGN_KEY_CHECKS = 1;
```

---

## 2. Redis Cache Flush
Clear rate limiting counters and cached catalog items:
```bash
redis-cli FLUSHALL
```

---

## 3. Log Rotation
Remove temporary log files to verify new correlation IDs:
```bash
rm -f logs/app.log logs/app-structured.json
```
Restart the application to trigger database bootstrapping.
