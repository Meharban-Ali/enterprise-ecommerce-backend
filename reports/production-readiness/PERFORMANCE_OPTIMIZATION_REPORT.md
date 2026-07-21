# Performance Optimization Report

This report documents the performance optimizations implemented in the project to reduce response latencies and support high-concurrency request volumes.

---

## 1. Implemented Optimizations

### A. Java 21 Virtual Threads
* **Configuration**: Added `spring.threads.virtual.enabled=true` in [`application.properties`](file:///D:/Meharban_code/ecommerce/src/main/resources/application.properties#L15).
* **Rationale**: Leverages Java 21 Virtual Threads (Project Loom) natively. Tomcat request dispatch executors execute lightweight threads instead of heavy OS threads, preventing thread-pool exhaustion during traffic spikes.
* **Expected Improvement**: Reduces memory overhead per thread and handles millions of concurrent virtual requests with minimal thread contention.

### B. Hibernate JDBC SQL Batching
* **Configuration**: Mapped the following in [`application-prod.properties`](file:///D:/Meharban_code/ecommerce/src/main/resources/application-prod.properties#L74-L77):
  ```properties
  spring.jpa.properties.hibernate.jdbc.batch_size=30
  spring.jpa.properties.hibernate.order_inserts=true
  spring.jpa.properties.hibernate.order_updates=true
  spring.jpa.properties.hibernate.jdbc.batch_versioned_data=true
  ```
* **Rationale**: Batches update/insert queries into single roundtrips.
* **Expected Improvement**: Minimizes roundtrips between the Spring Boot application and MySQL, increasing transaction throughput during multi-item checkouts.

### C. Connection Pool Hardening
* **Configuration**: Mapped Hikari pool size bounds and added:
  ```properties
  spring.datasource.hikari.leak-detection-threshold=2000
  ```
* **Rationale**: Automatically warns if a database connection is held longer than 2 seconds, helping identify leaks.
