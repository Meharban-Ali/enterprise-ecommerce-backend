# SECURITY EVOLUTION ROADMAP

This roadmap details the planned security enhancements.

## 1. OAuth2 & Identity Provider Integration
* **Keycloak Integration**: Replace custom JWT generation and credential verification with Keycloak or Okta.
  * *Business Value*: Standardizes authorization, supports Single Sign-On (SSO) and Multi-Factor Authentication (MFA).
  * *Complexity*: High.
  * *Priority*: **HIGH** (Target: Sprint 17).

---

## 2. Secrets Management
* **HashiCorp Vault**: Store database passwords, SMTP credentials, and Stripe webhook secret keys securely in Vault instead of environment variables.
  * *Business Value*: Automates secret rotations, prevents leaks.
  * *Complexity*: Medium.
  * *Priority*: **MEDIUM** (Target: Sprint 16).
