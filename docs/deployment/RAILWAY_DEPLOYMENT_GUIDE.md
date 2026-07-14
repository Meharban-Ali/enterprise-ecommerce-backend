# Railway Deployment Guide

This guide describes how to configure, provision, and deploy the E-Commerce Spring Boot application to Railway.

---

## 1. Provision Infrastructure
1. Log in to the [Railway Console](https://railway.app/).
2. Click **New Project** -> **Provision MySQL**.
3. Click **New** -> **Provision Redis**.

---

## 2. Deploy the Application Service
1. Click **New** -> **GitHub Repo**.
2. Connect your GitHub account and select your repository.
3. Railway will automatically detect the `Dockerfile` in the root directory and initiate a multi-stage container build.

---

## 3. Configure Service Variables
Select the **app** service and navigate to the **Variables** tab. Add the following keys:
* `SPRING_PROFILES_ACTIVE=prod`
* `DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}`
* `DB_USERNAME=${{MySQL.MYSQLUSER}}`
* `DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}`
* `REDIS_HOST=${{Redis.REDISHOST}}`
* `REDIS_PORT=${{Redis.REDISPORT}}`
* `JWT_SECRET=your_custom_base64_secret`
* `SUPER_ADMIN_NAME=superadmin`
* `SUPER_ADMIN_EMAIL=superadmin@company.com`
* `SUPER_ADMIN_PASSWORD=SecurePassword@2026!`
* `SUPER_ADMIN_PHONE=+14155551234`
* `APP_SECURITY_RATE_LIMIT_ENABLED=true`
* `MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED=true`

---

## 4. Configure Ingress & Public Domain
1. In the **app** service settings, scroll down to the **Networking** section.
2. Click **Generate Domain**. Railway will expose a public URL (e.g. `https://app-production.up.railway.app`).

---

## 5. Configure Health Check Probes
1. In the **app** service settings, navigate to the **Healthchecks** section.
2. Enter the path: `/actuator/health`.
3. Set the start period to `40` seconds (to allow JVM context initialization).
