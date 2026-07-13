# SYSTEM SCALING GUIDE

This guide explains how to scale the eCommerce platform.

## 1. Current Scalability Limitations
* **Single-Node Schedulers**: The background outbox scheduler lacks a distributed lock (ShedLock). Scaling the application horizontally to multiple replicas will cause duplicate scheduler runs and duplicate email dispatches.
* **Database Scaling**: All operations read and write to a single MySQL instance. Eager loading and N+1 query limits are optimized, but the database will eventually hit CPU limits under high concurrent write loads.

---

## 2. Horizontal Scaling Recommendations (Future)
* **Integrate ShedLock**: Annotate scheduling tasks with ShedLock to ensure only one instance executes the background outbox task at a time.
* **Database Read Replicas**: Implement a read/write data routing split. Write operations route to the MySQL primary node, while read queries route to read replicas.
* **Message Broker**: Replace database outbox polling with a message broker (e.g., Kafka or RabbitMQ) to decouple notification dispatches.
