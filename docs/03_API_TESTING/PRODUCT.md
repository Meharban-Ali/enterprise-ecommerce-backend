# PRODUCT & CATALOG API TESTING

This document details category creation, product additions, and catalog search API requests and responses.

## 1. Create Product (Requires Admin role)
* **HTTP Method**: `POST`
* **URL**: `{{baseUrl}}/api/products`
* **Authentication**: Yes (`ROLE_ADMIN`)
* **Headers**: `Authorization: Bearer {{jwt_token}}`, `Content-Type: application/json`
* **Request JSON**:
  ```json
  {
    "name": "Elite Headset",
    "description": "Active Noise Cancelling",
    "price": 199.99,
    "stockQuantity": 100,
    "categoryId": 1
  }
  ```
* **Success Response (201 Created)**:
  ```json
  {
    "success": true,
    "message": "Product created successfully",
    "data": {
      "id": 1,
      "name": "Elite Headset",
      "price": 199.99,
      "stockQuantity": 100
    }
  }
  ```

---

## 2. Get Products (Public)
* **HTTP Method**: `GET`
* **URL**: `{{baseUrl}}/api/products?page=0&size=10`
* **Authentication**: None (Public)
* **Success Response (200 OK)**: Returns a paginated list of products.
