# DEFAULT TESTING CREDENTIALS

This document details the default user accounts and credentials populated on startup.

## 1. Default Accounts Matrix

| Role | Username (Email) | Password | Target Security Group |
| :--- | :--- | :--- | :--- |
| **SUPER_ADMIN** | `admin@example.com` | `Admin123!` | `ROLE_SUPER_ADMIN` |
| **ADMIN** | `merchant@example.com` | `Merchant123!` | `ROLE_ADMIN` |
| **USER** | `customer@example.com` | `Customer123!` | `ROLE_USER` |

---

## 2. Profile Security Guidelines
* **Development & Staging Profiles**: Auto-creation of the default accounts matrix is enabled via `app.data-initializer.enabled=true` to simplify testing.
* **Production Profile**: **Eager initialization of default credentials is disabled**. Production environments must seed initial admin credentials securely via external variables.

---

## 3. JWT Token Configurations
* **HS256 Secret Key**: Set via `JWT_SECRET` in `.env`.
* **Access Token Lifespan**: Configured to 25 minutes.
* **Refresh Token Lifespan**: Configured to 7 days.
