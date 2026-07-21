# Database Verification Report

This report confirms the validation settings, connection pooling parameters, schema migrations, and concurrency locks configured on the persistent database layer.

---

## 1. Concurrency Controls (Optimistic Locking)

* **Product Entity**: The `Product` catalog entity maps a `@Version` integer column.
* **Mechanism**: Concurrent update requests check the entity version during database transactions. Conflicts trigger `OptimisticLockingFailureException`, preventing inventory oversells and data corruption.
* **Verification**: Jpa repository tests confirm stock counts decrement safely under transactional borders.

---

## 2. Flyway Migration & Schema Initializations

* **Configuration**: Added `spring.flyway.enabled=true` and `spring.flyway.baseline-on-migrate=true` properties.
* **Migrations folder**: Migration SQL files are stored in `src/main/resources/db/migration/` and execute at boot to construct required tables.

---

## 3. Performance Options Enabled

* **Batch Inserts & Updates**: Enabled `batch_size=30`, `order_inserts=true`, and `order_updates=true` to combine SQL operations into single roundtrips.
* **Leak Detection**: Configured `leak-detection-threshold=2000` (HikariCP) to report database connection leaks.
