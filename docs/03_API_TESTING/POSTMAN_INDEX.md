# POSTMAN INDEX & SETUP GUIDE

To manually certify the APIs using Postman, import the variables and follow the execution sequence detailed below.

## 1. Postman Environment Variables

Ensure the following variables are configured in your Postman Environment:

| Variable Key | Default DEV Value | Description |
| :--- | :--- | :--- |
| `baseUrl` | `http://localhost:8080` | Root host address of the Spring Boot application |
| `jwt_token` | `{{auth_response.data.accessToken}}` | Captured automatically from the login response |
| `refresh_token` | `{{auth_response.data.refreshToken}}` | UUID token used to renew the access token |
| `productId` | `1` | ID of the target product created for testing |
| `categoryId` | `1` | ID of the category created for testing |

---

## 2. API Execution Sequence

To prevent constraint violations, execute calls in this sequence:

```
[1. Liveness check] ──> [2. Register User] ──> [3. Login User]
                                                    │
[6. Check Notification] <── [5. Stripe Webhook] <── [4. Order Checkout]
```
