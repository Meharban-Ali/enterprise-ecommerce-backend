# DATABASE PLAYBOOK

This document details database connections verification and backup execution commands.

## 1. Connection Troubleshooting
If you encounter database connection failures:
1. Verify the MySQL server is running:
   ```bash
   mysqladmin -u root -p status
   ```
2. Verify the configuration match details in `.env`:
   ```properties
   SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/redisdb
   ```

---

## 2. Backup & Restore Commands
* **Backup Dump**:
  ```bash
  mysqldump -u root -p redisdb > backups/backup_db.sql
  ```
* **Restore Dump**:
  ```bash
  mysql -u root -p redisdb < backups/backup_db.sql
  ```
