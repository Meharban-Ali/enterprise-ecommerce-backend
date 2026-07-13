# REGRESSION TEST MATRIX

This matrix prioritizes features for smoke, sanity, and regression testing.

| Feature Area | Smoke Test | Sanity Test | Regression Test | Critical Priority | Automation Candidate |
| :--- | :---: | :---: | :---: | :---: | :---: |
| **JWT Login / Authenticate**| Yes | Yes | Yes | Yes | Yes (Newman/Postman) |
| **Catalog Lookups** | Yes | Yes | Yes | No | Yes (REST Assured) |
| **Category Creation** | No | Yes | Yes | No | Yes (REST Assured) |
| **Cart Modifications**| Yes | Yes | Yes | Yes | Yes (REST Assured) |
| **Order Placement** | Yes | Yes | Yes | Yes | Yes (REST Assured) |
| **Stripe Callback** | Yes | Yes | Yes | Yes | Yes (Mock Gateway) |
| **Outbox Schedulers**| No | No | Yes | No | Yes (Mock SMTP assertions) |
