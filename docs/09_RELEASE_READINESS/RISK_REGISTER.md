# RISK REGISTER

This register maps operational risks, impacts, probabilities, and mitigation steps.

| Risk ID | Description | Impact | Probability | Severity | Mitigation Strategy | Status |
| :--- | :--- | :--- | :--- | :--- | :--- | :---: |
| **RS-001** | **Missing Flyway database migrations** | Potential data corruption during schema alterations. | High | Critical | Integrate Flyway script versioning in the next sprint. | **OPEN** |
| **RS-002** | **Missing ShedLock distributed scheduler locking** | Duplicate background tasks run on multi-replica deployments. | High | Critical | Integrate ShedLock annotations in the next sprint. | **OPEN** |
| **RS-003** | **Mail service SMTP outage** | Pending order confirmation emails fail to dispatch. | Medium | High | Outbox pattern automatically retries delivery up to 5 times. | **MITIGATED** |
| **RS-004** | **Redis cache down** | Increased catalog read latency on MySQL database. | Low | Medium | Custom fallback cache error handler falls back to SQL queries. | **MITIGATED** |
| **RS-005** | **Expired JWT HMAC Secret Key** | Unauthorized users gain access using compromised tokens. | Low | Critical | Implement base64 secret key rotations. | **MITIGATED** |
