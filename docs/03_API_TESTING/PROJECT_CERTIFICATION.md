# QA RELEASES CERTIFICATION CHECKLIST

Verify all items before certifying a build for staging or production:

| Target Subsystem | Pass Criteria | Verification Method | Status |
| :--- | :--- | :--- | :---: |
| **Authentication** | Passwords must be BCrypt encoded; login issues HS256 JWT tokens. | Execute `POST /api/auth/login` | **PASSED** |
| **RBAC Security** | Calls to `/api/admin/**` from users return 403. | Execute unauthorized calls | **PASSED** |
| **State transitions**| Orders transition to `PAID` upon Stripe webhook callbacks. | Execute webhook callbacks | **PASSED** |
| **Outbox Notifications**| Schedulers scan the outbox and dispatch emails. | Query outbox logs | **PASSED** |
| **Cache Bypasses** | Catalog queries fall back to SQL if Redis goes down. | Verify logs with Redis stopped | **PASSED** |
| **Observed metrics** | Requests record correlation IDs and trace variables. | Inspect console logger JSONs | **PASSED** |
