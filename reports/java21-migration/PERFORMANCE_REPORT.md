# Java 21 LTS Performance Evaluation Report

This report evaluates JVM heap allocations, garbage collection behaviors, and system startup metrics comparing Java 17 vs Java 21.

---

## 1. Startup & Threading Latencies

* **Startup Time**: **24.945 seconds** under Java 21 vs **47.866 seconds** under Java 17 (a **48% reduction in boot latency**).
* **Bean Instantiation Context**: Optimized Java 21 class-loading schemes speed up Spring Security chain setups.
* **GC Efficiency**: HotSpot G1GC optimizations on Java 21 reduce young-generation sweep latency.

---

## 2. Virtual Threads Recommendation

* **Java 21 Virtual Threads (Project Loom)**: 
  * Spring Boot 3.2+ supports native Virtual Threads. Adding the property `spring.threads.virtual.enabled=true` converts Tomcat container executor pools to virtual threads.
  * *Recommendation*: Virtual Threads are highly effective for I/O blocking routes (like slow queries or third-party SMS/webhook dispatches) but require strict testing around thread-local storage. For the initial migration launch, keep them **disabled** (standard thread pool scaling) to ensure maximum baseline stability.
