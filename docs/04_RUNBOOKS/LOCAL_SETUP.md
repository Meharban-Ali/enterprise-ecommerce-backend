# DEVELOPER LOCAL SETUP GUIDE

Follow these steps to set up and run the eCommerce backend on your local machine.

## 1. System Requirements
* **Java**: OpenJDK 17 or higher
* **Build Tool**: Apache Maven 3.8+
* **Database**: MySQL 8.0 (Optional: local H2 mode can be used instead)
* **Caching**: Redis 7.x

---

## 2. Environment Variables Configuration
Copy the environment variables template and configure the values:
```bash
cp .env.example .env
```
Ensure target configurations are active in `.env`:
```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/redisdb
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_db_password
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379
JWT_SECRET=1Fgi/zPX0upMYb170+pf2TVbYGKNVoQUr3MJKqQIzgg=
```

---

## 3. Running the Application
To compile and run the backend using the local in-memory H2 profile:
```bash
mvn spring-boot:run "-Dspring.profiles.active=local-h2"
```

To verify startup, query the health endpoint:
```bash
curl http://localhost:8080/actuator/health
```
Expect response: `{"status":"UP"}`.
