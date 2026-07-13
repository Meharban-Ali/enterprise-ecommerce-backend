# CARTS & ORDERS CHECKOUT API TESTING

This document details cart additions, checkouts, and Stripe webhook payment API requests and responses.

## 1. Add Item to Cart
* **HTTP Method**: `POST`
* **URL**: `{{baseUrl}}/api/cart/items`
* **Authentication**: Yes (`ROLE_USER`)
* **Headers**: `Authorization: Bearer {{jwt_token}}`, `Content-Type: application/json`
* **Request JSON**:
  ```json
  {
    "productId": 1,
    "quantity": 2
  }
  ```
* **Success Response (200 OK)**: Returns the updated cart DTO.

---

## 2. Place Order (Checkout)
* **HTTP Method**: `POST`
* **URL**: `{{baseUrl}}/api/orders`
* **Headers**: `Authorization: Bearer {{jwt_token}}`, `Idempotency-Key: check-12098`
* **Success Response (201 Created)**: Returns the pending order DTO.

---

## 3. Stripe Payment Webhook
* **HTTP Method**: `POST`
* **URL**: `{{baseUrl}}/api/webhooks/stripe`
* **Authentication**: None (Public - HMAC header verified)
* **Request JSON**:
  ```json
  {
    "id": "evt_charge_102",
    "type": "charge.succeeded",
    "data": {
      "object": {
        "id": "ch_102",
        "amount": 39998,
        "metadata": {
          "order_id": "1"
        }
      }
    }
  }
  ```
* **Success Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Webhook processed successfully",
    "data": null
  }
  ```
