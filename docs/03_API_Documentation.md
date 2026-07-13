# 03. API Documentation

## 1. Authentication
* **Access Token**: JWT token passed in header: `Authorization: Bearer <token>` (expires in 25 minutes).
* **Refresh Token**: Exchanged at `POST /api/auth/refresh` to retrieve new access tokens.
* **API Key**: Admin integrations use headers `X-API-Key`.

## 2. Standard REST Envelope
All API endpoints return wrapped JSON envelopes:
```json
{
  "success": true,
  "message": "Detailed success message",
  "data": { ... }
}
```

## 3. Core Endpoint Mappings Reference
* **Auth**: `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/refresh`, `POST /api/auth/logout`.
* **Products**: `GET /api/products/{id}`, `GET /api/products/search?name=...`.
* **Cart**: `POST /api/cart/items` (Request DTO: `productId`, `quantity`).
* **Orders**: `POST /api/orders` (checkout cart), `GET /api/orders/my`.
