# TEST SCENARIO LIBRARY

This library details reusable functional scenarios for system verification.

## 1. Scenario: Dynamic Cache Eviction
* **Scenario ID**: `QA-SC-001`
* **Description**: Verify that updating a product evicts its cached version from Redis.
* **Preconditions**: Redis is running. The target product is cached.
* **Execution Steps**:
  1. Fetch a product: `GET /api/products/1` (Initial cache write).
  2. Modify the product: `PUT /api/products/1` (Updates product details).
  3. Verify the product details cache key is evicted in Redis.
* **Expected Result**: The product cache key is successfully deleted from Redis.
* **Priority**: **HIGH**

---

## 2. Scenario: Concurrent Stock Checkout Rollback
* **Scenario ID**: `QA-SC-002`
* **Description**: Verify that concurrent checkouts are handled correctly by optimistic locking.
* **Preconditions**: Product ID 1 stock level is set to 2.
* **Execution Steps**:
  1. Trigger two checkout requests for product ID 1 simultaneously.
* **Expected Result**: One request completes successfully, while the other fails with a version conflict error and rolls back.
* **Priority**: **CRITICAL**
