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

# Install curl for healthchecks and create non-root user/group
RUN apk add --no-cache curl && \
    addgroup -S spring && \
    adduser -S spring -G spring

COPY --from=build /app/target/*.jar app.jar

RUN chown -R spring:spring /app

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:InitialRAMPercentage=50.0", "-XX:+UseG1GC", "-XX:+ExitOnOutOfMemoryError", "-Djava.security.egd=file:/dev/./urandom", "-Dfile.encoding=UTF-8", "-Duser.timezone=UTC", "-jar", "app.jar"]
