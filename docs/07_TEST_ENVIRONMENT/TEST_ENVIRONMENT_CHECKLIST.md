# PRE-TESTING ENVIRONMENT CHECKLIST

Verify all items before initiating testing:

- [ ] **Spring Boot Boot**: The application starts successfully with port 8080 bound.
- [ ] **Database Connection**: Confirmed database connection and table checks pass.
- [ ] **Redis Connection**: Verified Redis is running on port 6379.
- [ ] **Seeded Data**: Checked that default users (admin, merchant, customer) are seeded.
- [ ] **Health Check**: Actuator health check `/actuator/health` returns status `UP`.
- [ ] **Clean Logs**: Log files are cleared and ready to trace request correlation IDs.
- [ ] **Postman Environment**: Configured environment variables (baseUrl, jwt_token) in Postman.
