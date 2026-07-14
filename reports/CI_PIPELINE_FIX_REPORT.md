# CI Pipeline Fix Report

This report documents the resolution of the Linux executable permission error (`./mvnw: Permission denied`, exit code `126`) encountered on the GitHub Actions runners.

---

## 1. Root Cause
The Maven Wrapper script (`mvnw`) is a shell script that requires execution permissions (`+x`) on Unix/Linux systems. However:
1. Git metadata did not record the file as executable in the repository index.
2. The GitHub Actions runner checked out the codebase without the executable bit set.
3. When the runner attempted to execute `./mvnw`, the operating system blocked the process and returned exit code `126`.

---

## 2. Changes Made & Files Modified

### A. Repository Index Hardening
We updated Git index properties for the Maven Wrapper wrapper shell script to guarantee it carries the executable flag natively upon checkout:
```bash
git update-index --chmod=+x mvnw
```

### B. Workflow File Hardening
We added an explicit pre-requisite step to grant execute permissions immediately after checking out the repository and setting up Java. This is a best-practice safety gate preventing runner filesystem overrides.

Modified workflows:
* **[.github/workflows/build.yml](file:///D:/Meharban_Code/ecommerce/.github/workflows/build.yml#L34-L35)**:
  ```yaml
  - name: Grant Execute Permissions on Maven Wrapper
    run: chmod +x mvnw
  ```
* **[.github/workflows/test.yml](file:///D:/Meharban_Code/ecommerce/.github/workflows/test.yml#L51-L52)**:
  ```yaml
  - name: Grant Execute Permissions on Maven Wrapper
    run: chmod +x mvnw
  ```
* **[.github/workflows/release.yml](file:///D:/Meharban_Code/ecommerce/.github/workflows/release.yml#L42-L43)**:
  ```yaml
  - name: Grant Execute Permissions on Maven Wrapper
    run: chmod +x mvnw
  ```

---

## 3. Why the Fix Works
* By setting `git update-index --chmod=+x`, the file permission metadata is committed directly to the repository repository, assuring any clone or checkout on a Unix environment (including GitHub runner instances) gets checked out with `rwxr-xr-x` permissions.
* The `chmod +x mvnw` step serves as an inline fallback to guarantee that even if Git index settings are ignored by the runner's checkout tool, the runner explicitly modifies the permission bit before invocation.

---

## 4. Expected Pipeline Execution Flow
```
[ Trigger Event ]
        │
        ▼
[ Checkout Code ]
        │
        ▼
[ Setup Java 17 ]
        │
        ▼
[ Grant chmod +x mvnw ]  <-- (RESOLVED GATE)
        │
        ▼
[ Execute Maven Command ] <-- (SUCCESSFUL COMPILE/TEST/PACKAGE)
```
