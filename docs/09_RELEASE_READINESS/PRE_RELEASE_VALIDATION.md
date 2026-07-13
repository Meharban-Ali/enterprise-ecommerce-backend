# PRE-RELEASE VERIFICATION GUIDE

Ensure all environment prerequisites are verified before deploying to production.

## 1. Environment Verification
Verify that variables are resolved correctly from the active profile properties:
* **MySQL Check**: Confirm database connectivity and pool configurations in `application-prod.properties`.
* **Redis Check**: Confirm Redis host and port values map correctly.
* **Secrets Check**: Verify that `JWT_SECRET` is defined in the environment variables and is not hardcoded.

---

## 2. Boot verification
Run the application locally to verify the boot cycle:
```bash
mvn spring-boot:run "-Dspring.profiles.active=local-h2"
```
Expect startup logs: `Started EcommerceApplication in ... seconds` with no errors.
