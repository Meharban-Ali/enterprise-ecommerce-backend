# CI/CD Pipeline Guide

This guide details the GitHub Actions CI/CD automation pipelines, workflow sequences, branch execution matrices, artifact uploads, and secrets management.

---

## 1. Pipeline Architecture
Our automation guarantees that every code change is validated, tested, packaged, and containerized before release promotion.

```
       [ COMMIT / PR ]
              │
      ┌───────┴───────┐
      ▼               ▼
 [ build.yml ]   [ test.yml ]
  (Compile)     (Unit & Integration)
      │               │
      └───────┬───────┘
              ▼
        [ docker.yml ]
      (Container Build)
              │
              ▼
        [ release.yml ] (Git tag v*)
     (Draft GitHub Release)
```

---

## 2. Workflow Index

* **[build.yml](file:///D:/Meharban_Code/ecommerce/.github/workflows/build.yml)**: Verifies clean compilation and packages the runnable application JAR.
* **[test.yml](file:///D:/Meharban_Code/ecommerce/.github/workflows/test.yml)**: Launches parallel MySQL and Redis Docker service containers inside the runner network and executes all 360 unit and integration tests.
* **[docker.yml](file:///D:/Meharban_Code/ecommerce/.github/workflows/docker.yml)**: Builds and inspects the production Docker container without pushing it to registry namespaces.
* **[release.yml](file:///D:/Meharban_Code/ecommerce/.github/workflows/release.yml)**: Triggers only when version tags are pushed (`v1.0.0`), running the test/compile cycle, building the docker release image, and publishing a draft release on GitHub containing build artifacts.

---

## 3. Branch Execution Matrix

| Branch / Tag | build.yml | test.yml | docker.yml | release.yml |
| :--- | :---: | :---: | :---: | :---: |
| `main` | **YES** | **YES** | **YES** | No |
| `develop` | **YES** | **YES** | **YES** | No |
| `feature/*` | **YES** | No | No | No |
| `release/*` | **YES** | **YES** | **YES** | No |
| `hotfix/*` | **YES** | **YES** | **YES** | No |
| Git tags `v*` | No | No | No | **YES** |

---

## 4. Required GitHub Secrets
To secure sensitive runtime connections during deployments, configure the following secrets inside your GitHub repository settings (`Settings -> Secrets and variables -> Actions`):

* `JWT_SECRET`: The Base64 token signing key.
* `SMTP_USERNAME` / `SMTP_PASSWORD`: SMTP credentials.
* `DB_USERNAME` / `DB_PASSWORD`: Database administrator credentials.

---

## 5. Future CI/CD Integrations
In the future deployment phase (Sprint D5), the pipelines will be connected to target environments:
* **Railway / Render / Fly.io**: Automatically triggers image pulls via CLI deploy triggers or webhooks.
* **AWS ECS / Azure ACA**: Configured by injecting AWS/Azure credentials and mapping target image pushes to ECR/ACR.
* **Kubernetes**: Deploys via `kubectl apply -f k8s/` or Helm charts using GitHub Runner service hooks.
