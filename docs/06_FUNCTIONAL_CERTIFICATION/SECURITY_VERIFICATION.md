# SECURITY VERIFICATION CHECKLIST

Verify security controls and access rules for all functional components.

## 1. Security Verification Checklist

| Security Component | Verification Method | Expected Result | Pass/Fail |
| :--- | :--- | :--- | :---: |
| **Password Encryption**| Query database user credentials | Confirm password values are hashed with BCrypt (no plaintext). | **PASSED** |
| **JWT Signature validation**| Send request with a malformed signature | Return HTTP `401 Unauthorized` with custom error. | **PASSED** |
| **JWT Expiration** | Send request with an expired access token | Return HTTP `401 Unauthorized`. | **PASSED** |
| **CORS Configuration** | Verify OPTIONS pre-flight request response headers | Returns allowed origins, methods, and headers. | **PASSED** |
| **HSTS Enforcements** | Verify HTTP response headers | Confirm `Strict-Transport-Security` header is present. | **PASSED** |
| **Rate Limiting** | Exceed rate limits using mock tools | Request is blocked by rate limiter; return `429 Too Many Requests`. | **PASSED** |
| **Idempotency** | Resend payment callback request using same token | Returns successful response, but action is not duplicated. | **PASSED** |
