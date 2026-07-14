# Post-Deployment Smoke Test Specification

This smoke test plan defines the validation steps required to verify that the deployed E-Commerce application is operational.

---

## Test Scenario 1: Verify Health Status
Verify that the liveness and readiness endpoints return `UP`.

* **Method**: `GET`
* **Endpoint**: `/actuator/health`
* **Headers**: None
* **Expected Response**:
  * Status: `200 OK`
  * Body:
    ```json
    {
      "status": "UP",
      "components": {
        "db": { "status": "UP" },
        "redis": { "status": "UP" }
      }
    }
    ```

---

## Test Scenario 2: Super Admin First-Login Lockout
Verify that the seeded Super Admin user is locked out until they change their password.

* **Step 1: Get JWT Token**
  * **Method**: `POST`
  * **Endpoint**: `/api/auth/login`
  * **Body**:
    ```json
    {
      "email": "superadmin@company.com",
      "password": "SecurePassword@2026!"
    }
    ```
  * **Expected Response**: `200 OK` containing a valid `accessToken`.

* **Step 2: Access Products Endpoint (Blocked)**
  * **Method**: `GET`
  * **Endpoint**: `/api/products`
  * **Headers**: `Authorization: Bearer <token>`
  * **Expected Response**:
    * Status: `403 Forbidden`
    * Body:
      ```json
      {
        "message": "Password change required on first login",
        "code": "PASSWORD_CHANGE_REQUIRED"
      }
      ```

---

## Test Scenario 3: Change Password & Unlock Account
Reset the temporary password to unlock the account.

* **Method**: `POST`
* **Endpoint**: `/api/auth/reset-password`
* **Headers**: `Authorization: Bearer <token>`
* **Body**:
  ```json
  {
    "email": "superadmin@company.com",
    "oldPassword": "SecurePassword@2026!",
    "newPassword": "NewPermanentPassword@2026!",
    "confirmPassword": "NewPermanentPassword@2026!"
  }
  ```
* **Expected Response**:
  * Status: `200 OK`
  * Body: `{"message":"Password reset successfully"}`

---

## Test Scenario 4: Access Products Endpoint (Allowed)
Verify that API access is permitted after the password has been reset.

* **Method**: `GET`
* **Endpoint**: `/api/products`
* **Headers**: `Authorization: Bearer <token>`
* **Expected Response**:
  * Status: `200 OK`
  * Body: Paginated list of products.
