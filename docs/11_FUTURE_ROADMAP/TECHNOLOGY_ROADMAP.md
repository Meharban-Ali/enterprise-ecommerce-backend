# TECHNOLOGY INTEGRATION ROADMAP

This roadmap details the future technologies to be integrated into the platform.

## 1. Future Tech Stack Integrations

* **Apache Kafka**: Replaces database outbox polling with event-driven message streaming.
  * *Expected Benefits*: Decouples message dispatch, supports high-throughput notification streams.
  * *Complexity*: High.
  * *Priority*: **HIGH** (Target: Sprint 18).
* **Elasticsearch / OpenSearch**: Speeds up product catalog searches.
  * *Expected Benefits*: Sub-second autocomplete search queries, fuzzy matching on catalog name substrings.
  * *Complexity*: Medium.
  * *Priority*: **MEDIUM** (Target: Sprint 19).
* **HashiCorp Vault**: Replaces environment variable secrets.
  * *Expected Benefits*: Automated secrets rotation, secure credential storage.
  * *Complexity*: Medium.
  * *Priority*: **MEDIUM** (Target: Sprint 16).
