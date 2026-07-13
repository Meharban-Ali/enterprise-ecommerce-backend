package com.redis.monitoring.service;

import com.redis.incident.dto.response.IncidentDashboardResponse;
import com.redis.payment.entity.PaymentExpirationScheduler;
import com.redis.monitoring.dto.response.AlertResponse;
import com.redis.reliability.dto.RecentErrorsResponse;
import com.redis.notification.entity.NotificationOutboxScheduler;
import com.redis.reliability.dto.ModuleHealthResponse;
import com.redis.reliability.dto.ModulesHealthResponse;
import com.redis.order.repository.OrderRepository;
import com.redis.reliability.dto.DashboardResponse;
import com.redis.reliability.dto.SystemInfoResponse;
import com.redis.observability.dto.response.JvmMetricsResponse;
import com.redis.monitoring.dto.MonitoringMetadata;
import com.redis.audit.repository.AuditLogRepository;
import com.redis.user.repository.UserRepository;
import com.redis.notification.repository.NotificationRepository;
import com.redis.reliability.dto.SystemHealthResponse;
import com.redis.notification.entity.NotificationRetryScheduler;
import com.redis.incident.dto.response.IncidentResponse;
import com.redis.reliability.dto.SchedulerStatusResponse;
import com.redis.product.repository.ProductRepository;
import com.redis.webhook.dto.response.WebhookDashboardResponse;
import com.redis.observability.dto.response.SystemMetricsResponse;
import com.redis.reliability.dto.RecentSystemErrorResponse;
import com.redis.notification.entity.NotificationStatus;
import com.redis.webhook.service.WebhookMetricsService;
import com.redis.payment.repository.PaymentRepository;
import com.redis.user.repository.UserSessionRepository;
import com.redis.incident.service.IncidentService;
import com.redis.audit.event.AuditEventListener;
import com.redis.webhook.dto.response.WebhookStatisticsResponse;

import com.redis.infrastructure.config.MonitoringProperties;
import com.redis.monitoring.event.MonitoringEvent;
import com.redis.monitoring.service.HealthIndicatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.GarbageCollectorMXBean;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemMonitoringServiceImpl implements SystemMonitoringService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ProductRepository productRepository;
    private final UserSessionRepository userSessionRepository;

    private final List<HealthIndicatorService> healthIndicators;
    private final MonitoringProperties monitoringProperties;
    private final MonitoringAlertService alertService;
    private final ApplicationEventPublisher eventPublisher;

    private final ObjectProvider<IncidentService> incidentServiceProvider;
    private final ObjectProvider<WebhookMetricsService> webhookMetricsServiceProvider;

    // Autowire schedulers to collect metrics
    private final PaymentExpirationScheduler paymentExpirationScheduler;
    private final NotificationRetryScheduler notificationRetryScheduler;
    private final NotificationOutboxScheduler notificationOutboxScheduler;
    private final AuditEventListener auditEventListener;

    // Executor for health check timeouts
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // In-memory circular buffer to store recent errors
    private final Queue<RecentSystemErrorResponse> errorBuffer = new ConcurrentLinkedQueue<>();

    // Internal cache map
    private final Map<String, CachedObject<?>> cache = new ConcurrentHashMap<>();

    private static class CachedObject<T> {
        private final T value;
        private final long expiryTime;

        CachedObject(T value, long ttlSeconds) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private String getRootCauseMessage(Throwable throwable) {
        if (throwable == null) return "No message";
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }

    private String getCorrelationId() {
        String cid = MDC.get("CorrelationId");
        return (cid != null && !cid.isBlank()) ? cid : "SYSTEM";
    }

    private String getGeneratedBy() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "anonymousUser";
    }

    @Override
    public void registerError(String component, Throwable throwable) {
        registerError(component, throwable, 0L, "ERROR");
    }

    public void registerError(String component, Throwable throwable, long durationMs, String severity) {
        String corrId = getCorrelationId();
        RecentSystemErrorResponse error = RecentSystemErrorResponse.builder()
                .component(component)
                .errorType(throwable.getClass().getSimpleName())
                .message(throwable.getMessage())
                .timestamp(LocalDateTime.now())
                .module(component)
                .exceptionClass(throwable.getClass().getName())
                .rootCauseMessage(getRootCauseMessage(throwable))
                .correlationId(corrId)
                .executionDurationMs(durationMs)
                .severity(severity)
                .build();

        errorBuffer.offer(error);
        while (errorBuffer.size() > monitoringProperties.getRecentErrorLimit()) {
            errorBuffer.poll();
        }
    }

    @Override
    public List<RecentSystemErrorResponse> getRecentErrors() {
        List<RecentSystemErrorResponse> list = new ArrayList<>(errorBuffer);
        list.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public DashboardResponse getDashboard() {
        long startTime = System.currentTimeMillis();
        String cacheKey = "dashboard";
        CachedObject<DashboardResponse> cached = (CachedObject<DashboardResponse>) cache.get(cacheKey);
        
        boolean hit = cached != null && !cached.isExpired();
        DashboardResponse dashboard;
        
        if (hit) {
            dashboard = cached.value;
            // Update metadata for cache hit representation
            MonitoringMetadata md = dashboard.getMetadata();
            if (md != null) {
                md.setCacheHit(true);
                md.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                md.setCorrelationId(getCorrelationId());
                md.setGeneratedBy(getGeneratedBy());
            }
            return dashboard;
        }

        // Construct Dashboard components
        SystemHealthResponse health = getSystemHealth();
        SystemMetricsResponse metrics = getSystemMetrics();
        List<SchedulerStatusResponse> schedulers = getSchedulerStatuses();
        List<RecentSystemErrorResponse> recentErrors = getRecentErrors();
        SystemInfoResponse systemInfo = getSystemInfo();
        JvmMetricsResponse jvmMetrics = getJvmMetrics();
        
        List<AlertResponse> alerts = alertService.evaluateAlertRules(health, metrics, schedulers, jvmMetrics);

        // Fetch Incident Analytics
        IncidentService incidentService = incidentServiceProvider.getIfAvailable();
        IncidentDashboardResponse incidentDashboard = null;
        List<IncidentResponse> activeIncidentsList = new ArrayList<>();
        long activeIncCount = 0;
        long critIncCount = 0;
        long avgResolveTime = 0;
        double availability = 100.0;
        Map<String, Long> trend = new HashMap<>();
        
        if (incidentService != null) {
            incidentDashboard = incidentService.getIncidentDashboard();
            activeIncidentsList = incidentDashboard.getActiveIncidentsList();
            activeIncCount = incidentDashboard.getActiveIncidents();
            critIncCount = incidentDashboard.getOpenCriticalIncidents();
            avgResolveTime = incidentDashboard.getAverageResolutionTimeMs();
            availability = incidentDashboard.getSystemAvailabilityPercentage();
            trend = incidentDashboard.getTopAlertRules();
        }

        // Fetch Webhook Analytics
        WebhookMetricsService webhookMetricsService = webhookMetricsServiceProvider.getIfAvailable();
        WebhookDashboardResponse webhookDashboard = null;
        double webhookSuccessRate = 100.0;
        long webhookFailures = 0;
        long webhookRetryQueue = 0;
        long webhookDlq = 0;
        double webhookAverageResponseTime = 0.0;

        if (webhookMetricsService != null) {
            webhookDashboard = webhookMetricsService.getDashboard();
            webhookSuccessRate = webhookDashboard.getEndpointsStats().isEmpty() ? 100.0 :
                    webhookDashboard.getEndpointsStats().stream().mapToDouble(WebhookStatisticsResponse::getSuccessPercentage).average().orElse(100.0);
            webhookFailures = webhookDashboard.getEndpointsStats().stream().mapToLong(WebhookStatisticsResponse::getFailureCount).sum();
            webhookRetryQueue = webhookDashboard.getWebhookRetryQueue();
            webhookDlq = webhookDashboard.getWebhookDlq();
            webhookAverageResponseTime = webhookDashboard.getAverageResponseTimeMs();
        }

        long execTime = System.currentTimeMillis() - startTime;
        MonitoringMetadata metadata = MonitoringMetadata.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(getGeneratedBy())
                .executionTimeMs(execTime)
                .cacheHit(false)
                .apiVersion("v1")
                .correlationId(getCorrelationId())
                .build();

        dashboard = DashboardResponse.builder()
                .generatedAt(LocalDateTime.now())
                .health(health)
                .metrics(metrics)
                .schedulers(schedulers)
                .recentErrors(recentErrors)
                .systemInfo(systemInfo)
                .jvmMetrics(jvmMetrics)
                .alerts(alerts)
                .generationTimeMs(execTime)
                .metadata(metadata)
                .activeAlerts(alerts)
                .activeIncidents(activeIncCount)
                .criticalIncidents(critIncCount)
                .averageResolutionTime(avgResolveTime)
                .systemAvailability(availability)
                .alertTrend(trend)
                .activeIncidentsList(activeIncidentsList)
                .incidentDashboard(incidentDashboard)
                .webhookDashboard(webhookDashboard)
                .webhookSuccessRate(webhookSuccessRate)
                .webhookFailures(webhookFailures)
                .webhookRetryQueue(webhookRetryQueue)
                .webhookDlq(webhookDlq)
                .webhookAverageResponseTime(webhookAverageResponseTime)
                .build();

        cache.put(cacheKey, new CachedObject<>(dashboard, monitoringProperties.getDashboardCacheSeconds()));
        return dashboard;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SystemHealthResponse getSystemHealth() {
        long startTime = System.currentTimeMillis();
        String cacheKey = "health";
        CachedObject<SystemHealthResponse> cached = (CachedObject<SystemHealthResponse>) cache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            SystemHealthResponse response = cached.value;
            if (response.getMetadata() != null) {
                response.getMetadata().setCacheHit(true);
                response.getMetadata().setExecutionTimeMs(System.currentTimeMillis() - startTime);
                response.getMetadata().setCorrelationId(getCorrelationId());
            }
            return response;
        }

        List<ModuleHealthResponse> modulesList = new ArrayList<>();
        String overallStatus = "UP";
        boolean hasDegraded = false;
        boolean hasWarning = false;
        boolean hasDown = false;

        // Submit ALL health checks in parallel
        Map<HealthIndicatorService, Future<ModuleHealthResponse>> futures = new LinkedHashMap<>();
        for (HealthIndicatorService indicator : healthIndicators) {
            futures.put(indicator, executorService.submit(indicator::checkHealth));
        }

        // Collect health responses
        for (Map.Entry<HealthIndicatorService, Future<ModuleHealthResponse>> entry : futures.entrySet()) {
            HealthIndicatorService indicator = entry.getKey();
            Future<ModuleHealthResponse> future = entry.getValue();
            ModuleHealthResponse response;
            try {
                response = future.get(monitoringProperties.getMonitoringTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("Health check for {} timed out", indicator.getName());
                eventPublisher.publishEvent(new MonitoringEvent(this, "HEALTH_CHECK_TIMEOUT", indicator.getName(), "Health check timed out"));
                response = ModuleHealthResponse.builder()
                        .moduleName(indicator.getName())
                        .status("WARNING")
                        .message("Health check timed out after " + monitoringProperties.getMonitoringTimeoutMs() + "ms")
                        .build();
            } catch (Exception e) {
                log.error("Health check for {} failed with error", indicator.getName(), e);
                response = ModuleHealthResponse.builder()
                        .moduleName(indicator.getName())
                        .status("DOWN")
                        .message("Unexpected health exception: " + e.getMessage())
                        .build();
            }

            modulesList.add(response);
            if ("DOWN".equals(response.getStatus())) {
                hasDown = true;
            } else if ("DEGRADED".equals(response.getStatus())) {
                hasDegraded = true;
            } else if ("WARNING".equals(response.getStatus())) {
                hasWarning = true;
            }
        }

        if (hasDown) {
            overallStatus = "DOWN";
        } else if (hasDegraded) {
            overallStatus = "DEGRADED";
        } else if (hasWarning) {
            overallStatus = "WARNING";
        }

        String dbStatus = modulesList.stream().filter(m -> "Database".equals(m.getModuleName())).map(ModuleHealthResponse::getStatus).findFirst().orElse("UP");
        String redisStatus = modulesList.stream().filter(m -> "Redis".equals(m.getModuleName())).map(ModuleHealthResponse::getStatus).findFirst().orElse("UP");
        String notificationStatus = modulesList.stream().filter(m -> "Notifications".equals(m.getModuleName())).map(ModuleHealthResponse::getStatus).findFirst().orElse("UP");
        String paymentStatus = modulesList.stream().filter(m -> "Payments".equals(m.getModuleName())).map(ModuleHealthResponse::getStatus).findFirst().orElse("UP");
        String schedulerStatus = modulesList.stream().filter(m -> "Scheduler".equals(m.getModuleName())).map(ModuleHealthResponse::getStatus).findFirst().orElse("UP");

        long activeUsers = 0;
        try {
            activeUsers = userSessionRepository.countOnlineSessions(LocalDateTime.now().minusMinutes(15));
        } catch (Exception e) {
            log.warn("Failed to fetch online session metrics dynamically: {}", e.getMessage());
        }

        long execTime = System.currentTimeMillis() - startTime;
        MonitoringMetadata metadata = MonitoringMetadata.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(getGeneratedBy())
                .executionTimeMs(execTime)
                .cacheHit(false)
                .apiVersion("v1")
                .correlationId(getCorrelationId())
                .build();

        SystemHealthResponse health = SystemHealthResponse.builder()
                .applicationStatus(overallStatus)
                .databaseStatus(dbStatus)
                .redisStatus(redisStatus)
                .notificationStatus(notificationStatus)
                .paymentStatus(paymentStatus)
                .schedulerStatus(schedulerStatus)
                .uptimeSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000)
                .activeUsersCount((int) activeUsers)
                .timestamp(LocalDateTime.now())
                .modules(modulesList)
                .metadata(metadata)
                .build();

        cache.put(cacheKey, new CachedObject<>(health, monitoringProperties.getHealthCacheSeconds()));
        return health;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SystemMetricsResponse getSystemMetrics() {
        long startTime = System.currentTimeMillis();
        String cacheKey = "metrics";
        CachedObject<SystemMetricsResponse> cached = (CachedObject<SystemMetricsResponse>) cache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            SystemMetricsResponse response = cached.value;
            if (response.getMetadata() != null) {
                response.getMetadata().setCacheHit(true);
                response.getMetadata().setExecutionTimeMs(System.currentTimeMillis() - startTime);
                response.getMetadata().setCorrelationId(getCorrelationId());
            }
            return response;
        }

        // Fail-safe Graceful Degradation queries
        long users = 0;
        try { users = userRepository.count(); } catch (Exception e) { log.warn("Graceful degradation: UserRepository.count() failed", e); }
        
        long orders = 0;
        try { orders = orderRepository.count(); } catch (Exception e) { log.warn("Graceful degradation: OrderRepository.count() failed", e); }
        
        long payments = 0;
        try { payments = paymentRepository.count(); } catch (Exception e) { log.warn("Graceful degradation: PaymentRepository.count() failed", e); }
        
        long notifications = 0;
        try { notifications = notificationRepository.count(); } catch (Exception e) { log.warn("Graceful degradation: NotificationRepository.count() failed", e); }
        
        long logsCount = 0;
        try { logsCount = auditLogRepository.count(); } catch (Exception e) { log.warn("Graceful degradation: AuditLogRepository.count() failed", e); }
        
        long activeProducts = 0;
        try { activeProducts = productRepository.count(); } catch (Exception e) { log.warn("Graceful degradation: ProductRepository.count() failed", e); }
        
        long lowStock = 0;
        try { lowStock = productRepository.countLowStock(10); } catch (Exception e) { log.warn("Graceful degradation: countLowStock failed", e); }
        
        long failedNotif = 0;
        try { failedNotif = notificationRepository.countByStatus(com.redis.notification.entity.NotificationStatus.FAILED); } catch (Exception e) { log.warn("Graceful degradation: countByStatus failed", e); }
        
        long retryQueue = 0;
        try { retryQueue = notificationRepository.countByStatus(com.redis.notification.entity.NotificationStatus.PENDING); } catch (Exception e) { log.warn("Graceful degradation: countByStatus failed", e); }
        
        long dlq = 0;
        try { dlq = notificationRepository.countByStatusAndRetryCountGreaterThanEqual(com.redis.notification.entity.NotificationStatus.FAILED, 3); } catch (Exception e) { log.warn("Graceful degradation: countByStatusAndRetryCount failed", e); }

        long activeSessions = 0;
        try { activeSessions = userSessionRepository.countOnlineSessions(LocalDateTime.now().minusMinutes(15)); } catch (Exception e) { log.warn("Graceful degradation: countOnlineSessions failed", e); }

        Map<String, Object> infra = new HashMap<>();
        infra.put("cpuCores", Runtime.getRuntime().availableProcessors());
        infra.put("heapMaxBytes", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax());
        infra.put("heapUsedBytes", ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());

        Map<String, Object> business = new HashMap<>();
        business.put("totalUsersCount", users);
        business.put("totalOrdersCount", orders);
        business.put("totalPaymentsCount", payments);
        business.put("totalNotificationsCount", notifications);
        business.put("activeProductsCount", activeProducts);
        business.put("lowStockProductsCount", lowStock);

        Map<String, Object> runtime = new HashMap<>();
        runtime.put("activeSessionsCount", activeSessions);
        runtime.put("totalAuditLogsCount", logsCount);
        runtime.put("failedNotificationsCount", failedNotif);
        runtime.put("retryQueueSize", retryQueue);
        runtime.put("dlqSize", dlq);

        long execTime = System.currentTimeMillis() - startTime;
        MonitoringMetadata metadata = MonitoringMetadata.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(getGeneratedBy())
                .executionTimeMs(execTime)
                .cacheHit(false)
                .apiVersion("v1")
                .correlationId(getCorrelationId())
                .build();

        SystemMetricsResponse metrics = SystemMetricsResponse.builder()
                .totalUsers(users)
                .totalOrders(orders)
                .totalPayments(payments)
                .totalNotifications(notifications)
                .totalAuditLogs(logsCount)
                .activeProducts(activeProducts)
                .lowStockProducts(lowStock)
                .failedNotificationsCount(failedNotif)
                .retryQueueSize(retryQueue)
                .dlqSize(dlq)
                .infrastructureMetrics(infra)
                .businessMetrics(business)
                .runtimeMetrics(runtime)
                .metadata(metadata)
                .build();

        cache.put(cacheKey, new CachedObject<>(metrics, monitoringProperties.getMetricsCacheSeconds()));
        return metrics;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SystemInfoResponse getSystemInfo() {
        long startTime = System.currentTimeMillis();
        String cacheKey = "systemInfo";
        CachedObject<SystemInfoResponse> cached = (CachedObject<SystemInfoResponse>) cache.get(cacheKey);
        
        if (cached != null && !cached.isExpired()) {
            SystemInfoResponse response = cached.value;
            if (response.getMetadata() != null) {
                response.getMetadata().setCacheHit(true);
                response.getMetadata().setExecutionTimeMs(System.currentTimeMillis() - startTime);
                response.getMetadata().setCorrelationId(getCorrelationId());
            }
            return response;
        }

        String host = "localhost";
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {}

        long execTime = System.currentTimeMillis() - startTime;
        MonitoringMetadata metadata = MonitoringMetadata.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(getGeneratedBy())
                .executionTimeMs(execTime)
                .cacheHit(false)
                .apiVersion("v1")
                .correlationId(getCorrelationId())
                .build();

        SystemInfoResponse info = SystemInfoResponse.builder()
                .applicationName("E-Commerce Application")
                .applicationVersion("1.0.0")
                .buildVersion("9.1-ENHANCED")
                .gitCommit("9.1.2-GA")
                .activeProfile("test")
                .javaVersion(System.getProperty("java.version"))
                .jvmVendor(System.getProperty("java.vm.vendor"))
                .operatingSystem(System.getProperty("os.name"))
                .hostname(host)
                .availableCpuCores(Runtime.getRuntime().availableProcessors())
                .jvmStartTimeMs(ManagementFactory.getRuntimeMXBean().getStartTime())
                .applicationUptimeSeconds(ManagementFactory.getRuntimeMXBean().getUptime() / 1000)
                .springBootVersion(org.springframework.boot.SpringBootVersion.getVersion())
                .osArchitecture(System.getProperty("os.arch"))
                .timezone(TimeZone.getDefault().getID())
                .jvmUptimeMs(ManagementFactory.getRuntimeMXBean().getUptime())
                .metadata(metadata)
                .build();

        cache.put(cacheKey, new CachedObject<>(info, 300));
        return info;
    }

    @Override
    public JvmMetricsResponse getJvmMetrics() {
        long startTime = System.currentTimeMillis();
        long heapUsed = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
        long heapMax = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
        double heapUtil = heapMax > 0 ? ((double) heapUsed / heapMax) * 100.0 : 0.0;

        long gcCount = 0;
        long gcTime = 0;
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            long count = gc.getCollectionCount();
            long time = gc.getCollectionTime();
            if (count > 0) gcCount += count;
            if (time > 0) gcTime += time;
        }

        long execTime = System.currentTimeMillis() - startTime;
        MonitoringMetadata metadata = MonitoringMetadata.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(getGeneratedBy())
                .executionTimeMs(execTime)
                .cacheHit(false)
                .apiVersion("v1")
                .correlationId(getCorrelationId())
                .build();

        return JvmMetricsResponse.builder()
                .heapUsedBytes(heapUsed)
                .heapMaxBytes(heapMax)
                .heapUtilizationPercentage(heapUtil)
                .nonHeapUsedBytes(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed())
                .threadCount(ManagementFactory.getThreadMXBean().getThreadCount())
                .daemonThreadCount(ManagementFactory.getThreadMXBean().getDaemonThreadCount())
                .peakThreadCount(ManagementFactory.getThreadMXBean().getPeakThreadCount())
                .garbageCollectionCount(gcCount)
                .garbageCollectionTimeMs(gcTime)
                .metadata(metadata)
                .build();
    }

    @Override
    public List<SchedulerStatusResponse> getSchedulerStatuses() {
        List<SchedulerStatusResponse> list = new ArrayList<>();
        if (monitoringProperties.isSchedulerMetricsEnabled()) {
            list.add(paymentExpirationScheduler.getStatusDetails());
            list.add(notificationRetryScheduler.getStatusDetails());
            list.add(notificationOutboxScheduler.getStatusDetails());
        }
        return list;
    }

    @Override
    public RecentErrorsResponse getRecentErrorsWrapped() {
        long startTime = System.currentTimeMillis();
        List<RecentSystemErrorResponse> errors = getRecentErrors();
        long execTime = System.currentTimeMillis() - startTime;
        
        MonitoringMetadata metadata = MonitoringMetadata.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(getGeneratedBy())
                .executionTimeMs(execTime)
                .cacheHit(false)
                .apiVersion("v1")
                .correlationId(getCorrelationId())
                .build();
                
        return RecentErrorsResponse.builder()
                .errors(errors)
                .metadata(metadata)
                .build();
    }

    @Override
    public ModulesHealthResponse getModulesHealthWrapped() {
        long startTime = System.currentTimeMillis();
        SystemHealthResponse health = getSystemHealth();
        long execTime = System.currentTimeMillis() - startTime;
        
        MonitoringMetadata metadata = MonitoringMetadata.builder()
                .generatedAt(LocalDateTime.now())
                .generatedBy(getGeneratedBy())
                .executionTimeMs(execTime)
                .cacheHit(health.getMetadata() != null && health.getMetadata().isCacheHit())
                .apiVersion("v1")
                .correlationId(getCorrelationId())
                .build();
                
        return ModulesHealthResponse.builder()
                .modules(health.getModules())
                .metadata(metadata)
                .build();
    }
}
