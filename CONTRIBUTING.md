# Contributing Guide

This guide outlines standards and pull request workflows for contributing to the E-Commerce backend.

---

## 1. Development Setup
1. Fork and clone the repository.
2. Setup environment credentials using the template in `.env.example`.
3. Launch local supporting containers:
   ```bash
   docker compose up -d mysql redis
   ```
4. Build and compile the project:
   ```bash
   ./mvnw clean install
   ```

---

## 2. Coding Standards & Styles
* **Java Version**: JDK 17
* **Formatting**: Follow standard Google Java Style guide conventions.
* **Architecture Rules**:
  * Never bypass service layers. Controllers must communicate only with services, and services with repositories.
  * Always use DTOs for REST API inputs and outputs. Never expose JPA Entities directly.
  * Ensure transaction scopes are specified using `@Transactional` on services.

---

## 3. Commit Message Format
We enforce Conventional Commits. Use the following prefixes:
* `feat`: A new business feature.
* `fix`: A bug fix.
* `docs`: Documentation changes.
* `style`: Code formatting changes (no logic changes).
* `refactor`: Structural code edits (no logic changes).
* `test`: Adding or updating test cases.
* `chore`: Configuration, package dependencies, or build tool changes.

Example:
`feat(cart): implement cart item count aggregation API`

---

## 4. Pull Request Process
1. Create a descriptive feature branch: `feature/cart-aggregation` or `bugfix/issue-102`.
2. Implement your changes and add tests covering all new logic paths.
3. Verify the build compiles and all tests pass locally:
   ```bash
   ./mvnw clean verify
   ```
4. Submit the Pull Request to the `develop` branch.
5. All PRs require at least one approving review from a Principal Engineer and passing CI builds before merge.
