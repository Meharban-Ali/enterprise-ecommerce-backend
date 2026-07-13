# NEGATIVE TESTING SCENARIOS

Verify how the API handles error conditions by testing the scenarios below.

## 1. Malformed JWT Access Token
* **Endpoint**: `GET {{baseUrl}}/api/cart`
* **Headers**: `Authorization: Bearer invalid-malformed-token`
* **Expected Response (401 Unauthorized)**:
  ```json
  {
    "success": false,
    "message": "Invalid JWT signature",
    "data": null
  }
  ```

---

## 2. Validation Constraints Failure
* **Endpoint**: `POST {{baseUrl}}/api/auth/register`
* **Request JSON**:
  ```json
  {
    "username": "",
    "email": "invalid-email-format",
    "password": "123"
  }
  ```
* **Expected Response (400 Bad Request)**:
  ```json
  {
    "success": false,
    "message": "Validation failed",
    "errors": {
      "email": "Must be a well-formed email address",
      "password": "Password must be at least 8 characters long"
    }
  }
  ```
