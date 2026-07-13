# SYSTEM ARCHITECTURE GUIDE

This document details the layer boundaries, execution pipelines, and runtime request lifecycles of the application.

## 1. High-Level Subsystems Flow

```mermaid
graph TD
    Client[Client Apps] -->|HTTPS Request| WebServer[Tomcat Web Server]
    WebServer -->|Filters| Security[Spring Security Filter Chain]
    Security -->|Routing| RESTController[REST Controllers]
    RESTController -->|Data Bind & Validate| BusinessService[Domain Service Layer]
    BusinessService -->|Lookup / Evict| RedisCache[(Redis Cache Store)]
    BusinessService -->|Query / Persist| JPA[Spring Data JPA Layer]
    JPA -->|Commit Transaction| MySQL[(MySQL Database)]
```

## 2. Layer Responsibilities

### Controller Layer
Handles HTTP requests, deserializes JSON request bodies into DTO objects, validates request parameters, and handles exceptions via global advisors.

### Service Layer
Contains core business logic, orchestrates data validation, manages database transactions, and manages the caching lifecycle.

### Repository Layer
Extends Spring Data JPA interfaces to execute database queries. No business logic is permitted at this level.

---

## 3. Request Lifecycle

```mermaid
sequenceDiagram
    participant User
    participant Filters as Filter Chain
    participant Controller
    participant Service
    participant Database
    User->>Filters: HTTP Request + Bearer Token
    Filters->>Filters: Rate Limiter & JWT Check
    Filters->>Controller: Route to Handler
    Controller->>Controller: Validate DTO Bounds
    Controller->>Service: Execute Transaction
    Service->>Database: Query/Persist Action
    Database-->>Service: Return Entity Data
    Service-->>Controller: Return DTO Payload
    Controller-->>User: HTTP 200 OK / 201 Created
```
