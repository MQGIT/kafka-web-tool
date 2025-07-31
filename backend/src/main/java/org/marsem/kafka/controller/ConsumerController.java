package org.marsem.kafka.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.marsem.kafka.model.ConsumerSession;
import org.marsem.kafka.model.ConsumerStatus;
import org.marsem.kafka.model.KafkaMessage;
import org.marsem.kafka.service.KafkaConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for Kafka message consumption.
 * 
 * Provides endpoints for managing consumer sessions, starting/stopping consumers,
 * and monitoring consumption progress with real-time WebSocket streaming.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@RestController
@RequestMapping("/consumer")
@Tag(name = "Consumer", description = "Kafka message consumption")
@CrossOrigin(origins = {"http://localhost:3000", "https://kafkatool.marsem.org"})
public class ConsumerController {

    private static final Logger logger = LoggerFactory.getLogger(ConsumerController.class);

    private final KafkaConsumerService consumerService;

    @Autowired
    public ConsumerController(KafkaConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    /**
     * Create a new consumer session.
     */
    @PostMapping("/sessions")
    @Operation(summary = "Create consumer session", description = "Create a new consumer session for a topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Consumer session created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid session data"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to create session")
    })
    public ResponseEntity<ConsumerSession> createConsumerSession(
            @Parameter(description = "Consumer session configuration") @Valid @RequestBody ConsumerSession session) {
        
        try {
            ConsumerSession createdSession = consumerService.createConsumerSession(session);
            logger.info("Created consumer session: {}", createdSession.getSessionId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSession);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid session data: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Failed to create consumer session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all consumer sessions.
     */
    @GetMapping("/sessions")
    @Operation(summary = "Get all consumer sessions", description = "Retrieve all consumer sessions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved sessions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<List<ConsumerSession>> getAllConsumerSessions(
            @Parameter(description = "Filter by connection ID") @RequestParam(required = false) Long connectionId,
            @Parameter(description = "Filter by topic") @RequestParam(required = false) String topic,
            @Parameter(description = "Filter by status") @RequestParam(required = false) ConsumerStatus status,
            @Parameter(description = "Filter by creator") @RequestParam(required = false) String createdBy) {
        
        try {
            List<ConsumerSession> sessions;
            
            if (connectionId != null || topic != null || status != null || createdBy != null) {
                // Use criteria-based search when filters are provided
                sessions = consumerService.getAllConsumerSessions().stream()
                        .filter(session -> connectionId == null || session.getConnectionId().equals(connectionId))
                        .filter(session -> topic == null || session.getTopic().equals(topic))
                        .filter(session -> status == null || session.getStatus().equals(status))
                        .filter(session -> createdBy == null || session.getCreatedBy().equals(createdBy))
                        .toList();
            } else {
                sessions = consumerService.getAllConsumerSessions();
            }
            
            logger.info("Retrieved {} consumer sessions", sessions.size());
            return ResponseEntity.ok(sessions);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve consumer sessions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get active consumer sessions.
     */
    @GetMapping("/sessions/active")
    @Operation(summary = "Get active consumer sessions", description = "Retrieve only active consumer sessions")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved active sessions")
    public ResponseEntity<List<ConsumerSession>> getActiveConsumerSessions() {
        try {
            List<ConsumerSession> sessions = consumerService.getActiveConsumerSessions();
            logger.info("Retrieved {} active consumer sessions", sessions.size());
            return ResponseEntity.ok(sessions);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve active consumer sessions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get consumer session by ID.
     */
    @GetMapping("/sessions/{sessionId}")
    @Operation(summary = "Get consumer session", description = "Retrieve a specific consumer session by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session found"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ConsumerSession> getConsumerSession(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {
        
        try {
            Optional<ConsumerSession> session = consumerService.getConsumerSession(sessionId);
            
            if (session.isPresent()) {
                logger.info("Retrieved consumer session: {}", sessionId);
                return ResponseEntity.ok(session.get());
            } else {
                logger.warn("Consumer session not found: {}", sessionId);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            logger.error("Failed to retrieve consumer session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get consumer sessions by connection.
     */
    @GetMapping("/connections/{connectionId}/sessions")
    @Operation(summary = "Get sessions by connection", description = "Retrieve consumer sessions for a specific connection")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved sessions")
    public ResponseEntity<List<ConsumerSession>> getConsumerSessionsByConnection(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId) {
        
        try {
            List<ConsumerSession> sessions = consumerService.getConsumerSessionsByConnection(connectionId);
            logger.info("Retrieved {} consumer sessions for connection: {}", sessions.size(), connectionId);
            return ResponseEntity.ok(sessions);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve consumer sessions for connection: {}", connectionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Start a consumer session.
     */
    @PostMapping("/sessions/{sessionId}/start")
    @Operation(summary = "Start consumer", description = "Start consuming messages for a session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consumer started successfully"),
        @ApiResponse(responseCode = "400", description = "Consumer already running or invalid state"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "500", description = "Failed to start consumer")
    })
    public ResponseEntity<CompletableFuture<Map<String, Object>>> startConsumer(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {
        
        try {
            CompletableFuture<Map<String, Object>> result = consumerService.startConsumer(sessionId);
            logger.info("Starting consumer session: {}", sessionId);
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request to start consumer: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Failed to start consumer session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Stop a consumer session.
     */
    @PostMapping("/sessions/{sessionId}/stop")
    @Operation(summary = "Stop consumer", description = "Stop consuming messages for a session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consumer stopped successfully"),
        @ApiResponse(responseCode = "404", description = "Session not found or not running"),
        @ApiResponse(responseCode = "500", description = "Failed to stop consumer")
    })
    public ResponseEntity<Map<String, Object>> stopConsumer(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {
        
        try {
            Map<String, Object> result = consumerService.stopConsumer(sessionId);
            logger.info("Stopping consumer session: {}", sessionId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to stop consumer session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Pause a consumer session.
     */
    @PostMapping("/sessions/{sessionId}/pause")
    @Operation(summary = "Pause consumer", description = "Pause consuming messages for a session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consumer paused successfully"),
        @ApiResponse(responseCode = "400", description = "Consumer not running"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "500", description = "Failed to pause consumer")
    })
    public ResponseEntity<Map<String, Object>> pauseConsumer(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {
        
        try {
            Map<String, Object> result = consumerService.pauseConsumer(sessionId);
            logger.info("Pausing consumer session: {}", sessionId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to pause consumer session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Resume a paused consumer session.
     */
    @PostMapping("/sessions/{sessionId}/resume")
    @Operation(summary = "Resume consumer", description = "Resume consuming messages for a paused session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Consumer resumed successfully"),
        @ApiResponse(responseCode = "400", description = "Consumer not paused"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "500", description = "Failed to resume consumer")
    })
    public ResponseEntity<Map<String, Object>> resumeConsumer(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {
        
        try {
            Map<String, Object> result = consumerService.resumeConsumer(sessionId);
            logger.info("Resuming consumer session: {}", sessionId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to resume consumer session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a consumer session.
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Delete consumer session", description = "Delete a consumer session")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Session deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Session not found"),
        @ApiResponse(responseCode = "500", description = "Failed to delete session")
    })
    public ResponseEntity<Void> deleteConsumerSession(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {
        
        try {
            consumerService.deleteConsumerSession(sessionId);
            logger.info("Deleted consumer session: {}", sessionId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Failed to delete consumer session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Stop all active consumers.
     */
    @PostMapping("/sessions/stop-all")
    @Operation(summary = "Stop all consumers", description = "Stop all active consumer sessions")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All consumers stopped successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to stop consumers")
    })
    public ResponseEntity<Map<String, Object>> stopAllConsumers() {
        try {
            consumerService.stopAllConsumers();
            logger.info("Stopped all active consumers");
            
            Map<String, Object> result = Map.of(
                "success", true,
                "message", "All active consumers stopped successfully"
            );
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to stop all consumers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create and start a consumer session in one call (convenience endpoint).
     */
    @PostMapping("/connections/{connectionId}/topics/{topicName}/consume")
    @Operation(summary = "Quick consume", description = "Create and start a consumer session for a topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Consumer session created and started"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to create/start consumer")
    })
    public ResponseEntity<ConsumerSession> quickConsume(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName,
            @Parameter(description = "Consumer group") @RequestParam String consumerGroup,
            @Parameter(description = "Max messages to consume") @RequestParam(required = false) Integer maxMessages,
            @Parameter(description = "Specific partition") @RequestParam(required = false) Integer partition,
            @Parameter(description = "Start offset") @RequestParam(required = false) Long startOffset) {
        
        try {
            // Create consumer session
            ConsumerSession session = new ConsumerSession();
            session.setConnectionId(connectionId);
            session.setTopic(topicName);
            session.setConsumerGroup(consumerGroup);
            session.setMaxMessages(maxMessages);
            session.setPartitionId(partition);
            session.setStartOffset(startOffset);
            session.setAutoCommit(true);
            session.setPollTimeoutMs(1000);
            
            ConsumerSession createdSession = consumerService.createConsumerSession(session);
            
            // Start the consumer
            consumerService.startConsumer(createdSession.getSessionId());
            
            logger.info("Created and started consumer session: {} for topic: {}", 
                       createdSession.getSessionId(), topicName);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSession);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid parameters for quick consume: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Failed to create/start consumer for topic: {}", topicName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get consumed messages for a session.
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "Get consumed messages", description = "Get messages consumed by a session")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved consumed messages")
    public ResponseEntity<List<KafkaMessage>> getConsumedMessages(
            @Parameter(description = "Session ID") @PathVariable String sessionId) {

        try {
            List<KafkaMessage> messages = consumerService.getConsumedMessages(sessionId);
            logger.info("Retrieved {} consumed messages for session: {}", messages.size(), sessionId);
            return ResponseEntity.ok(messages);

        } catch (Exception e) {
            logger.error("Failed to retrieve consumed messages for session: {}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
