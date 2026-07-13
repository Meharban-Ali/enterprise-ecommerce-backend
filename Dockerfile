# ─── STAGE 1: BUILD ──────────────────────────────────────────────────────────
# Compiles source code and packages the application JAR.
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .

# Download all dependencies before copying source (improves layer caching)
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

# ─── STAGE 2: RUN ────────────────────────────────────────────────────────────
# Uses a minimal JRE-only image to keep the final image size small.
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create a non-root system user/group for security best practices
RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/*.jar app.jar

RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
