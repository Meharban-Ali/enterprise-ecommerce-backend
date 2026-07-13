# FREQUENTLY ASKED QUESTIONS

---

### Q1: How do I create a Super Admin user?
* **Answer**: If `app.data-initializer.enabled=true`, default credentials are seeded on startup. Alternatively, register a new account and modify the user role to `ROLE_SUPER_ADMIN` in the database.

---

### Q2: How do I clear the Redis cache manually?
* **Answer**: Connect using `redis-cli` and run:
  ```bash
  redis-cli FLUSHALL
  ```

---

### Q3: How do I enable the production profile?
* **Answer**: Pass the active profile configuration argument on boot:
  ```bash
  mvn spring-boot:run "-Dspring.profiles.active=prod"
  ```
