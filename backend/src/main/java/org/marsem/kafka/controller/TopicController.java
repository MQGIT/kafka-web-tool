package org.marsem.kafka.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.marsem.kafka.model.TopicInfo;
import org.marsem.kafka.model.KafkaMessage;
import org.marsem.kafka.model.CreateTopicRequest;
import org.marsem.kafka.service.TopicManagementService;
import org.marsem.kafka.service.KafkaProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for Kafka topic management.
 * 
 * Provides endpoints for topic discovery, metadata retrieval, topic creation/deletion,
 * partition information, and consumer group details.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@RestController
@RequestMapping("/topics")
@Tag(name = "Topics", description = "Kafka topic management and browsing")
@CrossOrigin(origins = {"http://localhost:3000", "https://kafkatool.marsem.org"})
public class TopicController {

    private static final Logger logger = LoggerFactory.getLogger(TopicController.class);

    private final TopicManagementService topicService;
    private final KafkaProducerService producerService;

    @Autowired
    public TopicController(TopicManagementService topicService, KafkaProducerService producerService) {
        this.topicService = topicService;
        this.producerService = producerService;
    }

    /**
     * Get all topics for a connection.
     */
    @GetMapping("/connections/{connectionId}")
    @Operation(summary = "Get all topics", description = "Retrieve all topics for a Kafka connection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved topics"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve topics")
    })
    public CompletableFuture<ResponseEntity<List<TopicInfo>>> getTopics(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId) {

        try {
            logger.info("Getting topics for connection: {}", connectionId);

            return topicService.getTopics(connectionId)
                .thenApply(topics -> ResponseEntity.ok(topics))
                .exceptionally(ex -> {
                    logger.error("Failed to retrieve topics for connection: {}", connectionId, ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
            
        } catch (IllegalArgumentException e) {
            logger.warn("Connection not found: {}", connectionId);
            return CompletableFuture.completedFuture(ResponseEntity.notFound().build());

        } catch (Exception e) {
            logger.error("Failed to retrieve topics for connection: {}", connectionId, e);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * Get detailed information for a specific topic.
     */
    @GetMapping("/connections/{connectionId}/topics/{topicName}")
    @Operation(summary = "Get topic details", description = "Retrieve detailed information for a specific topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved topic details"),
        @ApiResponse(responseCode = "404", description = "Connection or topic not found"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve topic details")
    })
    public ResponseEntity<CompletableFuture<TopicInfo>> getTopicInfo(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName) {
        
        try {
            logger.info("Getting topic info for: {} on connection: {}", topicName, connectionId);
            
            CompletableFuture<TopicInfo> topicInfo = topicService.getTopicInfo(connectionId, topicName);
            
            return ResponseEntity.ok(topicInfo);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Connection not found: {}", connectionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Failed to retrieve topic info for: {} on connection: {}", topicName, connectionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get consumer groups for a topic.
     */
    @GetMapping("/connections/{connectionId}/topics/{topicName}/consumer-groups")
    @Operation(summary = "Get topic consumer groups", description = "Retrieve consumer groups for a specific topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved consumer groups"),
        @ApiResponse(responseCode = "404", description = "Connection or topic not found"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve consumer groups")
    })
    public ResponseEntity<CompletableFuture<List<TopicInfo.ConsumerGroupInfo>>> getTopicConsumerGroups(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName) {
        
        try {
            logger.info("Getting consumer groups for topic: {} on connection: {}", topicName, connectionId);
            
            CompletableFuture<List<TopicInfo.ConsumerGroupInfo>> consumerGroups = 
                topicService.getTopicConsumerGroups(connectionId, topicName);
            
            return ResponseEntity.ok(consumerGroups);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Connection not found: {}", connectionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Failed to retrieve consumer groups for topic: {} on connection: {}", topicName, connectionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get partition offsets for a topic.
     */
    @GetMapping("/connections/{connectionId}/topics/{topicName}/offsets")
    @Operation(summary = "Get topic offsets", description = "Retrieve partition offsets for a specific topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved offsets"),
        @ApiResponse(responseCode = "404", description = "Connection or topic not found"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve offsets")
    })
    public ResponseEntity<CompletableFuture<Map<Integer, Map<String, Long>>>> getTopicOffsets(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName) {
        
        try {
            logger.info("Getting offsets for topic: {} on connection: {}", topicName, connectionId);
            
            CompletableFuture<Map<Integer, Map<String, Long>>> offsets = 
                topicService.getTopicOffsets(connectionId, topicName);
            
            return ResponseEntity.ok(offsets);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Connection not found: {}", connectionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Failed to retrieve offsets for topic: {} on connection: {}", topicName, connectionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }





    /**
     * Get topic statistics summary.
     */
    @GetMapping("/connections/{connectionId}/statistics")
    @Operation(summary = "Get topic statistics", description = "Retrieve statistics summary for all topics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved statistics"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve statistics")
    })
    public ResponseEntity<CompletableFuture<Map<String, Object>>> getTopicStatistics(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId) {
        
        try {
            logger.info("Getting topic statistics for connection: {}", connectionId);
            
            CompletableFuture<Map<String, Object>> result = topicService.getTopics(connectionId)
                .thenApply(topics -> {
                    Map<String, Object> stats = new HashMap<>();
                    
                    long totalTopics = topics.size();
                    long totalPartitions = topics.stream()
                        .mapToLong(topic -> topic.getPartitionCount() != null ? topic.getPartitionCount() : 0)
                        .sum();
                    long totalMessages = topics.stream()
                        .mapToLong(topic -> topic.getTotalMessages() != null ? topic.getTotalMessages() : 0)
                        .sum();
                    long internalTopics = topics.stream()
                        .mapToLong(topic -> topic.isInternal() ? 1 : 0)
                        .sum();
                    
                    stats.put("totalTopics", totalTopics);
                    stats.put("totalPartitions", totalPartitions);
                    stats.put("totalMessages", totalMessages);
                    stats.put("internalTopics", internalTopics);
                    stats.put("userTopics", totalTopics - internalTopics);
                    
                    return stats;
                });
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Connection not found: {}", connectionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Failed to retrieve topic statistics for connection: {}", connectionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Search topics by name pattern.
     */
    @GetMapping("/connections/{connectionId}/search")
    @Operation(summary = "Search topics", description = "Search topics by name pattern")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved matching topics"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to search topics")
    })
    public ResponseEntity<CompletableFuture<List<TopicInfo>>> searchTopics(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Search pattern") @RequestParam String pattern,
            @Parameter(description = "Include internal topics") @RequestParam(defaultValue = "false") boolean includeInternal) {
        
        try {
            if (pattern == null || pattern.trim().isEmpty()) {
                logger.warn("Empty search pattern provided");
                return ResponseEntity.badRequest().build();
            }
            
            logger.info("Searching topics with pattern: {} on connection: {}", pattern, connectionId);
            
            CompletableFuture<List<TopicInfo>> result = topicService.getTopics(connectionId)
                .thenApply(topics -> topics.stream()
                    .filter(topic -> topic.getName().toLowerCase().contains(pattern.toLowerCase()))
                    .filter(topic -> includeInternal || !topic.isInternal())
                    .toList());
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Connection not found: {}", connectionId);
            return ResponseEntity.notFound().build();
            
        } catch (Exception e) {
            logger.error("Failed to search topics on connection: {}", connectionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Browse messages in a topic.
     */
    @GetMapping("/connections/{connectionId}/topics/{topicName}/messages")
    @Operation(summary = "Browse messages", description = "Browse messages in a specific topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved messages"),
        @ApiResponse(responseCode = "404", description = "Connection or topic not found"),
        @ApiResponse(responseCode = "500", description = "Failed to browse messages")
    })
    public CompletableFuture<ResponseEntity<List<KafkaMessage>>> browseMessages(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName,
            @Parameter(description = "Partition number") @RequestParam(required = false) Integer partition,
            @Parameter(description = "Start offset") @RequestParam(required = false, defaultValue = "latest") String startOffset,
            @Parameter(description = "Maximum messages to retrieve") @RequestParam(required = false, defaultValue = "100") Integer limit) {

        try {
            logger.info("Browsing messages for topic: {} on connection: {}, partition: {}, offset: {}, limit: {}",
                       topicName, connectionId, partition, startOffset, limit);

            return topicService.browseMessages(connectionId, topicName, partition, startOffset, limit)
                .thenApply(messages -> ResponseEntity.ok(messages))
                .exceptionally(ex -> {
                    logger.error("Failed to browse messages for topic: {} on connection: {}", topicName, connectionId, ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());

        } catch (Exception e) {
            logger.error("Failed to browse messages for topic: {} on connection: {}", topicName, connectionId, e);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * Get topic statistics including message count.
     */
    @GetMapping("/connections/{connectionId}/topics/{topicName}/stats")
    @Operation(summary = "Get topic statistics", description = "Get detailed statistics for a specific topic including message count")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved topic statistics"),
        @ApiResponse(responseCode = "404", description = "Connection or topic not found"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve topic statistics")
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> getTopicStats(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName) {

        try {
            logger.info("Getting topic statistics for: {} on connection: {}", topicName, connectionId);

            return topicService.getTopicStats(connectionId, topicName)
                .thenApply(stats -> ResponseEntity.ok(stats))
                .exceptionally(ex -> {
                    logger.error("Failed to retrieve topic statistics for: {} on connection: {}", topicName, connectionId, ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().build());

        } catch (Exception e) {
            logger.error("Failed to retrieve topic statistics for: {} on connection: {}", topicName, connectionId, e);
            return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * Create a new topic.
     */
    @PostMapping("/connections/{connectionId}/topics")
    @Operation(summary = "Create topic", description = "Create a new topic on the specified connection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Topic created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to create topic")
    })
    public CompletableFuture<ResponseEntity<String>> createTopic(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic creation request") @RequestBody CreateTopicRequest request) {

        try {
            logger.info("Creating topic: {} on connection: {}", request.getName(), connectionId);

            return topicService.createTopic(
                connectionId,
                request.getName(),
                request.getPartitions(),
                request.getReplicationFactor().shortValue(),
                request.getConfigs() != null ? request.getConfigs() : new HashMap<>())
                .thenApply(result -> ResponseEntity.status(HttpStatus.CREATED).body("Topic created successfully"))
                .exceptionally(ex -> {
                    logger.error("Failed to create topic: {} on connection: {}", request.getName(), connectionId, ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to create topic: " + ex.getMessage());
                });

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to create topic: {} on connection: {}", request.getName(), connectionId, e);
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create topic: " + e.getMessage()));
        }
    }

    /**
     * Delete a topic.
     */
    @DeleteMapping("/connections/{connectionId}/topics/{topicName}")
    @Operation(summary = "Delete topic", description = "Delete a topic from the specified connection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Topic deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Connection or topic not found"),
        @ApiResponse(responseCode = "500", description = "Failed to delete topic")
    })
    public CompletableFuture<ResponseEntity<String>> deleteTopic(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName) {

        try {
            logger.info("Deleting topic: {} on connection: {}", topicName, connectionId);

            return topicService.deleteTopic(connectionId, topicName)
                .thenApply(result -> ResponseEntity.ok("Topic deleted successfully"))
                .exceptionally(ex -> {
                    logger.error("Failed to delete topic: {} on connection: {}", topicName, connectionId, ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to delete topic: " + ex.getMessage());
                });

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to delete topic: {} on connection: {}", topicName, connectionId, e);
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete topic: " + e.getMessage()));
        }
    }

    /**
     * Update topic configuration.
     */
    @PutMapping("/connections/{connectionId}/topics/{topicName}/config")
    @Operation(summary = "Update topic configuration", description = "Update configuration for an existing topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Topic configuration updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
        @ApiResponse(responseCode = "404", description = "Connection or topic not found"),
        @ApiResponse(responseCode = "500", description = "Failed to update topic configuration")
    })
    public CompletableFuture<ResponseEntity<String>> updateTopicConfig(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName,
            @Parameter(description = "Topic configuration updates") @RequestBody Map<String, String> configs) {

        try {
            logger.info("Updating configuration for topic: {} on connection: {}", topicName, connectionId);

            return topicService.updateTopicConfig(connectionId, topicName, configs)
                .thenApply(result -> ResponseEntity.ok("Topic configuration updated successfully"))
                .exceptionally(ex -> {
                    logger.error("Failed to update configuration for topic: {} on connection: {}", topicName, connectionId, ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to update topic configuration: " + ex.getMessage());
                });

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request: {}", e.getMessage());
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body("Invalid request: " + e.getMessage()));

        } catch (Exception e) {
            logger.error("Failed to update configuration for topic: {} on connection: {}", topicName, connectionId, e);
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update topic configuration: " + e.getMessage()));
        }
    }

    /**
     * Edit a message by sending an updated version.
     */
    @PutMapping("/connections/{connectionId}/topics/{topicName}/messages")
    @Operation(summary = "Edit message", description = "Send an updated version of a message to the topic")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Message updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid message data"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to update message")
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> editMessage(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName,
            @Parameter(description = "Updated message") @RequestBody KafkaMessage updatedMessage) {

        try {
            // Validate that the message has a key (required for edit operations)
            if (updatedMessage.getKey() == null || updatedMessage.getKey().trim().isEmpty()) {
                logger.warn("Cannot edit message without a key");
                return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().build());
            }

            // Set the topic from the path parameter
            updatedMessage.setTopic(topicName);

            // Add metadata headers to indicate this is an updated message
            Map<String, String> headers = updatedMessage.getHeaders();
            if (headers == null) {
                headers = new HashMap<>();
                updatedMessage.setHeaders(headers);
            }
            headers.put("operation", "update");
            headers.put("updated_at", Instant.now().toString());

            logger.info("Editing message with key: {} in topic: {} using connection: {}",
                       updatedMessage.getKey(), topicName, connectionId);

            return producerService.sendMessage(connectionId, updatedMessage)
                .thenApply(result -> {
                    result.put("operation", "edit");
                    result.put("message", "Message updated successfully");
                    return ResponseEntity.ok(result);
                })
                .exceptionally(ex -> {
                    logger.error("Failed to edit message", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });

        } catch (Exception e) {
            logger.error("Failed to edit message", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }

    /**
     * Delete a message by sending a tombstone.
     */
    @DeleteMapping("/connections/{connectionId}/topics/{topicName}/messages")
    @Operation(summary = "Delete message", description = "Send a tombstone message to logically delete a message")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tombstone message sent successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "404", description = "Connection not found"),
        @ApiResponse(responseCode = "500", description = "Failed to delete message")
    })
    public CompletableFuture<ResponseEntity<Map<String, Object>>> deleteMessage(
            @Parameter(description = "Connection ID") @PathVariable Long connectionId,
            @Parameter(description = "Topic name") @PathVariable String topicName,
            @Parameter(description = "Message key to delete") @RequestParam String key,
            @Parameter(description = "Partition (optional)") @RequestParam(required = false) Integer partition) {

        try {
            // Validate that the key is provided
            if (key == null || key.trim().isEmpty()) {
                logger.warn("Cannot delete message without a key");
                return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().build());
            }

            // Create tombstone message (null value)
            KafkaMessage tombstone = new KafkaMessage();
            tombstone.setTopic(topicName);
            tombstone.setKey(key);
            tombstone.setValue(null); // Tombstone
            tombstone.setPartition(partition);

            // Add metadata headers
            Map<String, String> headers = new HashMap<>();
            headers.put("operation", "delete");
            headers.put("deleted_at", Instant.now().toString());
            tombstone.setHeaders(headers);

            logger.info("Deleting message with key: {} in topic: {} using connection: {}",
                       key, topicName, connectionId);

            return producerService.sendMessage(connectionId, tombstone)
                .thenApply(result -> {
                    result.put("operation", "delete");
                    result.put("message", "Tombstone message sent successfully");
                    return ResponseEntity.ok(result);
                })
                .exceptionally(ex -> {
                    logger.error("Failed to delete message", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });

        } catch (Exception e) {
            logger.error("Failed to delete message", e);
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
        }
    }
}
