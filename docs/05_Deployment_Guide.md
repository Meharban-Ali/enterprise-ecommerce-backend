# 05. Deployment Guide

## 1. Containerization
The project utilizes a multi-stage Dockerfile:
* **Stage 1 (Builder)**: Compiles source code using Alpine Maven image.
* **Stage 2 (Runner)**: Minimal Eclipse Temurin JRE runtime image.

## 2. Nginx Load Balancing Configuration
Configure proxy pass forwarding headers:
```nginx
location / {
    proxy_pass http://app_servers;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    client_max_body_size 2M;
}
```
