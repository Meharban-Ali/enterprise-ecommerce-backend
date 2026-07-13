# BUSINESS FLOW CERTIFICATION

This document details the verification steps, database assertions, and expected results for the primary business flows.

## 1. Flow: User Onboarding to Authenticated Login
* **Purpose**: Verify that users can register, log in, and receive secure JWT tokens.
* **Preconditions**: The application must be running in the local H2 profile.
* **Execution Steps**:
  1. Register a new user: `POST /api/auth/register`
  2. Log in with registered credentials: `POST /api/auth/login`
* **Database Verification**: Confirm that the user record is added to the `users` table with the password column hashed.
* **Redis Verification**: Confirm that login failures increment rate limiting IP block counters in Redis.
* **Pass Criteria**: The registration and login endpoints return successful responses containing a valid JWT access token and refresh token.

---

## 2. Flow: Customer Checkout to Order Completion
* **Purpose**: Verify that customers can check out cart items, and payment callbacks update order statuses.
* **Preconditions**: The user must be authenticated. At least 1 product with available stock must exist in the catalog.
* **Execution Steps**:
  1. Add items to cart: `POST /api/cart/items`
  2. Place order (checkout): `POST /api/orders`
  3. Send a simulated payment success callback to `POST /api/webhooks/stripe`
* **Database Verification**: Confirm that the active cart is cleared, order status transitions to `PAID`, and a notification event is added to the outbox table.
* **Redis Verification**: Confirm that product details caches are evicted on modification.
* **Pass Criteria**: The entire checkout process completes successfully without throwing network or database exceptions.
