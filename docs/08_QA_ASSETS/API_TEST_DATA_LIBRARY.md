# API REQUEST DATA LIBRARY

This library provides reusable JSON request payloads for core REST endpoints.

## 1. User Registration (`POST /api/auth/register`)
* **Valid Request**:
  ```json
  {
    "username": "qa_tester",
    "email": "qa_tester@example.com",
    "password": "Password123!",
    "securityQuestion": "First pet?",
    "securityAnswer": "Buddy"
  }
  ```
* **Invalid Request (Short Password)**:
  ```json
  {
    "username": "qa_tester",
    "email": "qa_tester@example.com",
    "password": "123",
    "securityQuestion": "First pet?",
    "securityAnswer": "Buddy"
  }
  ```

---

## 2. Product Creation (`POST /api/products`)
* **Valid Request**:
  ```json
  {
    "name": "QA Test Product",
    "description": "Standard test item DTO",
    "price": 49.99,
    "stockQuantity": 100,
    "categoryId": 1
  }
  ```
* **Invalid Request (Negative Price)**:
  ```json
  {
    "name": "QA Test Product",
    "description": "Standard test item DTO",
    "price": -10.00,
    "stockQuantity": 100,
    "categoryId": 1
  }
  ```
