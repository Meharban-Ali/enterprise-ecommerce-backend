# PERFORMANCE ROADMAP

This roadmap details the planned performance improvements.

## 1. Database Connection Tuning
* **HikariCP Tuning**: Optimize pool properties (`maximum-pool-size=20`, `minimum-idle=5`) in production to balance database resource usage and response times.

---

## 2. Asynchronous Thread Execution
* **Spring Boot `@Async`**: Enable asynchronous processing for non-blocking tasks (e.g., logging activity, cleaning expired carts).

---

## 3. Product Catalog Search Indexing
* **Lucene / Elasticsearch Indexing**: Offload fuzzy string queries and category filter operations from MySQL to Elasticsearch to achieve sub-second catalog search responses.
