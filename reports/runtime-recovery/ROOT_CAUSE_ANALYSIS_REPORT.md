# Root Cause Analysis Report

This report documents the diagnostic findings and root causes resolved during the recovery and stabilization phase.

---

## 1. Diagnostic Findings

| Finding / Issue | Target File | Root Cause | Resolution |
| :--- | :--- | :--- | :--- |
| **Silent Database Fallback Risk** | [`application.properties`](file:///D:/Meharban_code/ecommerce/src/main/resources/application.properties) | Test H2 database configuration parameters were declared in the production root file, causing silent fallback boots in production environments. | Removed H2 properties from the baseline file. They now reside exclusively under test profiles. |
| **Hardcoded Secret Key Vulnerability** | [`JwtProperties.java`](file:///D:/Meharban_code/ecommerce/src/main/java/com/redis/infrastructure/config/JwtProperties.java) | A default hardcoded JWT base64 signing secret string was assigned inside code properties. | Removed default assignment, forcing compile/runtime injection from environment variables. |
| **Obsolete Duplicate Files** | `docs/` and root folders | Duplicate documentation containing suffix copy names (e.g. `README (1).md`) bloated repository indexes. | Purged duplicate files from the filesystem. |
| **Invalid Docker Compose Interpolation** | [`docker-compose.yml`](file:///D:/Meharban_code/ecommerce/docker-compose.yml) | Docker compose environment variables used the non-standard format `${VAR:default}` which causes parser errors in multiple Compose engines. | Standardized syntax to use the standard default format `${VAR:-default}`. |
| **Local SMTP & Redis Downtime** | Local Runtime Port | Health Actuator `/actuator/health` returned `DOWN` / `DEGRADED` status indicators because mail and caching servers are offline locally. | Verified that health component indicators successfully report active connections when services run inside Docker environments. |
