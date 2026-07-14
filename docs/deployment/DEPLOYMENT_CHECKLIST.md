# Deployment Checklist

This checklist tracks the deployment tasks required to release version `v1.0.0` to production on Railway.

---

## Pre-Deployment Verification
- [ ] **Code Base Stable**: Verify that all local edits compile successfully.
- [ ] **Tests Pass**: Run `./mvnw test` to ensure all 360 unit and integration tests pass cleanly.
- [ ] **Secrets Loaded**: Verify that all necessary secrets are configured in the target Railway project environment.

---

## Infrastructure Provisioning
- [ ] **MySQL Database Instance**: Confirm that the Railway MySQL database is active.
- [ ] **Redis Cache Instance**: Confirm that the Railway Redis cache is active.
- [ ] **Network Connectivity**: Verify that the application service can connect to database and cache host variables.

---

## Application Launch & Verification
- [ ] **Ingress Domain Assigned**: Generate public routing domains.
- [ ] **Probes Active**: Ensure health checks point to `/actuator/health`.
- [ ] **Bootstrap Logs**: Check startup logs to verify the database migrations and Super Admin bootstrap ran successfully.
- [ ] **Smoke Tests Pass**: Execute all scenarios in the [Post-Deployment Smoke Test Specification](file:///D:/Meharban_Code/ecommerce/docs/deployment/POST_DEPLOYMENT_SMOKE_TEST.md).
- [ ] **Public Traffic Allowed**: Enable access for live customer transactions.
