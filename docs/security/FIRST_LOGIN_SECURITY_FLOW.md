# FIRST_LOGIN_SECURITY_FLOW.md

This document describes the security flow enforced upon the Super Admin's first login attempt.

---

## 1. Initial Login & JWT Retrieval
The Super Admin authenticates via the standard auth endpoint:
```bash
POST /api/auth/login
Body:
{
  "email": "superadmin@company.com",
  "password": "SecurePassword@2026!"
}
```
*Response*:
Returns the JWT access token and refresh token:
```json
{
  "status": "SUCCESS",
  "message": "User logged in successfully",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "7c88b90a..."
  }
}
```

---

## 2. API Authorization Lockout
If the Super Admin attempts to access any functional API endpoint (e.g. `/api/admin/dashboard` or `/api/admin/reliability/dashboard`) using the retrieved token before changing their password:
1. `JwtAuthenticationFilter` intercepts the request.
2. The filter extracts the user identity, checks `user.isPasswordChangeRequired()`, and detects it is `true`.
3. If the request URI is NOT `/api/auth/reset-password`, the filter blocks the request, returning **HTTP 403 Forbidden** immediately:
   ```json
   {
     "message": "Password change required on first login",
     "code": "PASSWORD_CHANGE_REQUIRED"
   }
   ```
4. Access to all operational resources is completely denied.

---

## 3. Password Reset Request (First Login)
To unlock the account, the Super Admin must submit a password reset request containing their temporary password and new password:
```bash
POST /api/auth/reset-password
Headers: Authorization: Bearer <token>
Body:
{
  "email": "superadmin@company.com",
  "oldPassword": "SecurePassword@2026!",
  "newPassword": "MyPermanentSecurePassword@2026!",
  "confirmPassword": "MyPermanentSecurePassword@2026!"
}
```
*Expected Actions*:
1. `ResetPasswordServiceImpl` validates that `oldPassword` matches the current DB hash.
2. The service encrypts `newPassword` and saves it.
3. The service clears the restriction: `user.setPasswordChangeRequired(false)`.
4. Subsequent API calls using the JWT token are allowed, as the filter detects `passwordChangeRequired` is now `false`.
