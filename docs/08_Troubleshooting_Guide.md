# 08. Troubleshooting & Runbooks

## 1. Out-of-Memory / Max Body Size rejections
* **Symptom**: Large payload uploads cause rejections.
* **Solution**: Check Tomcat parameters (`server.tomcat.max-http-form-post-size=2MB` in properties overrides).

## 2. Cache Inconsistencies
* **Symptom**: Product updates do not reflect in read listings.
* **Solution**: Clear Redis manually using `redis-cli FLUSHALL` or call `DELETE /api/products/cache`.
