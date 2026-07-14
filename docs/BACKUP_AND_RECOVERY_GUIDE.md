# Backup and Recovery Guide

This guide documents the procedures for backing up and restoring the database for the Spring Boot eCommerce Backend.

---

## 1. Automated Backups Strategy
* **RPO Target**: 24 Hours
* **RTO Target**: 4 Hours
* **Storage Path**: `./backups/` directory on the host machine.
* **Mechanism**:
  * [BackupService.java](file:///D:/Meharban_Code/ecommerce/src/main/java/com/redis/reliability/service/BackupService.java) triggers full gzip-compressed SQL dumps.
  * Files are named in the format: `backup_v[Version]_[Type]_[Timestamp].sql.gz`.
  * Automated retention cleanups purge files older than the configured limit (`backupRetentionDays`, defaults to 7 days).

---

## 2. Dynamic Backup Verification
After generating a backup file:
1. The system calculates the SHA-256 hash checksum of the file.
2. The system verifies that the archive can be read cleanly.
3. The verification status is updated in the database:
   * `VERIFIED`: Checksum match and readable.
   * `CORRUPTED`: Checksum mismatch or unreadable.

---

## 3. Manual Database Restoration Play (Operational Step)
Database restorations are critical operations. The system protects against accidental restores by requiring a dynamic confirmation token.

### Step 1: Request Safety Confirmation Token
To request a restoration, call the `PlatformReliabilityController` or trigger token generation:
1. Request a token for restoring backup ID `X`:
   * The system calls `ProductionSafetyService.generateConfirmationToken("RESTORE_" + X)`.
   * A dynamic token (e.g. `zXyW129A_...`) is returned and printed to logs.

### Step 2: Trigger the Restore
Execute the restoration request, passing the token:
```bash
POST /api/admin/reliability/restore
Headers: Authorization: Bearer <SuperAdminJWT>
Body:
{
  "backupId": X,
  "restoreType": "FULL",
  "dryRun": false,
  "confirmationToken": "zXyW129A_..."
}
```
* **Dry Run Mode (`dryRun=true`)**: Validates the backup archive readability without applying changes to the database tables.
* **Live Mode (`dryRun=false`)**: Requires the token, validates the checksum, and triggers the restore.

---

## 4. Disaster Recovery (DR) Command Line Reversion
If the REST API dashboard is unavailable, perform a command-line restoration directly inside the containers:

1. Decompress the gzip backup file:
   ```bash
   gunzip -c backups/backup_vX_full_YYYYMMDD_HHMMSS.sql.gz > restore.sql
   ```
2. Apply the SQL script to the MySQL database container:
   ```bash
   docker exec -i mysql_db mysql -u ecommerce_user -pyour_secure_db_password ecommerce < restore.sql
   ```
3. Check application logs to confirm the schema validates cleanly on reboot:
   ```bash
   docker restart springboot_app
   docker logs springboot_app
   ```
