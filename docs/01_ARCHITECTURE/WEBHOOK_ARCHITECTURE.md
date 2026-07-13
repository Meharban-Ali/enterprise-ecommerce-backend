# WEBHOOK LIFECYCLE & INTEGRATIONS

This document details the webhook subsystem, signature verification, and delivery workflow.

## 1. Webhook Processing Flow

```mermaid
sequenceDiagram
    participant Stripe as Payment Gateway
    participant WebhookController
    participant WebhookService
    participant Database as Database (idempotency checks)
    Stripe->>WebhookController: POST /api/webhooks/stripe (with HMAC Header)
    WebhookController->>WebhookController: Verify Signature
    WebhookController->>WebhookService: Process Event
    WebhookService->>Database: Query Idempotency Key
    Database-->>WebhookService: Key Not Used
    WebhookService->>Database: Save delivery record, Update Order PAID
    WebhookService-->>WebhookController: Event Processed
    WebhookController-->>Stripe: 200 OK
```

## 2. Security & Idempotency
* **Signature Verification**: Validates HMAC signature headers using secrets to confirm request authenticity.
* **Idempotency**: Webhook events are logged in the database using their unique IDs to prevent duplicate processing.
