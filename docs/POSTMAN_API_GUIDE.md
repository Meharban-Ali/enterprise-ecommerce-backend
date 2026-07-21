# Postman API Guide

This guide provides full API reference documentation for developers and QA engineers to interact with the backend service.

---

## 1. Authentication & Tokens Usage

* **JWT Access Token**: Clients must include this token in the header of all protected requests:
  ```text
  Authorization: Bearer <your_access_token>
  ```
* **Refresh Token Flow**: When the access token expires (`401 Unauthorized`), make a request to `/api/auth/refresh` containing the refresh token payload to receive a new access token.
* **API Testing Order**:
  1. Boot the application and call `/api/auth/register` to register a client account.
  2. Call `/api/auth/login` to retrieve the Access Token and Refresh Token.
  3. Query public endpoints (Products/Categories).
  4. Query user endpoints (Cart/Wishlist/Orders) injecting the `Authorization` header.

---

## 2. API Endpoints Catalog

### A. Authentication APIs

#### 1. Register User
* **Endpoint**: `/api/auth/register`
* **Method**: `POST`
* **Headers**: `Content-Type: application/json`
* **Auth Required**: No
* **Request Body**:
  ```json
  {
    "username": "client_user",
    "email": "user@example.com",
    "password": "Password123!"
  }
  ```
* **Sample Response (201 Created)**:
  ```json
  {
    "message": "User registered successfully",
    "userId": "d748f2b1-e23a-4bc1-be19-dda472ab134a"
  }
  ```
* **Error Response (400 Bad Request)**:
  ```json
  {
    "error": "Email is already taken"
  }
  ```

#### 2. Login User
* **Endpoint**: `/api/auth/login`
* **Method**: `POST`
* **Headers**: `Content-Type: application/json`
* **Auth Required**: No
* **Request Body**:
  ```json
  {
    "email": "user@example.com",
    "password": "Password123!"
  }
  ```
* **Sample Response (200 OK)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
    "refreshToken": "74826b1d-f2ab-471a-bc01-da7482a13b5d",
    "tokenType": "Bearer",
    "username": "client_user"
  }
  ```

#### 3. Refresh Access Token
* **Endpoint**: `/api/auth/refresh`
* **Method**: `POST`
* **Headers**: `Content-Type: application/json`
* **Auth Required**: No
* **Request Body**:
  ```json
  {
    "refreshToken": "74826b1d-f2ab-471a-bc01-da7482a13b5d"
  }
  ```
* **Sample Response (200 OK)**:
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzUxMiJ9.new...",
    "refreshToken": "74826b1d-f2ab-471a-bc01-da7482a13b5d"
  }
  ```

---

### B. Users APIs

#### Get Profile
* **Endpoint**: `/api/users/profile`
* **Method**: `GET`
* **Headers**: `Authorization: Bearer <token>`
* **Auth Required**: Yes (`ROLE_USER` or higher)
* **Sample Response (200 OK)**:
  ```json
  {
    "username": "client_user",
    "email": "user@example.com",
    "createdAt": "2026-07-20T00:15:00"
  }
  ```

---

### C. Products APIs

#### 1. List Products (Paginated)
* **Endpoint**: `/api/products?page=0&size=10`
* **Method**: `GET`
* **Auth Required**: No
* **Sample Response (200 OK)**:
  ```json
  {
    "content": [
      {
        "id": 1,
        "name": "Wireless Headphones",
        "price": 99.99,
        "stockQuantity": 15
      }
    ],
    "totalPages": 1,
    "totalElements": 1
  }
  ```

#### 2. Get Product Details
* **Endpoint**: `/api/products/{id}`
* **Method**: `GET`
* **Auth Required**: No
* **Sample Response (200 OK)**:
  ```json
  {
    "id": 1,
    "name": "Wireless Headphones",
    "price": 99.99,
    "description": "High-fidelity Bluetooth headphones",
    "stockQuantity": 15
  }
  ```

---

### D. Categories APIs

#### List Categories
* **Endpoint**: `/api/categories`
* **Method**: `GET`
* **Auth Required**: No
* **Sample Response (200 OK)**:
  ```json
  [
    {
      "id": 1,
      "name": "Electronics"
    }
  ]
  ```

---

### E. Cart APIs

#### 1. View Cart
* **Endpoint**: `/api/cart`
* **Method**: `GET`
* **Headers**: `Authorization: Bearer <token>`
* **Auth Required**: Yes
* **Sample Response (200 OK)**:
  ```json
  {
    "items": [
      {
        "productId": 1,
        "productName": "Wireless Headphones",
        "quantity": 2,
        "price": 99.99
      }
    ],
    "totalPrice": 199.98
  }
  ```

---

### F. Wishlist APIs

#### View Wishlist
* **Endpoint**: `/api/wishlist`
* **Method**: `GET`
* **Headers**: `Authorization: Bearer <token>`
* **Auth Required**: Yes
* **Sample Response (200 OK)**:
  ```json
  [
    {
      "productId": 1,
      "productName": "Wireless Headphones"
    }
  ]
  ```

---

### G. Orders APIs

#### Create Order
* **Endpoint**: `/api/orders`
* **Method**: `POST`
* **Headers**: 
  * `Authorization: Bearer <token>`
  * `Idempotency-Key: <unique_uuid>`
* **Auth Required**: Yes
* **Request Body**:
  ```json
  {
    "shippingAddress": "123 Main St, New York, NY",
    "items": [
      {
        "productId": 1,
        "quantity": 2
      }
    ]
  }
  ```
* **Sample Response (201 Created)**:
  ```json
  {
    "orderId": "4865-f2ab-47a1-bc39",
    "status": "PENDING_PAYMENT",
    "totalAmount": 199.98
  }
  ```

---

### H. Payments APIs

#### Process Payment
* **Endpoint**: `/api/payments/process`
* **Method**: `POST`
* **Headers**: 
  * `Authorization: Bearer <token>`
  * `Idempotency-Key: <unique_uuid>`
* **Auth Required**: Yes
* **Request Body**:
  ```json
  {
    "orderId": "4865-f2ab-47a1-bc39",
    "paymentMethod": "STRIPE",
    "paymentToken": "tok_visa"
  }
  ```
* **Sample Response (200 OK)**:
  ```json
  {
    "transactionId": "ch_3M47b19dfa",
    "status": "SUCCESSFUL",
    "amountPaid": 199.98
  }
  ```

---

### I. Notifications APIs

#### View In-App Notifications
* **Endpoint**: `/api/notifications`
* **Method**: `GET`
* **Headers**: `Authorization: Bearer <token>`
* **Auth Required**: Yes
* **Sample Response (200 OK)**:
  ```json
  [
    {
      "id": 1,
      "message": "Your order 4865 has been shipped",
      "read": false,
      "createdAt": "2026-07-20T00:15:00"
    }
  ]
  ```

---

### J. Admin APIs

#### 1. Add Product
* **Endpoint**: `/api/admin/products`
* **Method**: `POST`
* **Headers**: `Authorization: Bearer <admin_token>`
* **Auth Required**: Yes (`ROLE_ADMIN` / `ROLE_SUPER_ADMIN`)
* **Request Body**:
  ```json
  {
    "name": "Mechanical Keyboard",
    "price": 129.99,
    "description": "RGB backlight mechanical keyboard",
    "stockQuantity": 50,
    "categoryId": 1
  }
  ```
* **Sample Response (201 Created)**:
  ```json
  {
    "id": 2,
    "name": "Mechanical Keyboard",
    "status": "CREATED"
  }
  ```

---

### K. Webhook APIs

#### Register Webhook Endpoint
* **Endpoint**: `/api/admin/webhooks`
* **Method**: `POST`
* **Headers**: `Authorization: Bearer <admin_token>`
* **Auth Required**: Yes (`ROLE_ADMIN`)
* **Request Body**:
  ```json
  {
    "name": "Inventory Event Listener",
    "targetUrl": "https://client-erp.com/webhook",
    "filterSeverity": "HIGH",
    "enabled": true
  }
  ```
* **Sample Response (201 Created)**:
  ```json
  {
    "id": 1,
    "secretKey": "whsec_7482fba97da12b9a7c39..."
  }
  ```

---

### L. Health & Actuator APIs

#### Actuator Health Check
* **Endpoint**: `/actuator/health`
* **Method**: `GET`
* **Auth Required**: No
* **Sample Response (200 OK)**:
  ```json
  {
    "status": "UP",
    "components": {
      "db": {
        "status": "UP",
        "details": {
          "database": "MySQL"
        }
      },
      "redis": {
        "status": "UP"
      }
    }
  }
  ```
