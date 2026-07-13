# API CONTRACT CHECKLIST

Use this checklist to verify API contracts and REST standards compliance.

* [ ] **HTTP Method Mapping**: Endpoints use correct HTTP methods (e.g., `POST` for creation, `GET` for retrieval, `PUT` for updates).
* [ ] **Status Code Mapping**: Endpoints return correct status codes (e.g., `201 Created` for creations, `200 OK` for retrievals).
* [ ] **Header Mapping**: Secured routes require `Authorization: Bearer <token>` and return `X-Correlation-ID` headers.
* [ ] **Validation Constraints**: Inputs are annotated with Jakarta validation constraints (e.g., `@NotNull`, `@Size`).
* [ ] **Response Consistency**: Responses are mapped to a standard JSON wrapper (`success`, `message`, `data`).
* [ ] **Error Envelope Integrity**: Error responses match the standard wrapper and do not expose stack traces.
