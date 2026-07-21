# Configuration Verification Report

This report confirms the profile configurations and parameter mappings defined to isolate environments.

---

## 1. Profiles Layout

The project separates configuration scopes into dedicated property layers:

* **[`application.properties`](file:///D:/Meharban_code/ecommerce/src/main/resources/application.properties)**: Global baseline options (Jackson timestamps, upload limits, and Java 21 Virtual Threads property).
* **[`application-dev.properties`](file:///D:/Meharban_code/ecommerce/src/main/resources/application-dev.properties)**: Local dev parameters (localhost ports, database updates, trace logs, and Swagger access enabled).
* **[`application-prod.properties`](file:///D:/Meharban_code/ecommerce/src/main/resources/application-prod.properties)**: Production hardened properties (ddl-auto set to validate, database pools, JWT environment mapping, Swagger disabled, and cookie security flags active).

---

## 2. Parameter Bindings

All credential properties map to environment variables for runtime binding:

* `SPRING_PROFILES_ACTIVE` -> `prod` / `dev`
* `DB_URL` -> URL connection string
* `REDIS_HOST` -> Cache hostname
* `SMTP_PASSWORD` -> Mail app token
* `JWT_SECRET` -> JWT signature key
