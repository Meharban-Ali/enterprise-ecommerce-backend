# EMERGENCY_RECOVERY_PROCEDURE.md

This document details the enterprise recovery procedures to follow if the only Super Admin account is deleted, compromised, or permanently locked out.

---

## 1. Safety Policy Rules
* **No Auto-Recreation**: Application APIs must **never** automatically recreate a Super Admin account.
* **Server Access Required**: Recovery requires direct, privileged access to the database host or the deployment infrastructure console.
* **Audit Enforcement**: Any manual recovery actions must be logged and approved by the security committee.

---

## 2. Recovery Workflow

### Step 1: Secure Approval & Ticket Logging
Log an emergency security ticket before executing any database scripts. Ensure you have the approval hash keys from the Security Operations Center (SOC).

### Step 2: Establish Server-Level Database Connection
Access the database host or target container shell:
```bash
docker exec -it mysql_db mysql -u root -p
```

### Step 3: Remove the Bootstrap Completion Lock
To allow the bootstrap logic to re-evaluate environment credentials on the next startup, clear the permanent lock from the `system_settings` table:
```sql
USE ecommerce;
DELETE FROM system_settings WHERE setting_key = 'bootstrap.completed';
```

### Step 4: Re-inject Temporary Configuration Variables
In the orchestrator or host env console, set the temporary recovery credentials:
```bash
export SUPER_ADMIN_NAME=superadmin_recovery
export SUPER_ADMIN_EMAIL=recovery_superadmin@company.com
export SUPER_ADMIN_PASSWORD=EmergencyPassword@2026!
export SUPER_ADMIN_PHONE=+14155551234
```

### Step 5: Restart the Application
Restart the Spring Boot instance to trigger the initialization seeder:
```bash
docker compose restart app
```
Verify successful recreation from logs:
```bash
docker logs springboot_app | grep "IDENTITY_BOOTSTRAP"
```

### Step 6: Log in and Enforce Password Change
1. Log in via `/api/auth/login` using the temporary credentials to get a JWT token.
2. Immediately reset the recovery password via `/api/auth/reset-password`, changing it to a permanent, secure credentials store password.
3. Verify that the `password_change_required` flag on the user is cleared.
4. Clear the environment variables from the server console:
   ```bash
   unset SUPER_ADMIN_NAME
   unset SUPER_ADMIN_EMAIL
   unset SUPER_ADMIN_PASSWORD
   unset SUPER_ADMIN_PHONE
   ```
