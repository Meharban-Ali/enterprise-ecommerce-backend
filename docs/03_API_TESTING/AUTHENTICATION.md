# AUTHENTICATION API TESTING

This document details the authentication and credentials verification API requests and responses.

## 1. Register User
* **HTTP Method**: `POST`
* **URL**: `{{baseUrl}}/api/auth/register`
* **Authentication**: None (Public)
* **Headers**: `Content-Type: application/json`
* **Request JSON**:
  ```json
  {
    "username": "tester",
    "email": "tester@example.com",
    "password": "Password123!",
    "securityQuestion": "Birthplace?",
    "securityAnswer": "Denver"
  }
  ```
* **Success Response (201 Created)**:
  ```json
  {
    "success": true,
    "message": "User registered successfully",
    "data": {
      "id": 1,
      "username": "tester",
      "email": "tester@example.com",
      "role": "ROLE_USER"
    }
  }
  ```

---

## 2. Login User
* **HTTP Method**: `POST`
* **URL**: `{{baseUrl}}/api/auth/login`
* **Authentication**: None (Public)
* **Request JSON**:
  ```json
  {
    "username": "tester@example.com",
    "password": "Password123!"
  }
  ```
* **Success Response (200 OK)**:
  ```json
  {
    "success": true,
    "message": "Login successful",
    "data": {
      "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "refreshToken": "75cf9989-0d49-4fe0-86f8-120908b5df9c"
    }
  }
  ```
