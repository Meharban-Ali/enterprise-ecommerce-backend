# SYSTEM SCALABILITY ROADMAP

This roadmap details the planned improvements to support high concurrent user loads.

## 1. Database Scaling: Read/Write Splitting
Implement a read/write data routing split:
* **Primary Node**: Handles all write transactions (e.g., place order, update stock).
* **Read Replicas**: Handle all read-only catalog queries (e.g., list products, search categories).

---

## 2. Distributed Caching
* **Redis Cluster**: Migrate from a single Redis node to a clustered Redis setup to support high-throughput catalog read caching.

---

## 3. Distributed Scheduling (ShedLock)
* **ShedLock Lockings**: Integrate ShedLock to coordinate background outbox schedulers and prevent duplicate runs on clustered application instances.
