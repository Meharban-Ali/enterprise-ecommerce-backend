# DATABASE VERIFICATION BASELINE

Ensure the database matches this baseline state before initiating testing.

## 1. Expected Row Counts (Fresh Seed)
When the database seeds with `app.data-initializer.enabled=true`, confirm baseline counts:

| Table | Expected Row Count | Verification Query |
| :--- | :---: | :--- |
| `users` | `3` | `SELECT COUNT(*) FROM users;` |
| `categories` | `3` | `SELECT COUNT(*) FROM categories;` |
| `products` | `4` | `SELECT COUNT(*) FROM products;` |

---

## 2. Integrity Checks
Verify role assignments before executing tests:
```sql
SELECT email, role FROM users;
```
Ensure output matches:
* `admin@example.com` -> `ROLE_SUPER_ADMIN`
* `merchant@example.com` -> `ROLE_ADMIN`
* `customer@example.com` -> `ROLE_USER`
