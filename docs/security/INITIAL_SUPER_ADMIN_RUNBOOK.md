# INITIAL_SUPER_ADMIN_RUNBOOK.md

This runbook outlines the operational steps for system administrators to bootstrap, log in, and verify the initial Super Admin account.

---

## 1. Setup Environment Variables
Before launching the container or application instance, define the bootstrap environment variables:

```bash
export SUPER_ADMIN_NAME=superadmin_operator
export SUPER_ADMIN_EMAIL=superadmin@company.com
export SUPER_ADMIN_PASSWORD=SecurePassword@2026!
export SUPER_ADMIN_PHONE=+14155552671
```

*Ensure the password is complex and secure. If it does not meet the 12-character complexity rules, startup will fail securely.*

---

## 2. Launch the Application
Run the startup script or compose file:
```bash
docker compose up -d
```
Inspect logs to verify bootstrap execution:
```bash
docker logs springboot_app | grep "IDENTITY_BOOTSTRAP"
```
*Expected Output*:
```
[INFO] IDENTITY_BOOTSTRAP | Starting secure Super Admin identity bootstrap process...
[INFO] IDENTITY_BOOTSTRAP | Bootstrap state permanently locked in system_settings table.
[INFO] IDENTITY_BOOTSTRAP | Super Admin identity bootstrap completed successfully.
```

---

## 3. Verify Database Persistence
Log in to the database and query the table locks and user roles:
```sql
SELECT username, email, role, password_change_required FROM users WHERE role = 'ROLE_SUPER_ADMIN';
```
*Expected Output*:
* Contains exactly one row matching `superadmin_operator`.
* `password_change_required` is `1` (true).

Verify lock state:
```sql
SELECT * FROM system_settings WHERE setting_key = 'bootstrap.completed';
```
*Expected Output*:
* Contains `bootstrap.completed` with value `true`.

---

## 4. Second Startup Verification
Restart the application:
```bash
docker restart springboot_app
docker logs springboot_app | grep "IDENTITY_BOOTSTRAP"
```
*Expected Output*:
```
[DEBUG] IDENTITY_BOOTSTRAP | Bootstrap lock active or SUPER_ADMIN role already exists. Skipping bootstrap.
```
*No duplicate accounts or settings are generated.*
