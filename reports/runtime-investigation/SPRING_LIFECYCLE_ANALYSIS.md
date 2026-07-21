# Spring Lifecycle Analysis

This report outlines the step-by-step lifecycle flow of the Spring ApplicationContext refresh phase executed during startup.

---

## 1. Context Refresh Sequence

1. **Bean Definition Loading**: Reads annotations and registers 29 repositories and custom services.
2. **Embedded Tomcat Start**:
   * Creates `TomcatServletWebServerFactory`.
   * Binds connector to port `8090`.
3. **BeanFactory Initialization**: Instantiates singleton instances.
4. **Data Migrations**: Flyway executes schemas before Hibernate initializes.
5. **JPA EntityManager Factory initialization**: Pre-compiles entities.
6. **Task Scheduler Activation**: Message Broker pool registers `@Scheduled` methods.
7. **CommandLineRunners Invocation**: Runs `DataInitializer`, `RevokedApiKeyBloomFilter`, `OpenApiGovernanceInitializer`, and `PlatformReadinessChecker` sequentially.
8. **Ready Alert**: Context triggers `ApplicationReadyEvent`.
