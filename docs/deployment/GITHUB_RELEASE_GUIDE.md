# GitHub Release Guide

This guide defines the branching model, tagging conventions, release management, and hotfix workflows for the E-Commerce platform.

---

## 1. Branching Strategy
We follow a modified GitFlow branching model:

```
  main      ───────────────────────────────● (Production Releases)
                                          /
  release   ───────────●─────────────────/ (Release branch for testing)
                      /                 /
  develop   ─────────●─────────────────● (Integration Branch)
                    / \               /
  feature   ───────●   ●─────────────● (Feature development)
```

### A. Core Branches
* `main`: Represents the active production release. Code on this branch must always be stable. Direct commits are blocked.
* `develop`: Integration branch where features are merged. Direct commits are blocked.

### B. Supporting Branches
* `feature/*`: Branched from `develop`. Used for isolated feature development. Merged back to `develop` via pull requests.
* `release/*`: Branched from `develop` when preparing a release candidate. Only bug fixes are allowed. Merged to `main` and `develop`.
* `hotfix/*`: Branched from `main` to address critical production issues. Merged to `main` and `develop`.

---

## 2. Versioning Strategy
We adhere to Semantic Versioning (SemVer) `MAJOR.MINOR.PATCH`:
* **MAJOR**: Breaking API contract changes (e.g. `v2.0.0`).
* **MINOR**: New backward-compatible business features (e.g. `v1.1.0`).
* **PATCH**: Backward-compatible bug fixes or security patches (e.g. `v1.0.1`).

---

## 3. Tagging Strategy
* **Release Candidates**: Tagged as `vMAJOR.MINOR.PATCH-RC[Number]` (e.g., `v1.0.0-RC1`).
* **Production Releases**: Tagged as `vMAJOR.MINOR.PATCH` (e.g., `v1.0.0`).
* Create Git tags via command line:
  ```bash
  git tag -a v1.0.0 -m "Release version 1.0.0"
  git push origin v1.0.0
  ```

---

## 4. Hotfix Process
If a critical vulnerability or blocker is detected in production:
1. Create a hotfix branch from `main`:
   ```bash
   git checkout main
   git checkout -b hotfix/v1.0.1-session-leak
   ```
2. Implement the fix and run tests locally.
3. Open a pull request targeting `main`.
4. Once merged, tag the release as `v1.0.1` and deploy to production.
5. Merge the hotfix PR back into `develop` to sync codebases.
