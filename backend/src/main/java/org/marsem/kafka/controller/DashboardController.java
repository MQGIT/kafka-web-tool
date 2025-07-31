package org.marsem.kafka.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.marsem.kafka.service.DashboardService;
import org.marsem.kafka.service.KafkaConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for dashboard metrics and statistics.
 * 
 * Provides endpoints for retrieving application metrics, Kafka cluster statistics,
 * and real-time monitoring data for the dashboard.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Dashboard metrics and statistics")
@CrossOrigin(origins = {"http://localhost:3000", "https://kafkawebtool.marsem.org"})
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    private final DashboardService dashboardService;
    private final KafkaConsumerService consumerService;

    @Autowired
    public DashboardController(DashboardService dashboardService, KafkaConsumerService consumerService) {
        this.dashboardService = dashboardService;
        this.consumerService = consumerService;
    }

    /**
     * Get overall dashboard metrics.
     */
    @GetMapping("/metrics")
    @Operation(summary = "Get dashboard metrics", description = "Get overall dashboard metrics and statistics")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved dashboard metrics")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getDashboardMetrics() {
        
        try {
            logger.info("Getting dashboard metrics");
            
            return dashboardService.getDashboardMetrics()
                .thenApply(metrics -> ResponseEntity.ok(metrics))
                .exceptionally(ex -> {
                    logger.error("Failed to retrieve dashboard metrics", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
            
        } catch (Exception e) {
            logger.error("Failed to retrieve dashboard metrics", e);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * Get metrics for a specific connection.
     */
    @GetMapping("/connections/{connectionId}/metrics")
    @Operation(summary = "Get connection metrics", description = "Get metrics for a specific Kafka connection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved connection metrics"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve connection metrics")
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getConnectionMetrics(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId) {
        
        try {
            logger.info("Getting metrics for connection: {}", connectionId);
            
            return dashboardService.getConnectionMetrics(connectionId)
                .thenApply(metrics -> ResponseEntity.ok(metrics))
                .exceptionally(ex -> {
                    logger.error("Failed to retrieve metrics for connection: {}", connectionId, ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
            
        } catch (IllegalArgumentException e) {
            logger.warn("Connection not found: {}", connectionId);
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
            
        } catch (Exception e) {
            logger.error("Failed to retrieve metrics for connection: {}", connectionId, e);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * Get application health metrics.
     */
    @GetMapping("/health")
    @Operation(summary = "Get health metrics", description = "Get application health and performance metrics")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved health metrics")
    public ResponseEntity<Map<String, Object>> getHealthMetrics() {
        
        try {
            logger.info("Getting health metrics");
            
            Map<String, Object> healthMetrics = dashboardService.getHealthMetrics();
            return ResponseEntity.ok(healthMetrics);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve health metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all running consumer sessions.
     */
    @GetMapping("/running-consumers")
    @Operation(summary = "Get running consumers", description = "Get all currently running consumer sessions")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved running consumers")
    public ResponseEntity<List<Map<String, Object>>> getRunningConsumers() {
        try {
            logger.info("Getting running consumer sessions");

            List<Map<String, Object>> runningConsumers = consumerService.getRunningConsumerSessions();
            return ResponseEntity.ok(runningConsumers);

        } catch (Exception e) {
            logger.error("Failed to retrieve running consumers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Stop a running consumer session.
     */
    @PostMapping("/running-consumers/{sessionId}/stop")
    @Operation(summary = "Stop consumer", description = "Stop a running consumer session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consumer stopped successfully"),
        @ApiResponse(responseCode = "404", description = "Consumer session not found"),
        @ApiResponse(responseCode = "500", description = "Failed to stop consumer")
    })
    public ResponseEntity<Map<String, Object>> stopRunningConsumer(
            @Parameter(description = "Consumer session ID") @PathVariable String sessionId) {
        try {
            logger.info("Stopping consumer session: {}", sessionId);

            Map<String, Object> result = consumerService.stopConsumer(sessionId);

            if ((Boolean) result.get("success")) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("Failed to stop consumer session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Stop all running consumer sessions.
     */
    @PostMapping("/running-consumers/stop-all")
    @Operation(summary = "Stop all consumers", description = "Stop all running consumer sessions")
    @ApiResponse(responseCode = "200", description = "All consumers stopped successfully")
    public ResponseEntity<Map<String, Object>> stopAllRunningConsumers() {
        try {
            logger.info("Stopping all running consumer sessions");

            consumerService.stopAllConsumers();

            Map<String, Object> result = Map.of(
                "success", true,
                "message", "All running consumers stopped successfully"
            );

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            logger.error("Failed to stop all consumers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get recent activity for dashboard.
     */
    @GetMapping("/recent-activity")
    @Operation(summary = "Get recent activity", description = "Get recent activity for dashboard display")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved recent activity")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity() {
        try {
            logger.info("Getting recent activity");

            List<Map<String, Object>> activity = dashboardService.getRecentActivity();
            return ResponseEntity.ok(activity);

        } catch (Exception e) {
            logger.error("Failed to retrieve recent activity", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
