package org.marsem.kafka.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check controller for monitoring application status.
 * 
 * Provides endpoints for health checks, readiness probes, and application information.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@RestController
@RequestMapping("/health")
@Tag(name = "Health", description = "Application health and status")
@CrossOrigin(origins = {"http://localhost:3000", "https://your-hostname.com"})
public class HealthController {

    private final DataSource dataSource;
    private final BuildProperties buildProperties;

    @Autowired
    public HealthController(DataSource dataSource,
                           @Autowired(required = false) BuildProperties buildProperties) {
        this.dataSource = dataSource;
        this.buildProperties = buildProperties;
    }

    /**
     * Basic health check endpoint.
     */
    @GetMapping
    @Operation(summary = "Health check", description = "Basic application health check")
    @ApiResponse(responseCode = "200", description = "Application is healthy")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("application", "Kafka Web App v2");
        health.put("version", buildProperties != null ? buildProperties.getVersion() : "development");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Detailed health check with dependencies.
     */
    @GetMapping("/detailed")
    @Operation(summary = "Detailed health check", description = "Detailed health check including dependencies")
    @ApiResponse(responseCode = "200", description = "Detailed health information")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now());
        health.put("application", "Kafka Web App v2");
        health.put("version", buildProperties != null ? buildProperties.getVersion() : "development");
        health.put("buildTime", buildProperties != null ? buildProperties.getTime() : Instant.now());
        
        // Check database connectivity
        Map<String, Object> database = new HashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            database.put("status", "UP");
            database.put("url", conn.getMetaData().getURL());
            database.put("driver", conn.getMetaData().getDriverName());
        } catch (Exception e) {
            database.put("status", "DOWN");
            database.put("error", e.getMessage());
        }
        health.put("database", database);
        
        // System information
        Map<String, Object> system = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        system.put("processors", runtime.availableProcessors());
        system.put("totalMemory", runtime.totalMemory());
        system.put("freeMemory", runtime.freeMemory());
        system.put("maxMemory", runtime.maxMemory());
        health.put("system", system);
        
        return ResponseEntity.ok(health);
    }

    /**
     * Readiness probe for Kubernetes.
     */
    @GetMapping("/ready")
    @Operation(summary = "Readiness probe", description = "Kubernetes readiness probe")
    @ApiResponse(responseCode = "200", description = "Application is ready")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> readiness = new HashMap<>();
        
        // Check if database is accessible
        boolean databaseReady = false;
        try (Connection conn = dataSource.getConnection()) {
            databaseReady = conn.isValid(5);
        } catch (Exception e) {
            // Database not ready
        }
        
        if (databaseReady) {
            readiness.put("status", "READY");
            readiness.put("timestamp", Instant.now());
            return ResponseEntity.ok(readiness);
        } else {
            readiness.put("status", "NOT_READY");
            readiness.put("timestamp", Instant.now());
            readiness.put("reason", "Database not accessible");
            return ResponseEntity.status(503).body(readiness);
        }
    }

    /**
     * Liveness probe for Kubernetes.
     */
    @GetMapping("/live")
    @Operation(summary = "Liveness probe", description = "Kubernetes liveness probe")
    @ApiResponse(responseCode = "200", description = "Application is alive")
    public ResponseEntity<Map<String, Object>> live() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "ALIVE");
        liveness.put("timestamp", Instant.now());
        
        return ResponseEntity.ok(liveness);
    }
}
