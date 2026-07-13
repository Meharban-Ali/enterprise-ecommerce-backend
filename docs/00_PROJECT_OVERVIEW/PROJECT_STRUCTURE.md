# CODEBASE PACKAGE STRUCTURE

This guide details the package layout and design responsibilities across the codebase.

## Package Tree Map

```
src/main/java/com/redis/
├── auth/                      # Registration, logins, and refresh token exchange
├── catalog/                   # Categories and product catalog records
├── cart/                      # Shopping cart operations
├── order/                     # Order lifecycle management
├── payment/                   # Payment processing and Stripe integrations
├── notification/              # Outbox schedulers and mail dispatches
├── monitoring/                # Actuators and tracer interceptors
├── common/                    # Shared exceptions and utility handlers
└── infrastructure/            # Core configurations and security filters
```

## Domain Context Responsibilities

### 1. com.redis.auth
Coordinates user onboarding, user profiles, and session security. It handles credentials validation, refresh tokens, and password reset flows.

### 2. com.redis.catalog
Manages the product catalog. Category and product services cache read queries to minimize database lookups.

### 3. com.redis.order
Manages orders from cart checkout to completion. It verifies stock levels and updates order status.

### 4. com.redis.notification
Implements the outbox pattern. The outbox scheduler scans for pending events and dispatches notifications asynchronously to avoid blocking user threads.
