# Enterprise E-Commerce System — Postman API Testing Guide

A comprehensive, production-grade API testing manual for the Enterprise E-Commerce platform. Every endpoint in this document is derived strictly from authoritative `@RestController` annotations, Request/Response DTO schemas, and domain entities across the codebase.

---

## Table of Contents

1. [Authentication Module (`/api/auth`)](#1-authentication-module-apiauth)
2. [User & Profile Module (`/api/user`, `/api/super-admin`)](#2-user--profile-module-apiuser-apisuper-admin)
3. [Product Catalog Module (`/api/products`)](#3-product-catalog-module-apiproducts)
4. [Category Module (`/api/categories`)](#4-category-module-apicategories)
5. [Shopping Cart Module (`/api/cart`)](#5-shopping-cart-module-apicart)
6. [Wishlist Module (`/api/wishlist`)](#6-wishlist-module-apiwishlist)
7. [Order Processing Module (`/api/orders`)](#7-order-processing-module-apiorders)
8. [Payment & Checkout Module (`/api/payments`)](#8-payment--checkout-module-apipayments)
9. [Inbound Payment Webhooks (`/api/webhooks`)](#9-inbound-payment-webhooks-apiwebhooks)
10. [Notification & Preferences Module (`/api/notifications`)](#10-notification--preferences-module-apinotifications)
11. [Admin Notification Templates (`/api/admin/notification-templates`)](#11-admin-notification-templates-apiadminnotification-templates)
12. [Outbound Webhook Management (`/api/admin/webhooks`)](#12-outbound-webhook-management-apiadminwebhooks)
13. [Audit & Compliance Framework (`/api/admin/audit`)](#13-audit--compliance-framework-apiadminaudit)
14. [Incident & Alerting Management (`/api/admin/incidents`, `/api/admin/alerts`)](#14-incident--alerting-management-apiadminincidents-apiadminalerts)
15. [System Monitoring & Metrics (`/api/admin/system`, `/api/analytics`)](#15-system-monitoring--metrics-apiadminsystem-apianalytics)
16. [Platform Reliability & Disaster Recovery (`/api/admin/reliability`)](#16-platform-reliability--disaster-recovery-apiadminreliability)
17. [API Governance & Security Keys (`/api/admin`)](#17-api-governance--security-keys-apiadmin)
18. [Observability & Diagnostics (`/api/admin/observability`)](#18-observability--diagnostics-apiadminobservability)
19. [Public Health & Readiness Probes (`/api/health`, `/actuator/health`)](#19-public-health--readiness-probes-apihealth-actuatorhealth)

---

## Standard Response Wrappers

All endpoints return a standardized `ApiResponse<T>` JSON envelope:

### Standard Success Envelope
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {},
  "timestamp": 1722000000000
}
```

### Standard Error Envelope
```json
{
  "success": false,
  "message": "Detailed error description",
  "errorCode": "INVALID_INPUT",
  "timestamp": 1722000000000
}
```

---

## 1. Authentication Module (`/api/auth`)

### 1.1 Register User
* **Method & Path**: `POST /api/auth/register`
* **Description**: Registers a new customer account with security question credentials.
* **Authentication**: None (Public)
* **Parameters**: None

#### Sample Request Body
```json
{
  "username": "johndoe",
  "email": "john.doe@example.com",
  "password": "Password123!",
  "role": "ROLE_USER",
  "securityQuestion": "What was the name of your first pet?",
  "securityAnswer": "Fluffy"
}
```

#### Sample Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": 101,
    "username": "johndoe",
    "email": "john.doe@example.com",
    "role": "ROLE_USER",
    "createdAt": "2026-07-27T01:00:00"
  },
  "timestamp": 1722000000000
}
```

#### Sample Error Response (`400 Bad Request`)
```json
{
  "success": false,
  "message": "Email already exists in system",
  "errorCode": "DUPLICATE_EMAIL",
  "timestamp": 1722000000000
}
```

---

### 1.2 Login User
* **Method & Path**: `POST /api/auth/login`
* **Description**: Authenticates credentials and returns a JWT access token & refresh token.
* **Authentication**: None (Public)
* **Parameters**: None

#### Sample Request Body
```json
{
  "email": "john.doe@example.com",
  "password": "Password123!"
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "User logged in successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIsImF1dGgiOiJST0xFX1VTRVIifQ...",
    "refreshToken": "d8a7c2b4-e5f6-4a1b-9c8d-7e6f5a4b3c2d",
    "tokenType": "Bearer",
    "userId": 101,
    "email": "john.doe@example.com",
    "role": "ROLE_USER"
  },
  "timestamp": 1722000000000
}
```

#### Sample Error Response (`401 Unauthorized`)
```json
{
  "success": false,
  "message": "Invalid email or password",
  "errorCode": "BAD_CREDENTIALS",
  "timestamp": 1722000000000
}
```

---

### 1.3 Refresh Token
* **Method & Path**: `POST /api/auth/refresh`
* **Description**: Generates a new JWT access token using a valid refresh token.
* **Authentication**: None (Public)

#### Sample Request Body
```json
{
  "refreshToken": "d8a7c2b4-e5f6-4a1b-9c8d-7e6f5a4b3c2d"
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huLmRvZUBleGFtcGxlLmNvbSIs...",
    "refreshToken": "d8a7c2b4-e5f6-4a1b-9c8d-7e6f5a4b3c2d",
    "tokenType": "Bearer"
  },
  "timestamp": 1722000000000
}
```

---

### 1.4 Logout User
* **Method & Path**: `POST /api/auth/logout`
* **Description**: Invalidates the active JWT session / refresh token.
* **Authentication**: None (Public)

#### Sample Request Body
```json
{
  "refreshToken": "d8a7c2b4-e5f6-4a1b-9c8d-7e6f5a4b3c2d"
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "User logged out and token invalidated successfully",
  "data": null,
  "timestamp": 1722000000000
}
```

---

### 1.5 Forgot Password (Retrieve Security Question)
* **Method & Path**: `POST /api/auth/forgot-password`
* **Description**: Retrieves the configured security question for an account.
* **Authentication**: None (Public)

#### Sample Request Body
```json
{
  "email": "john.doe@example.com"
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "email": "john.doe@example.com",
  "securityQuestion": "What was the name of your first pet?"
}
```

---

### 1.6 Verify Security Answer
* **Method & Path**: `POST /api/auth/forgot-password/verify`
* **Description**: Verifies the answer to the security question and issues a password reset token.
* **Authentication**: None (Public)

#### Sample Request Body
```json
{
  "email": "john.doe@example.com",
  "securityAnswer": "Fluffy"
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Security answer verified successfully",
  "data": "reset-token-uuid-12345",
  "timestamp": 1722000000000
}
```

---

### 1.7 Reset Forgot Password
* **Method & Path**: `POST /api/auth/forgot-password/reset`
* **Description**: Resets password using the verified reset token.
* **Authentication**: None (Public)

#### Sample Request Body
```json
{
  "email": "john.doe@example.com",
  "resetToken": "reset-token-uuid-12345",
  "newPassword": "NewStrongPassword123!"
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": null,
  "timestamp": 1722000000000
}
```

---

### 1.8 Reset Password (Authenticated)
* **Method & Path**: `POST /api/auth/reset-password`
* **Description**: Authenticated user changes password using current password.
* **Authentication**: None / User JWT

#### Sample Request Body
```json
{
  "email": "john.doe@example.com",
  "currentPassword": "Password123!",
  "newPassword": "NewStrongPassword123!"
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Password reset successfully.",
  "data": null,
  "timestamp": 1722000000000
}
```

---

## 2. User & Profile Module (`/api/user`, `/api/super-admin`)

### 2.1 Get Current User Profile
* **Method & Path**: `GET /api/user/profile`
* **Description**: Retrieves profile details for the authenticated user.
* **Authentication**: `ROLE_USER`

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Profile details retrieved successfully",
  "data": {
    "id": 101,
    "username": "johndoe",
    "email": "john.doe@example.com",
    "role": "ROLE_USER",
    "accountEnabled": true,
    "accountNonLocked": true,
    "createdAt": "2026-07-27T01:00:00",
    "updatedAt": "2026-07-27T01:00:00"
  },
  "timestamp": 1722000000000
}
```

---

### 2.2 Update Profile
* **Method & Path**: `PUT /api/user/profile`
* **Description**: Updates profile details (email, username).
* **Authentication**: `ROLE_USER`

#### Sample Request Body
```json
{
  "username": "john_updated",
  "email": "john.updated@example.com"
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": 101,
    "username": "john_updated",
    "email": "john.updated@example.com",
    "role": "ROLE_USER",
    "accountEnabled": true,
    "accountNonLocked": true
  },
  "timestamp": 1722000000000
}
```

---

### 2.3 Super Admin List Users
* **Method & Path**: `GET /api/super-admin/users`
* **Description**: Super admin endpoint to filter and paginate user accounts.
* **Authentication**: `ROLE_SUPER_ADMIN`
* **Parameters**:
  - `role` (query, string, optional): `ROLE_USER`, `ROLE_ADMIN`
  - `search` (query, string, optional)
  - `enabled` (query, boolean, optional)
  - `nonLocked` (query, boolean, optional)
  - `page` (query, int, default: 0)
  - `size` (query, int, default: 20)

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Users list retrieved successfully",
  "data": {
    "content": [
      {
        "id": 101,
        "username": "johndoe",
        "email": "john.doe@example.com",
        "role": "ROLE_USER",
        "accountEnabled": true,
        "accountNonLocked": true
      }
    ],
    "pageable": { "pageNumber": 0, "pageSize": 20 },
    "totalElements": 1,
    "totalPages": 1
  },
  "timestamp": 1722000000000
}
```

---

### 2.4 Super Admin User Status Engine
* **Method & Path**: `PATCH /api/super-admin/users/{id}/{action}`
* **Actions**: `activate`, `deactivate`, `enable`, `disable`, `lock`, `unlock`
* **Authentication**: `ROLE_SUPER_ADMIN`

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "User account locked successfully",
  "data": {
    "id": 101,
    "username": "johndoe",
    "accountNonLocked": false
  },
  "timestamp": 1722000000000
}
```

---

## 3. Product Catalog Module (`/api/products`)

### 3.1 Create Product
* **Method & Path**: `POST /api/products`
* **Description**: Creates a new catalog product (Admin only).
* **Authentication**: `ROLE_ADMIN` / `ROLE_SUPER_ADMIN`

#### Sample Request Body
```json
{
  "name": "Wireless Noise-Canceling Headphones",
  "description": "Premium over-ear Bluetooth headphones with active noise cancellation.",
  "price": 299.99,
  "stockQuantity": 50,
  "categoryId": 5,
  "sku": "AUD-WNC-001"
}
```

#### Sample Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Product created successfully",
  "data": {
    "id": 42,
    "name": "Wireless Noise-Canceling Headphones",
    "description": "Premium over-ear Bluetooth headphones with active noise cancellation.",
    "price": 299.99,
    "stockQuantity": 50,
    "categoryName": "Audio",
    "sku": "AUD-WNC-001",
    "createdAt": "2026-07-27T01:00:00"
  },
  "timestamp": 1722000000000
}
```

---

### 3.2 Get Product by ID
* **Method & Path**: `GET /api/products/{id}`
* **Description**: Retrieves product details by ID (Redis cached).
* **Authentication**: `ROLE_USER` / `ROLE_ADMIN`

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Product fetched from Redis cache",
  "data": {
    "id": 42,
    "name": "Wireless Noise-Canceling Headphones",
    "price": 299.99,
    "stockQuantity": 50
  },
  "timestamp": 1722000000000
}
```

---

### 3.3 Search Products by Name
* **Method & Path**: `GET /api/products/search`
* **Description**: Case-insensitive product search.
* **Authentication**: `ROLE_USER` / `ROLE_ADMIN`
* **Parameters**: `name` (required, string), `page` (default 0), `size` (default 10)

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Search results for: Headphones",
  "data": {
    "content": [
      { "id": 42, "name": "Wireless Noise-Canceling Headphones", "price": 299.99 }
    ]
  },
  "timestamp": 1722000000000
}
```

---

## 4. Category Module (`/api/categories`)

### 4.1 Create Category
* **Method & Path**: `POST /api/categories`
* **Description**: Creates a new product category.
* **Authentication**: `ROLE_ADMIN` / `ROLE_SUPER_ADMIN`

#### Sample Request Body
```json
{
  "name": "Audio & Sound",
  "description": "Headphones, speakers, and audio accessories."
}
```

#### Sample Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Category created successfully",
  "data": {
    "id": 5,
    "name": "Audio & Sound",
    "description": "Headphones, speakers, and audio accessories."
  },
  "timestamp": 1722000000000
}
```

---

## 5. Shopping Cart Module (`/api/cart`)

### 5.1 Add Item to Cart
* **Method & Path**: `POST /api/cart/items`
* **Description**: Adds a product item and quantity to the customer's cart.
* **Authentication**: `ROLE_USER`

#### Sample Request Body
```json
{
  "productId": 42,
  "quantity": 2
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Item added to cart successfully",
  "data": {
    "cartId": 12,
    "totalPrice": 599.98,
    "items": [
      {
        "itemId": 88,
        "productId": 42,
        "productName": "Wireless Noise-Canceling Headphones",
        "quantity": 2,
        "unitPrice": 299.99,
        "subtotal": 599.98
      }
    ]
  },
  "timestamp": 1722000000000
}
```

---

## 6. Wishlist Module (`/api/wishlist`)

### 6.1 Add Product to Wishlist
* **Method & Path**: `POST /api/wishlist/products/{productId}`
* **Description**: Adds a product to the user's saved wishlist.
* **Authentication**: `ROLE_USER`

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Product added to wishlist successfully",
  "data": {
    "wishlistId": 15,
    "products": [
      { "id": 42, "name": "Wireless Noise-Canceling Headphones", "price": 299.99 }
    ]
  },
  "timestamp": 1722000000000
}
```

---

## 7. Order Processing Module (`/api/orders`)

### 7.1 Place Order
* **Method & Path**: `POST /api/orders`
* **Description**: Converts the user's cart items into a confirmed order.
* **Authentication**: `ROLE_USER`

#### Sample Request Body
```json
{
  "shippingAddress": "123 Innovation Way, Tech District, CA 90210",
  "paymentMethod": "STRIPE"
}
```

#### Sample Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Order placed successfully",
  "data": {
    "orderId": 501,
    "orderNumber": "ORD-2026-99104",
    "totalAmount": 599.98,
    "status": "PENDING",
    "shippingAddress": "123 Innovation Way, Tech District, CA 90210",
    "createdAt": "2026-07-27T01:05:00"
  },
  "timestamp": 1722000000000
}
```

---

## 8. Payment & Checkout Module (`/api/payments`)

### 8.1 Create Payment Session
* **Method & Path**: `POST /api/payments/create`
* **Description**: Creates a payment gateway session (Stripe or Razorpay).
* **Authentication**: `ROLE_USER`

#### Sample Request Body
```json
{
  "orderId": 501,
  "gateway": "STRIPE"
}
```

#### Sample Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Payment session created successfully",
  "data": {
    "paymentId": 801,
    "orderId": 501,
    "amount": 599.98,
    "status": "PENDING",
    "gateway": "STRIPE",
    "clientSecret": "pi_3MtwB2LkdIwHu7ix08W53V_secret_test123"
  },
  "timestamp": 1722000000000
}
```

---

### 8.2 Process Refund
* **Method & Path**: `POST /api/payments/{paymentId}/refund`
* **Description**: Processes a partial or full refund for a payment.
* **Authentication**: `ROLE_USER` / `ROLE_ADMIN`

#### Sample Request Body
```json
{
  "amount": 200.00,
  "reason": "Customer requested partial refund for item damage"
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Refund processed successfully",
  "data": {
    "paymentId": 801,
    "refundId": 92,
    "refundAmount": 200.00,
    "status": "PARTIALLY_REFUNDED"
  },
  "timestamp": 1722000000000
}
```

---

## 9. Inbound Payment Webhooks (`/api/webhooks`)

### 9.1 Handle Stripe Webhook
* **Method & Path**: `POST /api/webhooks/stripe`
* **Description**: External callback handler for Stripe event webhooks (Idempotent via Redis SETNX).
* **Authentication**: Signature header (`Stripe-Signature`)
* **Headers**: `Stripe-Signature: t=1672531199,v1=abcdef...`, `Idempotency-Key: evt_123`

#### Sample Request Body
```json
{
  "id": "evt_1MtwB2LkdIwHu7ix08W53V",
  "type": "payment_intent.succeeded",
  "data": {
    "object": {
      "id": "pi_3MtwB2LkdIwHu7ix08W53V",
      "amount": 59998,
      "currency": "usd",
      "status": "succeeded"
    }
  }
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Stripe webhook processed successfully",
  "data": "Event Received",
  "timestamp": 1722000000000
}
```

---

## 10. Notification & Preferences Module (`/api/notifications`)

### 10.1 Get My Notifications
* **Method & Path**: `GET /api/notifications/my`
* **Description**: Retrieves paginated notifications for the logged-in user.
* **Authentication**: `ROLE_USER`

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Notifications fetched successfully",
  "data": {
    "content": [
      {
        "id": 301,
        "recipientEmail": "john.doe@example.com",
        "subject": "Order Confirmation - ORD-2026-99104",
        "message": "Your order has been placed successfully.",
        "read": false,
        "createdAt": "2026-07-27T01:05:00"
      }
    ]
  },
  "timestamp": 1722000000000
}
```

---

## 11. Admin Notification Templates (`/api/admin/notification-templates`)

### 11.1 Create Notification Template
* **Method & Path**: `POST /api/admin/notification-templates`
* **Description**: Defines a customizable HTML email template.
* **Authentication**: `ROLE_ADMIN` / `ROLE_SUPER_ADMIN`

#### Sample Request Body
```json
{
  "templateCode": "ORDER_CONFIRMATION",
  "templateName": "Order Confirmation Email",
  "channel": "EMAIL",
  "subjectTemplate": "Order Confirmation - {{data.orderNumber}}",
  "bodyTemplate": "<h1>Thank you for your order!</h1><p>Order Total: ${{data.totalAmount}}</p>"
}
```

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Template created successfully",
  "data": {
    "id": 14,
    "templateCode": "ORDER_CONFIRMATION",
    "version": 1,
    "active": true
  },
  "timestamp": 1722000000000
}
```

---

## 12. Outbound Webhook Management (`/api/admin/webhooks`)

### 12.1 Register Outbound Webhook Endpoint
* **Method & Path**: `POST /api/admin/webhooks`
* **Description**: Registers an external webhook consumer endpoint.
* **Authentication**: `ROLE_ADMIN` / `ROLE_SUPER_ADMIN`

#### Sample Request Body
```json
{
  "name": "ERP Integration Webhook",
  "targetUrl": "https://erp.enterprise.com/webhooks/orders",
  "secretKey": "whsec_supersecretkey123",
  "filterChannel": "EMAIL",
  "retryEnabled": true,
  "maxRetryCount": 3
}
```

#### Sample Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "Webhook endpoint registered successfully",
  "data": {
    "id": 7,
    "name": "ERP Integration Webhook",
    "targetUrl": "https://erp.enterprise.com/webhooks/orders",
    "enabled": true
  },
  "timestamp": 1722000000000
}
```

---

## 13. Audit & Compliance Framework (`/api/admin/audit`)

### 13.1 Search Audit Logs
* **Method & Path**: `GET /api/admin/audit/logs`
* **Description**: Searches compliance audit logs with multi-attribute filtering.
* **Authentication**: `ROLE_ADMIN` / `ROLE_SUPER_ADMIN`
* **Parameters**: `userId`, `actionType`, `resourceType`, `startDate`, `endDate`, `page`, `size`

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Audit logs retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1005,
        "eventId": "evt_99812",
        "actor": "admin@enterprise.com",
        "actionType": "USER_LOGIN",
        "status": "SUCCESS",
        "createdAt": "2026-07-27T01:00:00"
      }
    ]
  },
  "timestamp": 1722000000000
}
```

---

## 14. Incident & Alerting Management (`/api/admin/incidents`, `/api/admin/alerts`)

### 14.1 Get Incident Dashboard
* **Method & Path**: `GET /api/admin/incidents/dashboard`
* **Description**: Retrieves real-time incident statistics, SLA breaches, and open alerts.
* **Authentication**: `ROLE_ADMIN` / `ROLE_SUPER_ADMIN`

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "Incident dashboard stats retrieved successfully",
  "data": {
    "totalOpenIncidents": 0,
    "criticalIncidentsCount": 0,
    "slaBreachedCount": 0
  },
  "timestamp": 1722000000000
}
```

---

## 15. System Monitoring & Metrics (`/api/admin/system`, `/api/analytics`)

### 15.1 Get Operational System Metrics
* **Method & Path**: `GET /api/admin/system/metrics`
* **Description**: Fetches CPU, memory, thread pool, and system load metrics.
* **Authentication**: `ROLE_ADMIN` / `ROLE_SUPER_ADMIN`

#### Sample Success Response (`200 OK`)
```json
{
  "success": true,
  "message": "System metrics details retrieved successfully",
  "data": {
    "systemCpuLoad": 0.12,
    "processCpuLoad": 0.05,
    "freeMemoryMb": 1024,
    "totalMemoryMb": 4096
  },
  "timestamp": 1722000000000
}
```

---

## 16. Platform Reliability & Disaster Recovery (`/api/admin/reliability`)

### 16.1 Get Reliability Dashboard
* **Method & Path**: `GET /api/admin/reliability/dashboard`
* **Description**: Circuit breaker states, maintenance mode, and backup status.
* **Authentication**: `ROLE_ADMIN` / `ROLE_SUPER_ADMIN`

#### Sample Success Response (`200 OK`)
```json
{
  "maintenanceMode": false,
  "featureFlagsEnabled": true,
  "systemAvailability": 99.98,
  "databaseStatus": "UP",
  "redisStatus": "UP",
  "storageStatus": "UP"
}
```

---

## 17. API Governance & Security Keys (`/api/admin`)

### 17.1 Create API Key
* **Method & Path**: `POST /api/admin/api-keys`
* **Description**: Generates an API key for machine-to-machine integrations.
* **Authentication**: `ROLE_ADMIN` / `ROLE_SUPER_ADMIN`

#### Sample Request Body
```json
{
  "name": "Inventory Monitoring Service Key",
  "requestsPerHour": 1000,
  "permissions": ["READ_PRODUCTS", "READ_INVENTORY"]
}
```

#### Sample Success Response (`201 Created`)
```json
{
  "success": true,
  "message": "API Key created successfully",
  "data": {
    "id": 4,
    "name": "Inventory Monitoring Service Key",
    "keyValue": "ak_live_998877665544332211"
  },
  "timestamp": 1722000000000
}
```

---

## 18. Observability & Diagnostics (`/api/admin/observability`)

### 18.1 Get Observability Snapshot
* **Method & Path**: `GET /api/admin/observability/dashboard`
* **Description**: Combined runtime diagnostics, queue stats, and health score.
* **Authentication**: `ROLE_ADMIN` / `ROLE_SUPER_ADMIN`

#### Sample Success Response (`200 OK`)
```json
{
  "metrics": {},
  "runtime": { "blockedThreads": 0 },
  "redis": { "status": "CONNECTED" },
  "queues": { "pendingNotifications": 0 },
  "observabilityScore": 100.0
}
```

---

## 19. Public Health & Readiness Probes (`/api/health`, `/actuator/health`)

### 19.1 Application Liveness Probe
* **Method & Path**: `GET /api/health/liveness`
* **Description**: Kubernetes / load balancer liveness health check.
* **Authentication**: None (Public)

#### Sample Success Response (`200 OK`)
```json
{
  "status": "UP",
  "message": "Application is running",
  "timestamp": 1722000000000
}
```

---

### 19.2 Application Readiness Probe
* **Method & Path**: `GET /api/health/readiness`
* **Description**: Checks individual module health (Database, Redis, Storage).
* **Authentication**: None (Public)

#### Sample Success Response (`200 OK`)
```json
{
  "database": "UP",
  "redis": "UP",
  "storage": "UP",
  "status": "UP",
  "timestamp": 1722000000000
}
```

---

### 19.3 Spring Boot Actuator Health Probe
* **Method & Path**: `GET /actuator/health`
* **Description**: Standard Spring Boot Actuator health endpoint.
* **Authentication**: None (Public)

#### Sample Success Response (`200 OK`)
```json
{
  "status": "UP"
}
```
