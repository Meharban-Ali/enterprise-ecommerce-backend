# REDIS CACHE BASELINE

This document details the expected Redis cache state before and after testing.

## 1. Expected Cache Keys (Fresh Startup)
Upon startup, the Redis cache should contain no catalog entries.
```bash
redis-cli KEYS "*"
```
Expect response: `(empty array)`.

---

## 2. Key Lifecycle Mappings
Once API calls execute, keys are generated under standard namespaces:
* **Product Details Key**: `products::<product_id>` (e.g., `products::1`).
* **Category Listing Key**: `categories::<category_id>`.
* **Token Blacklist Key**: `blacklist::<token_signature>`.

---

## 3. Cache Eviction Checks
Verify that modifications to products evict related caches:
1. Cache a product by calling `GET /api/products/1`.
2. Update the product: `PUT /api/products/1`.
3. Check Redis keys. The key `products::1` should be evicted.
