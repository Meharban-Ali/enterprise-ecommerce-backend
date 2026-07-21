# Mail Subsystem — Root Cause Report

This report documents the diagnostic findings of the connection failure to the SMTP host.

---

## 1. Investigation & Evidence

* **Symptom**: Actuator Health reported the `mail` component as `DOWN`, with error message:
  ```text
  Couldn't connect to host localhost:25
  ```
* **Discovery Trace**:
  * **Environment File**: The `.env` file explicitly declared:
    ```properties
    SMTP_HOST=localhost
    SMTP_PORT=25
    ```
    This overrode auto-configurations since `spring-dotenv` injected these variables into the environment.
  * **Default Fallbacks**: In `application-dev.properties`, configurations were resolved via placeholders defaulting to localhost:25:
    ```properties
    spring.mail.host=${SMTP_HOST:localhost}
    spring.mail.port=${SMTP_PORT:25}
    ```
  * **Authentication Flags**: Development configurations disabled SMTP authentication (`spring.mail.properties.mail.smtp.auth=false`) and STARTTLS (`spring.mail.properties.mail.smtp.starttls.enable=false`), preventing integration with standard external email relays.
