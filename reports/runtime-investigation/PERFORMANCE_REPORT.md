# Performance Report

This report analyzes the performance enhancements obtained through logging level corrections and isolated transaction boundaries.

---

## 1. Startup Duration Analysis

* **Before Optimization**: 
  * The context refresh cycle printed >19,000 lines of verbose Hibernate `DEBUG` statements.
  * In Windows terminal hosts, rendering this volume of output bottlenecked execution, taking several minutes to reach Tomcat binding status.
* **After Optimization**:
  * Hibernate output is capped to `WARN` level.
  * Startup logs are reduced by **98%** (only ~150 lines are printed).
  * Tomcat started successfully in **42.6 seconds**.

---

## 2. Hikari Database Pool Health

* **Idle / Minimum Connections**: Connection lifecycle checks show connection generation takes < 900 ms.
* **Database Readiness**: Flyway validation executes schema checks in under 70 ms.
