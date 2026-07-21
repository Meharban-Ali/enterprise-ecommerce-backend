# Blocking Component Report

This report evaluates CommandLineRunners and PostConstruct methods that execute during the bootstrap process.

---

## 1. Startup Component Inventory

| Component Name | Type | Thread | Execution Scope | Blocking Risk |
| :--- | :--- | :--- | :--- | :---: |
| `DataInitializer` | `CommandLineRunner` | `main` | Seeds default products, rules, and admin credentials. | **NONE** (Returns immediately if already seeded or variables are missing) |
| `PlatformReadinessChecker` | `CommandLineRunner` | `main` | Checks db/redis/storage health and halts if down. | **NONE** (Performs quick local SQL checks with no blocking delays) |
| `RevokedApiKeyBloomFilter` | `CommandLineRunner` | `main` | Queries active API keys and populates the Bloom filter. | **NONE** (Loads state sequentially without blocking locks) |
| `NotificationQueueWorker` | `@PostConstruct` | `worker-pool` | Submits workers to dequeue and process notification events. | **NONE** (Submits to an asynchronous thread pool executor) |
