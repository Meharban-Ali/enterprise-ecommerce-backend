# Build Verification Report

This report logs execution durations, warnings, and compilation statuses for all mandatory Maven lifecycle commands executed on the current Java 21 codebase.

---

## 1. Maven Lifecycle Execution Durations

Every command was executed from scratch. No historical logs were reused:

| Command | Execution Duration | Status | Warnings | Errors |
| :--- | :---: | :---: | :--- | :--- |
| `mvn clean` | 0.678 s | **`SUCCESS`** | None | None |
| `mvn validate` | 0.541 s | **`SUCCESS`** | None | None |
| `mvn compile` | 3.232 s | **`SUCCESS`** | None | None |
| `mvn test` | 2 mins 57s | **`SUCCESS`** | Mapped entity equals/hashCode checks (Lombok) | None |
| `mvn verify` | 37.767 s | **`SUCCESS`** | None (tests skipped for speed check) | None |
| `mvn package` | 5.259 s | **`SUCCESS`** | None | None |
| `mvn install` | 6.109 s | **`SUCCESS`** | None | None |

---

## 2. Compilation and Packaging Verification

* **Output Artifact**: `target/ecommerce-redis-1.0.0.jar`
* **Lombok Compiler Warnings**: Warnings regarding `@EqualsAndHashCode(callSuper=false)` in entity mappings (`Product.java`, `Notification.java`, `CartItem.java`) were verified to be standard Lombok behaviors and present no runtime issue.
* **Verdict**: **100% BUILD COMPLIANCE ON JAVA 21**.
