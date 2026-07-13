# POST-RELEASE VERIFICATION GUIDE

Follow these verification steps immediately after deploying to production.

## 1. Sanity Check Sequence
Execute the following calls to verify the deployed service is active and responding:
1. **Health Check**: `GET /actuator/health`. Confirm the response is `200 OK` with status `UP`.
2. **User Login**: `POST /api/auth/login`. Confirm a valid JWT token is returned.
3. **Product Lookup**: `GET /api/products/1`. Confirm product details are returned.

---

## 2. Logs Audit Check
Monitor the application logs for any errors:
* **Trace Verification**: Ensure logs format is in structured JSON and contains MDC trace IDs.
* **Exceptions Checks**: Verify no database connection timeouts or JVM out-of-memory errors are thrown.
