# MASTER FUNCTIONAL CHECKLIST

Use this checklist to verify core functional features before certifying a release.

## 1. Functional Verification Checklist

| Feature Name | Verification Steps | Expected Result | Pass/Fail | Remarks |
| :--- | :--- | :--- | :---: | :--- |
| **User Registration**| Register a new account: `POST /api/auth/register` | Return HTTP 201 Created; database user record is saved. | **PASSED** | |
| **User Login** | Log in with credentials: `POST /api/auth/login` | Return HTTP 200 OK containing JWT access and refresh tokens. | **PASSED** | |
| **Catalog Search** | Query category listing: `GET /api/products?page=0` | Return HTTP 200 OK containing paginated product DTOs. | **PASSED** | |
| **Cart Operations** | Add product ID 1 to cart: `POST /api/cart/items` | Return HTTP 200 OK containing updated cart items. | **PASSED** | |
| **Checkout Order** | Place order: `POST /api/orders` | Return HTTP 201 Created containing pending order DTO. | **PASSED** | |
| **Stripe Callback** | Post payment callback: `POST /api/webhooks/stripe`| Return HTTP 200 OK; order status updates to PAID. | **PASSED** | |
| **Outbox Dispatch** | Query notification logs | Scheduler processes and updates outbox state to SENT. | **PASSED** | |
| **Trace Logging** | Check console output | Logger output matches the structured JSON correlation template. | **PASSED** | |
