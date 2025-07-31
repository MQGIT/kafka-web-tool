package org.marsem.kafka.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.marsem.kafka.model.KafkaMessage;
import org.marsem.kafka.service.KafkaProducerService;
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
 * REST controller for Kafka message production.
 * 
 * Provides endpoints for sending messages to Kafka topics with support
 * for single messages, batch operations, and asynchronous processing.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@RestController
@RequestMapping("/producer")
@Tag(name = "Producer", description = "Kafka message production")
@CrossOrigin(origins = {"http://localhost:3000", "https://kafkatool.marsem.org"})
public class ProducerController {

    private static final Logger logger = LoggerFactory.getLogger(ProducerController.class);

    private final KafkaProducerService producerService;

    @Autowired
    public ProducerController(KafkaProducerService producerService) {
        this.producerService = producerService;
    }

    /**
     * Send a single message to a Kafka topic.
     */
    @PostMapping("/connections/{connectionId}/send")
    @Operation(summary = "Send message", description = "Send a single message to a Kafka topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid message data"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to send message")
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> sendMessage(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Message to send") @Valid @RequestBody KafkaMessage message) {

        try {
            logger.info("Sending message to topic: {} using connection: {}", message.getTopic(), connectionId);

            return producerService.sendMessage(connectionId, message)
                .thenApply(result -> ResponseEntity.ok(result))
                .exceptionally(ex -> {
                    logger.error("Failed to send message", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());

        } catch (Exception e) {
            logger.error("Failed to send message", e);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * Send a single message synchronously.
     */
    @PostMapping("/connections/{connectionId}/send/sync")
    @Operation(summary = "Send message synchronously", description = "Send a single message and wait for result")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid message data"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to send message")
    })
    public ResponseEntity<Map<String, Object>> sendMessageSync(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Message to send") @Valid @RequestBody KafkaMessage message) {
        
        try {
            logger.info("Sending message synchronously to topic: {} using connection: {}", message.getTopic(), connectionId);
            
            CompletableFuture<Map<String, Object>> future = producerService.sendMessage(connectionId, message);
            Map<String, Object> result = future.get(); // Wait for completion
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Failed to send message synchronously", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Send multiple messages in batch.
     */
    @PostMapping("/connections/{connectionId}/send/batch")
    @Operation(summary = "Send messages in batch", description = "Send multiple messages to Kafka topics in batch")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch send initiated"),
        @ApiResponse(responseCode = "400", description = "Invalid message data"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to send batch")
    })
    public ResponseEntity<CompletableFuture<Map<String, Object>>> sendMessagesBatch(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Messages to send") @Valid @RequestBody List<KafkaMessage> messages) {
        
        try {
            if (messages == null || messages.isEmpty()) {
                logger.warn("Empty message batch provided");
                return ResponseEntity.badRequest().build();
            }
            
            logger.info("Sending batch of {} messages using connection: {}", messages.size(), connectionId);
            
            CompletableFuture<Map<String, Object>> result = producerService.sendMessagesBatch(connectionId, messages);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Failed to send message batch", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Send a message asynchronously with callback.
     */
    @PostMapping("/connections/{connectionId}/send/async")
    @Operation(summary = "Send message asynchronously", description = "Send a message asynchronously with callback")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "Message send initiated"),
        @ApiResponse(responseCode = "400", description = "Invalid message data"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to initiate send")
    })
    public ResponseEntity<CompletableFuture<Map<String, Object>>> sendMessageAsync(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Message to send") @Valid @RequestBody KafkaMessage message) {
        
        try {
            logger.info("Sending message asynchronously to topic: {} using connection: {}", message.getTopic(), connectionId);
            
            CompletableFuture<Map<String, Object>> result = producerService.sendMessageAsync(connectionId, message);
            
            return ResponseEntity.accepted().body(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Failed to send message asynchronously", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get producer metrics for a connection.
     */
    @GetMapping("/connections/{connectionId}/metrics")
    @Operation(summary = "Get producer metrics", description = "Retrieve producer metrics for a specific connection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Connection not found or no active producer"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve metrics")
    })
    public ResponseEntity<Map<String, Object>> getProducerMetrics(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId) {
        
        try {
            Map<String, Object> metrics = producerService.getProducerMetrics(connectionId);
            
            if (metrics.containsKey("error")) {
                logger.warn("No active producer for connection: {}", connectionId);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("Retrieved producer metrics for connection: {}", connectionId);
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            logger.error("Failed to retrieve producer metrics for connection: {}", connectionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Close producer for a specific connection.
     */
    @DeleteMapping("/connections/{connectionId}/producer")
    @Operation(summary = "Close producer", description = "Close the producer for a specific connection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Producer closed successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to close producer")
    })
    public ResponseEntity<Void> closeProducer(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId) {
        
        try {
            producerService.closeProducer(connectionId);
            logger.info("Closed producer for connection: {}", connectionId);
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Failed to close producer for connection: {}", connectionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Close all producers.
     */
    @DeleteMapping("/producers")
    @Operation(summary = "Close all producers", description = "Close all active producers")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "All producers closed successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to close producers")
    })
    public ResponseEntity<Void> closeAllProducers() {
        try {
            producerService.closeAllProducers();
            logger.info("Closed all producers");
            return ResponseEntity.noContent().build();
            
        } catch (Exception e) {
            logger.error("Failed to close all producers", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Send a simple text message (convenience endpoint).
     */
    @PostMapping("/connections/{connectionId}/topics/{topicName}/send")
    @Operation(summary = "Send simple message", description = "Send a simple text message to a topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to send message")
    })
    public ResponseEntity<CompletableFuture<Map<String, Object>>> sendSimpleMessage(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName,
            @Parameter(description = "Message key") @RequestParam(required = false) String key,
            @Parameter(description = "Message value") @RequestBody String value) {
        
        try {
            if (value == null || value.trim().isEmpty()) {
                logger.warn("Empty message value provided");
                return ResponseEntity.badRequest().build();
            }
            
            KafkaMessage message = new KafkaMessage(topicName, key, value);
            
            logger.info("Sending simple message to topic: {} using connection: {}", topicName, connectionId);
            
            CompletableFuture<Map<String, Object>> result = producerService.sendMessage(connectionId, message);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Failed to send simple message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
