# BACKUP & DISASTER RECOVERY PLAN

This document details database backup commands and recovery validation procedures.

## 1. Database Backup Recommendations
Configure daily automated backups using cron tasks:
```bash
mysqldump -u root -p redisdb > backups/backup_db.sql
```
* **Recovery Validation**: Periodically restore database backups to a separate staging environment to verify data integrity.

---

## 2. Redis Caching Considerations
* **Eviction Policies**: Redis key evictions rely on the `volatile-lru` policy. Since the application falls back to SQL queries if Redis goes down, Redis backups are not critical for system recovery.

---

## 3. Disaster Recovery (RTO & RPO Goals)
* **Recovery Time Objective (RTO)**: Recovery within 30 minutes using backup images.
* **Recovery Point Objective (RPO)**: Data loss of less than 24 hours using daily database backups.
