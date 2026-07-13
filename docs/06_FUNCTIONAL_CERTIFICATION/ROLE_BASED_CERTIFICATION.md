# ROLE-BASED CERTIFICATION CHECKLIST

Verify access controls and authorization rules for each user role.

## 1. Role Verification Matrix

| Target Endpoint | HTTP Method | Expected User Result | Expected Admin Result | Expected Super Admin Result |
| :--- | :---: | :---: | :---: | :---: |
| `/api/cart/**` | `POST` | `200 OK` | `403 Forbidden` | `403 Forbidden` |
| `/api/orders/**` | `POST` | `201 Created` | `403 Forbidden` | `403 Forbidden` |
| `/api/categories` | `POST` | `403 Forbidden` | `201 Created` | `201 Created` |
| `/api/super-admin/**` | `POST` | `403 Forbidden` | `403 Forbidden` | `200 OK` |

---

## 2. Authorization Rules Verification
* **401 Unauthorized**: Send a request to a secured route (e.g., `/api/cart`) without an `Authorization` header. Expect status `401 Unauthorized` with a custom error envelope.
* **403 Forbidden**: Log in as a customer (`customer@example.com`), then send a request to create a category: `POST /api/categories`. Expect status `403 Forbidden`.
