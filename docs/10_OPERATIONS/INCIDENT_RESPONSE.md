# INCIDENT RESPONSE GUIDE

This guide provides action plans for handling production incidents.

---

### Incident 1: High Database Connection Latency
* **Symptoms**: API response times exceed 5000ms; Hikari pool logs connection timeouts: `Connection is not available, request timed out after...`
* **Possible Causes**: Unindexed queries, connection leaks, database CPU spikes.
* **Investigation Steps**:
  1. Check database CPU and memory metrics.
  2. Find slow queries:
     ```sql
     SHOW FULL PROCESSLIST;
     ```
* **Resolution**:
  1. Add missing indexes for slow queries.
  2. Increase pool size limits in `application-prod.properties` if load exceeds capacities.
* **Escalation**: Level 2 (Database Administrator / Backend Lead).

---

### Incident 2: Mail Outbox Schedulers Failure
* **Symptoms**: Customer transaction emails are not sent; outbox rows remain in `PENDING` or `FAILED` states.
* **Possible Causes**: SMTP server is down or credentials have expired.
* **Investigation Steps**:
  1. Check connection status in outbox table:
     ```sql
     SELECT recipient, state, retry_count, failure_reason FROM notification_outbox WHERE state = 'FAILED';
     ```
* **Resolution**:
  1. Verify SMTP connection settings in `.env`.
  2. Once resolved, the outbox scheduler will automatically retry and dispatch pending notifications.
* **Escalation**: Level 1 (Infrastructure Team).
