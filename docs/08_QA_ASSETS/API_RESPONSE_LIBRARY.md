# API RESPONSE LIBRARY

This library documents the standard JSON response structures returned by the platform APIs.

## 1. Standard Success Envelope (HTTP 200/201)
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    "id": 1,
    "name": "Super Phone 12"
  }
}
```

---

## 2. Validation Constraints Error (HTTP 400)
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Must be a well-formed email address"
  }
}
```

---

## 3. Security Credentials Failures (HTTP 401/403)
* **Expired Token (HTTP 401)**:
  ```json
  {
    "success": false,
    "message": "JWT token has expired",
    "data": null
  }
  ```
* **Insufficient Role Scope (HTTP 403)**:
  ```json
  {
    "success": false,
    "message": "Access Denied",
    "data": null
  }
  ```
