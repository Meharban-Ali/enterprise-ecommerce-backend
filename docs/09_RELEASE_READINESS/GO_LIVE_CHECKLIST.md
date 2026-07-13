# GO-LIVE CHECKLIST

Verify all items before final sign-off:

- [ ] **Environment Ready**: Database and caching infrastructure are active.
- [ ] **Secrets Configured**: Production API keys and JWT secrets are set in the environment variables.
- [ ] **Application Started**: Boot completes successfully with port 8080 bound.
- [ ] **Sanity Passed**: Health check and basic user login test cases pass.
- [ ] **Monitoring Active**: Actuator metrics endpoint is accessible by monitoring tools.
- [ ] **Logs Verified**: Structured JSON logging is active.
- [ ] **Rollback Plan Available**: Reversion scripts and steps are verified.
- [ ] **Release Approved**: Sign-off from all engineering leads is complete.
