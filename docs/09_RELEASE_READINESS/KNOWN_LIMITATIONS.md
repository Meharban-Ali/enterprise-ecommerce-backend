# KNOWN ARCHITECTURAL LIMITATIONS

Before releasing the platform candidate, document active system limitations:

## 1. Database Schema Updates
The database relies on Hibernate's `ddl-auto=update` setting. Altering tables in staging or production without Flyway or Liquibase versioning scripts is a production blocker.

## 2. Horizontal Scaling Constraints
The outbox scheduler runs without a distributed lock (ShedLock). Deploying multiple application replicas concurrently will cause duplicate scheduler runs and duplicate email dispatches.

## 3. Container Root User Execution
The final Docker runner stage runs as root, increasing the risk of container escape.
