package org.marsem.kafka.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.marsem.kafka.model.Connection;
import org.marsem.kafka.service.KafkaConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for managing Kafka connections.
 * 
 * Provides endpoints for CRUD operations on Kafka connection configurations,
 * connection testing, and connection statistics.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@RestController
@RequestMapping("/connections")
@Tag(name = "Connections", description = "Kafka connection management")
@CrossOrigin(origins = {"http://localhost:3000", "https://kafkatool.marsem.org"})
public class ConnectionController {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionController.class);

    private final KafkaConnectionService connectionService;

    @Autowired
    public ConnectionController(KafkaConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Get all connections.
     */
    @GetMapping
    @Operation(summary = "Get all connections", description = "Retrieve all Kafka connection configurations")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved connections"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<Connection>> getAllConnections(
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter by security protocol") @RequestParam(required = false) String securityProtocol,
            @Parameter(description = "Filter by creator") @RequestParam(required = false) String createdBy) {
        
        try {
            List<Connection> connections;
            
            if (active != null || securityProtocol != null || createdBy != null) {
                connections = connectionService.findConnectionsByCriteria(active, securityProtocol, createdBy);
            } else {
                connections = connectionService.getAllConnections();
            }
            
            logger.info("Retrieved {} connections", connections.size());
            return ResponseEntity.ok(connections);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve connections", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active connections only.
     */
    @GetMapping("/active")
    @Operation(summary = "Get active connections", description = "Retrieve only active Kafka connections")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved active connections")
    public ResponseEntity<List<Connection>> getActiveConnections() {
        try {
            List<Connection> connections = connectionService.getActiveConnections();
            logger.info("Retrieved {} active connections", connections.size());
            return ResponseEntity.ok(connections);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve active connections", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get connection by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get connection by ID", description = "Retrieve a specific connection by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connection found"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Connection> getConnectionById(
            @Parameter(description = "Connection ID") @PathVariable Long id) {
        
        try {
            Optional<Connection> connection = connectionService.getConnectionById(id);
            
            if (connection.isPresent()) {
                logger.info("Retrieved connection: {}", connection.get().getName());
                return ResponseEntity.ok(connection.get());
            } else {
                logger.warn("Connection not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Failed to retrieve connection with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new connection.
     */
    @PostMapping
    @Operation(summary = "Create connection", description = "Create a new Kafka connection configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Connection created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid connection data"),
        @ApiResponse(responseCode = "409", description = "Connection name already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Connection> createConnection(
            @Parameter(description = "Connection configuration") @Valid @RequestBody Connection connection) {
        
        try {
            Connection createdConnection = connectionService.createConnection(connection);
            logger.info("Created connection: {}", createdConnection.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdConnection);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid connection data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Failed to create connection", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update an existing connection.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update connection", description = "Update an existing Kafka connection configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connection updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid connection data"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "409", description = "Connection name already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Connection> updateConnection(
            @Parameter(description = "Connection ID") @PathVariable Long id,
            @Parameter(description = "Updated connection configuration") @Valid @RequestBody Connection connection) {
        
        try {
            Connection updatedConnection = connectionService.updateConnection(id, connection);
            logger.info("Updated connection: {}", updatedConnection.getName());
            return ResponseEntity.ok(updatedConnection);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid connection data or connection not found: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Failed to update connection with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a connection.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete connection", description = "Delete a Kafka connection configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Connection deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteConnection(
            @Parameter(description = "Connection ID") @PathVariable Long id) {
        
        try {
            connectionService.deleteConnection(id);
            logger.info("Deleted connection with ID: {}", id);
            return ResponseEntity.noContent().build();
            
        } catch (IllegalArgumentException e) {
            logger.warn("Connection not found with ID: {}", id);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Failed to delete connection with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Test a connection.
     */
    @PostMapping("/{id}/test")
    @Operation(summary = "Test connection", description = "Test connectivity to a Kafka cluster")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connection test completed"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> testConnection(
            @Parameter(description = "Connection ID") @PathVariable Long id) {
        
        try {
            Optional<Connection> connectionOpt = connectionService.getConnectionById(id);
            
            if (connectionOpt.isEmpty()) {
                logger.warn("Connection not found with ID: {}", id);
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> testResult = connectionService.testConnection(connectionOpt.get());
            logger.info("Connection test completed for ID: {}", id);
            return ResponseEntity.ok(testResult);
            
        } catch (Exception e) {
            logger.error("Failed to test connection with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Test connection with provided configuration (without saving).
     */
    @PostMapping("/test")
    @Operation(summary = "Test connection configuration", description = "Test a connection configuration without saving it")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connection test completed"),
        @ApiResponse(responseCode = "400", description = "Invalid connection configuration"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Map<String, Object>> testConnectionConfig(
            @Parameter(description = "Connection configuration to test") @Valid @RequestBody Connection connection) {
        
        try {
            Map<String, Object> testResult = connectionService.testConnection(connection);
            logger.info("Connection configuration test completed for: {}", connection.getName());
            return ResponseEntity.ok(testResult);
            
        } catch (Exception e) {
            logger.error("Failed to test connection configuration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get connection statistics.
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get connection statistics", description = "Retrieve statistics about all connections")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics")
    public ResponseEntity<Map<String, Object>> getConnectionStatistics() {
        try {
            Map<String, Object> statistics = connectionService.getConnectionStatistics();
            logger.info("Retrieved connection statistics");
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve connection statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
