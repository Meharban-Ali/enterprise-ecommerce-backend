# 06. Testing Guide

## 1. Quality Gate Controls
* **Tests executions**: `mvn clean test "-Dspring.profiles.active=local-h2"`.
* All 355 unit and mockito integration test suites must pass on pull requests.

## 2. QA Smoke Checklist
- [ ] Liveness probe GET `/api/health/liveness` returns 200 OK.
- [ ] Readiness probe GET `/api/health/readiness` returns 200 OK.
- [ ] User login returns active access and refresh tokens.
