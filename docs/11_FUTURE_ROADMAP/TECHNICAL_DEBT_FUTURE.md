# TECHNICAL DEBT REGISTER

This register tracks outstanding technical debt and engineering tasks.

| Debt Item | Impact | Priority | Complexity | Recommended Sprint |
| :--- | :--- | :---: | :--- | :---: |
| **Integrate Flyway database migrations** | High risk of schema drift in staging/production. | **CRITICAL** | Medium | Sprint 15 |
| **Integrate ShedLock distributed scheduler locking** | Duplicate background tasks run on multi-replica deployments. | **CRITICAL** | Medium | Sprint 15 |
| **Improve API Versioning** | High risk of breaking client integrations on API contract updates. | **HIGH** | Medium | Sprint 16 |
| **Add OpenTelemetry distributed tracing** | Difficult to trace requests across async threads. | **MEDIUM** | Medium | Sprint 17 |
| **Docker Container Security Hardening** | Running container as root poses security risks. | **HIGH** | Low | Sprint 15 |
