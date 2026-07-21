# Runtime Root Cause Analysis

This report documents the diagnostic evidence compiled to investigate startup durations and logging volumes.

---

## 1. Primary Diagnostic Findings

### A. Verbose Hibernate Debug Logging (Console Flooding)
* **Evidence**: Live startup logs recorded over 19,000 lines of Hibernate mapping, hydration, and transaction debug statements *before* Tomcat finished starting.
* **Root Cause**: The development profile parameter `logging.level.org.hibernate=DEBUG` forced Hibernate to output name-query search exceptions (e.g. stack traces for 29 repositories * 3 checks each) directly to stdout. Under Windows console environments, rendering 20,000 lines of log messages to stdout throttles execution throughput, causing the boot process to look hung and trigger script timeouts.
* **Resolution**: Standardized the dev Hibernate logging level to `WARN` in [`application-dev.properties`](file:///D:/Meharban_code/ecommerce/src/main/resources/application-dev.properties#L13).

### B. Transaction Rollback Contamination (Optimistic Locks)
* **Evidence**: Schedulers emitted periodic `ObjectOptimisticLockingFailureException` warnings on `Incident#12` updates.
* **Root Cause**: The `@Transactional` annotation was declared at the root method `evaluateRules()`. When a single rule evaluation threw an optimistic locking error, the entire Spring transaction context was marked as `rollback-only`, causing all subsequent rule checks to fail.
* **Resolution**: Removed `@Transactional` from `evaluateRules()` and wrapped the inner evaluation steps in a programmatic `TransactionTemplate` to isolate transaction states per rule.
