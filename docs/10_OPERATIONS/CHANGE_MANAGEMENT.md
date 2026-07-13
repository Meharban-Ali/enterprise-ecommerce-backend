# CHANGE MANAGEMENT GUIDELINES

This document details the procedures for introducing changes to the eCommerce platform.

## 1. Code Modification Gates
All code modifications (e.g., adding endpoints or services) must follow this lifecycle:
1. **Branch Naming**: Branch from `main` using semantic naming (e.g., `feat/coupon-module` or `fix/auth-leak`).
2. **Local Verification**: Compile and run all unit and integration tests locally before proposing changes:
   ```bash
   mvn clean compile test
   ```
3. **Pull Request Gate**: Open a pull request. The CI pipeline will automatically run build checks and test suites. At least one senior developer must approve the changes before merge.

---

## 2. Configuration & Schema Gates
* **Schema Modifications**: Direct manual changes to databases are forbidden. All schema modifications must be scripted using SQL migration files (e.g., Flyway) and tested in staging before deployment.
* **Environment Variables**: Additions to `.env` variables list must be documented in configuration guides.
