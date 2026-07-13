# 04. Database Documentation

## 1. Entity Relationships Diagram
```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : "has"
    USERS ||--o{ ORDERS : "places"
    ORDERS ||--|{ ORDER_ITEMS : "contains"
    PRODUCTS ||--o{ ORDER_ITEMS : "ordered_in"
```

## 2. Integrity & Lock controls
* **Optimistic Locking**: Write-heavy product stocks use `@Version` annotations.
* **Unique Indexes**: unique index exists on `users.email` and `refresh_tokens.token`.
