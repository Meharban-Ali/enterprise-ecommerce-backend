# ERROR CATALOG

This catalog maps HTTP status codes, business exceptions, validation errors, and user recovery recommendations.

| HTTP Status | Business Error Code | Validation Error Message | Recommended User Message | Recovery Action |
| :--- | :--- | :--- | :--- | :--- |
| **`400`** | `VALIDATION_FAILED` | `Must be a well-formed email address` | "Please enter a valid email address." | Verify email format. |
| **`400`** | `STOCK_UNAVAILABLE` | `Insufficient stock` | "This product is temporarily out of stock." | Reduce item quantity in cart. |
| **`401`** | `BAD_CREDENTIALS` | `Bad credentials` | "Invalid email or password." | Re-enter credentials. |
| **`401`** | `TOKEN_EXPIRED` | `JWT token has expired` | "Session expired. Please log in again." | Call login to renew token. |
| **`403`** | `ACCESS_DENIED` | `Access Denied` | "You do not have permission to view this resource." | Verify user account permissions. |
| **`409`** | `EMAIL_DUPLICATE` | `Email already registered` | "An account is already registered with this email." | Log in or use a different email. |
| **`429`** | `RATE_LIMIT_EXCEEDED`| `Too Many Requests` | "Too many requests. Please wait a few minutes." | Wait 15 minutes for limit to reset. |
