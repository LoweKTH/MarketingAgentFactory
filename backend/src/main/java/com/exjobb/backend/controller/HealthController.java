package com.exjobb.backend.controller;

import com.exjobb.backend.dto.ApiResponse;
import com.exjobb.backend.service.PythonServiceClient;
import com.exjobb.backend.service.UserService;
import com.exjobb.backend.repository.TaskRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for system monitoring.
 *
 * This controller provides health endpoints that can be used by:
 * - Load balancers to check if the service is healthy
 * - Monitoring systems to track service availability
 * - Operations teams for troubleshooting
 *
 * It checks the health of various system components including
 * database connectivity, Python service availability, and overall system status.
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health Check", description = "System health monitoring endpoints")
public class HealthController {

    private final DataSource dataSource;
    private final PythonServiceClient pythonServiceClient;
    private final TaskRepository taskRepository;
    private final UserService userService;

    /**
     * Basic health check endpoint.
     *
     * Returns a simple health status that can be used by load balancers
     * and basic monitoring systems. This is a lightweight check that
     * doesn't perform deep system validation.
     *
     * @return Basic health status
     */
    @GetMapping
    @Operation(
            summary = "Basic health check",
            description = "Returns basic health status of the service"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Service is healthy"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "Service is unhealthy"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> basicHealthCheck() {
        log.debug("Basic health check requested");

        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            health.put("service", "marketing-agent-factory-backend");
            health.put("version", "1.0.0");

            return ResponseEntity.ok(
                    ApiResponse.success(health, "Service is healthy")
            );

        } catch (Exception e) {
            log.error("Basic health check failed", e);

            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("timestamp", LocalDateTime.now());
            health.put("error", e.getMessage());

            return ResponseEntity.status(503)
                    .body(ApiResponse.error("Service is unhealthy", "HEALTH_CHECK_FAILED", health));
        }
    }

    /**
     * Detailed health check endpoint.
     *
     * Performs comprehensive health checks on all system components
     * including database, Python service, and internal services.
     * This provides detailed information for troubleshooting.
     *
     * @return Detailed health status of all components
     */
    @GetMapping("/detailed")
    @Operation(
            summary = "Detailed health check",
            description = "Returns detailed health status of all system components"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Detailed health information retrieved"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "One or more components are unhealthy"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailedHealthCheck() {
        log.debug("Detailed health check requested");

        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "marketing-agent-factory-backend");
        health.put("version", "1.0.0");

        // Track overall health status
        boolean overallHealthy = true;

        // Check database connectivity
        Map<String, Object> databaseHealth = checkDatabaseHealth();
        health.put("database", databaseHealth);
        if (!"UP".equals(databaseHealth.get("status"))) {
            overallHealthy = false;
        }

        // Check Python service connectivity
        Map<String, Object> pythonServiceHealth = checkPythonServiceHealth();
        health.put("pythonService", pythonServiceHealth);
        if (!"UP".equals(pythonServiceHealth.get("status"))) {
            overallHealthy = false;
        }

        // Check system resources and statistics
        Map<String, Object> systemHealth = checkSystemHealth();
        health.put("system", systemHealth);

        // Check application-specific health
        Map<String, Object> applicationHealth = checkApplicationHealth();
        health.put("application", applicationHealth);
        if (!"UP".equals(applicationHealth.get("status"))) {
            overallHealthy = false;
        }

        // Set overall status
        health.put("status", overallHealthy ? "UP" : "DOWN");

        // Return appropriate response
        if (overallHealthy) {
            return ResponseEntity.ok(
                    ApiResponse.success(health, "All components are healthy")
            );
        } else {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error("One or more components are unhealthy", "HEALTH_CHECK_FAILED", health));
        }
    }

    /**
     * Readiness check endpoint.
     *
     * Checks if the service is ready to accept requests.
     * Different from liveness - the service might be alive but not ready
     * (e.g., still initializing, loading data, etc.).
     *
     * @return Readiness status
     */
    @GetMapping("/ready")
    @Operation(
            summary = "Readiness check",
            description = "Checks if the service is ready to accept requests"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Service is ready"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "Service is not ready"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> readinessCheck() {
        log.debug("Readiness check requested");

        try {
            Map<String, Object> readiness = new HashMap<>();
            readiness.put("timestamp", LocalDateTime.now());

            // Check if essential services are available
            boolean ready = true;

            // Database must be accessible
            if (!isDatabaseHealthy()) {
                ready = false;
                readiness.put("database", "NOT_READY");
            } else {
                readiness.put("database", "READY");
            }

            // Python service should be accessible (though we might still function without it)
            if (!isPythonServiceHealthy()) {
                readiness.put("pythonService", "NOT_READY");
                readiness.put("warning", "Python service unavailable - content generation will fail");
            } else {
                readiness.put("pythonService", "READY");
            }

            readiness.put("status", ready ? "READY" : "NOT_READY");

            if (ready) {
                return ResponseEntity.ok(
                        ApiResponse.success(readiness, "Service is ready")
                );
            } else {
                return ResponseEntity.status(503)
                        .body(ApiResponse.error("Service is not ready", "NOT_READY", readiness));
            }

        } catch (Exception e) {
            log.error("Readiness check failed", e);

            Map<String, Object> readiness = new HashMap<>();
            readiness.put("status", "NOT_READY");
            readiness.put("error", e.getMessage());
            readiness.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(503)
                    .body(ApiResponse.error("Readiness check failed", "READINESS_CHECK_FAILED", readiness));
        }
    }

    /**
     * Liveness check endpoint.
     *
     * Checks if the service is alive and functioning.
     * Used by orchestration platforms like Kubernetes to determine
     * if the service should be restarted.
     *
     * @return Liveness status
     */
    @GetMapping("/live")
    @Operation(
            summary = "Liveness check",
            description = "Checks if the service is alive and functioning"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Service is alive"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "503",
                    description = "Service is not responding properly"
            )
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> livenessCheck() {
        log.debug("Liveness check requested");

        try {
            // Basic liveness check - if we can execute this code, we're alive
            Map<String, Object> liveness = new HashMap<>();
            liveness.put("status", "ALIVE");
            liveness.put("timestamp", LocalDateTime.now());
            liveness.put("uptime", getUptime());

            return ResponseEntity.ok(
                    ApiResponse.success(liveness, "Service is alive")
            );

        } catch (Exception e) {
            log.error("Liveness check failed", e);

            Map<String, Object> liveness = new HashMap<>();
            liveness.put("status", "NOT_ALIVE");
            liveness.put("error", e.getMessage());
            liveness.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(503)
                    .body(ApiResponse.error("Service is not alive", "LIVENESS_CHECK_FAILED", liveness));
        }
    }

    /**
     * Check database health.
     *
     * @return Database health information
     */
    private Map<String, Object> checkDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();

        try {
            // Test database connectivity
            try (Connection connection = dataSource.getConnection()) {
                // Simple query to verify database is responding
                connection.createStatement().execute("SELECT 1");

                dbHealth.put("status", "UP");
                dbHealth.put("database", connection.getCatalog());
                dbHealth.put("url", connection.getMetaData().getURL());

                // Get some basic statistics
                long taskCount = taskRepository.count();
                dbHealth.put("taskCount", taskCount);

            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }

        return dbHealth;
    }

    /**
     * Check Python service health.
     *
     * @return Python service health information
     */
    private Map<String, Object> checkPythonServiceHealth() {
        Map<String, Object> pythonHealth = new HashMap<>();

        try {
            boolean isHealthy = pythonServiceClient.checkPythonServiceHealth();

            if (isHealthy) {
                pythonHealth.put("status", "UP");
                pythonHealth.put("message", "Python service is responding");
            } else {
                pythonHealth.put("status", "DOWN");
                pythonHealth.put("message", "Python service is not responding");
            }

        } catch (Exception e) {
            log.error("Python service health check failed", e);
            pythonHealth.put("status", "DOWN");
            pythonHealth.put("error", e.getMessage());
        }

        return pythonHealth;
    }

    /**
     * Check system health (memory, disk, etc.).
     *
     * @return System health information
     */
    private Map<String, Object> checkSystemHealth() {
        Map<String, Object> systemHealth = new HashMap<>();

        try {
            Runtime runtime = Runtime.getRuntime();

            // Memory information
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;

            Map<String, Object> memory = new HashMap<>();
            memory.put("maxMemoryMB", maxMemory / (1024 * 1024));
            memory.put("totalMemoryMB", totalMemory / (1024 * 1024));
            memory.put("usedMemoryMB", usedMemory / (1024 * 1024));
            memory.put("freeMemoryMB", freeMemory / (1024 * 1024));
            memory.put("memoryUsagePercent", Math.round((double) usedMemory / totalMemory * 100));

            systemHealth.put("memory", memory);

            // System properties
            systemHealth.put("javaVersion", System.getProperty("java.version"));
            systemHealth.put("osName", System.getProperty("os.name"));
            systemHealth.put("processorCount", runtime.availableProcessors());

            systemHealth.put("status", "UP");

        } catch (Exception e) {
            log.error("System health check failed", e);
            systemHealth.put("status", "DOWN");
            systemHealth.put("error", e.getMessage());
        }

        return systemHealth;
    }

    /**
     * Check application-specific health.
     *
     * @return Application health information
     */
    private Map<String, Object> checkApplicationHealth() {
        Map<String, Object> appHealth = new HashMap<>();

        try {
            // Check if core services are working
            UserService.UserStatistics userStats = userService.getUserStatistics();

            appHealth.put("status", "UP");
            appHealth.put("userCount", userStats.getTotalUsers());
            appHealth.put("activeUserCount", userStats.getActiveUsers());

            // Check for any critical issues
            if (userStats.getTotalUsers() == 0) {
                appHealth.put("warning", "No users in the system");
            }

        } catch (Exception e) {
            log.error("Application health check failed", e);
            appHealth.put("status", "DOWN");
            appHealth.put("error", e.getMessage());
        }

        return appHealth;
    }

    /**
     * Check if database is healthy.
     *
     * @return True if database is healthy
     */
    private boolean isDatabaseHealthy() {
        try {
            try (Connection connection = dataSource.getConnection()) {
                connection.createStatement().execute("SELECT 1");
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if Python service is healthy.
     *
     * @return True if Python service is healthy
     */
    private boolean isPythonServiceHealthy() {
        try {
            return pythonServiceClient.checkPythonServiceHealth();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get application uptime.
     *
     * @return Uptime information
     */
    private Map<String, Object> getUptime() {
        Map<String, Object> uptime = new HashMap<>();

        try {
            long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
            long uptimeSeconds = uptimeMs / 1000;
            long uptimeMinutes = uptimeSeconds / 60;
            long uptimeHours = uptimeMinutes / 60;
            long uptimeDays = uptimeHours / 24;

            uptime.put("uptimeMs", uptimeMs);
            uptime.put("uptimeFormatted", String.format("%dd %dh %dm %ds",
                    uptimeDays, uptimeHours % 24, uptimeMinutes % 60, uptimeSeconds % 60));

        } catch (Exception e) {
            uptime.put("error", "Could not determine uptime");
        }

        return uptime;
    }
}