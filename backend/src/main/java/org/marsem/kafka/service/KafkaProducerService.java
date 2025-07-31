package org.marsem.kafka.service;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringSerializer;
import org.marsem.kafka.config.KafkaConfig;
import org.marsem.kafka.model.Connection;
import org.marsem.kafka.model.KafkaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Service for producing messages to Kafka topics.
 * 
 * This service handles message production with support for different
 * connection configurations, headers, and batch operations.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    private final KafkaConfig kafkaConfig;
    private final KafkaConnectionService connectionService;
    
    // Cache of producers by connection ID
    private final Map<Long, KafkaProducer<String, String>> producerCache = new ConcurrentHashMap<>();

    @Autowired
    public KafkaProducerService(KafkaConfig kafkaConfig, KafkaConnectionService connectionService) {
        this.kafkaConfig = kafkaConfig;
        this.connectionService = connectionService;
    }

    /**
     * Send a single message to a Kafka topic.
     */
    public CompletableFuture<Map<String, Object>> sendMessage(Long connectionId, KafkaMessage message) {
        logger.info("Sending message to topic: {} using connection: {}", message.getTopic(), connectionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                KafkaProducer<String, String> producer = getOrCreateProducer(connection);
                
                // Create producer record
                ProducerRecord<String, String> record = createProducerRecord(message);
                
                // Send message synchronously for immediate feedback
                Future<RecordMetadata> future = producer.send(record);
                RecordMetadata metadata = future.get();
                
                // Create result
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("topic", metadata.topic());
                result.put("partition", metadata.partition());
                result.put("offset", metadata.offset());
                result.put("timestamp", Instant.ofEpochMilli(metadata.timestamp()));
                result.put("serializedKeySize", metadata.serializedKeySize());
                result.put("serializedValueSize", metadata.serializedValueSize());
                
                logger.info("Message sent successfully to {}:{} at offset {}", 
                           metadata.topic(), metadata.partition(), metadata.offset());
                
                return result;
                
            } catch (Exception e) {
                logger.error("Failed to send message to topic: {}", message.getTopic(), e);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getClass().getSimpleName());
                result.put("message", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Send multiple messages to Kafka topics in batch.
     */
    @Async("kafkaTaskExecutor")
    public CompletableFuture<Map<String, Object>> sendMessagesBatch(Long connectionId, List<KafkaMessage> messages) {
        logger.info("Sending batch of {} messages using connection: {}", messages.size(), connectionId);

        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = new HashMap<>();
            result.put("totalMessages", messages.size());
            result.put("successCount", 0);
            result.put("failureCount", 0);
            result.put("results", new HashMap<String, Object>());

            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                KafkaProducer<String, String> producer = getOrCreateProducer(connection);
                
                int successCount = 0;
                int failureCount = 0;
                Map<String, Object> messageResults = new HashMap<>();
                
                for (int i = 0; i < messages.size(); i++) {
                    KafkaMessage message = messages.get(i);
                    String messageKey = "message_" + i;
                    
                    try {
                        ProducerRecord<String, String> record = createProducerRecord(message);
                        Future<RecordMetadata> future = producer.send(record);
                        RecordMetadata metadata = future.get();
                        
                        Map<String, Object> messageResult = new HashMap<>();
                        messageResult.put("success", true);
                        messageResult.put("partition", metadata.partition());
                        messageResult.put("offset", metadata.offset());
                        messageResult.put("timestamp", Instant.ofEpochMilli(metadata.timestamp()));
                        
                        messageResults.put(messageKey, messageResult);
                        successCount++;
                        
                    } catch (Exception e) {
                        Map<String, Object> messageResult = new HashMap<>();
                        messageResult.put("success", false);
                        messageResult.put("error", e.getMessage());
                        
                        messageResults.put(messageKey, messageResult);
                        failureCount++;
                        
                        logger.warn("Failed to send message {} in batch: {}", i, e.getMessage());
                    }
                }
                
                result.put("successCount", successCount);
                result.put("failureCount", failureCount);
                result.put("results", messageResults);
                result.put("success", failureCount == 0);
                
                logger.info("Batch send completed: {} success, {} failures", successCount, failureCount);
                
            } catch (Exception e) {
                logger.error("Failed to send message batch", e);
                result.put("success", false);
                result.put("error", e.getMessage());
                result.put("failureCount", messages.size());
            }
            
            return result;
        });
    }

    /**
     * Send a message asynchronously with callback.
     */
    @Async("kafkaTaskExecutor")
    public CompletableFuture<Map<String, Object>> sendMessageAsync(Long connectionId, KafkaMessage message) {
        logger.info("Sending message asynchronously to topic: {} using connection: {}", message.getTopic(), connectionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                KafkaProducer<String, String> producer = getOrCreateProducer(connection);
                ProducerRecord<String, String> record = createProducerRecord(message);
                
                CompletableFuture<Map<String, Object>> resultFuture = new CompletableFuture<>();
                
                producer.send(record, (metadata, exception) -> {
                    Map<String, Object> result = new HashMap<>();
                    
                    if (exception == null) {
                        result.put("success", true);
                        result.put("topic", metadata.topic());
                        result.put("partition", metadata.partition());
                        result.put("offset", metadata.offset());
                        result.put("timestamp", Instant.ofEpochMilli(metadata.timestamp()));
                        
                        logger.debug("Async message sent to {}:{} at offset {}", 
                                   metadata.topic(), metadata.partition(), metadata.offset());
                    } else {
                        result.put("success", false);
                        result.put("error", exception.getClass().getSimpleName());
                        result.put("message", exception.getMessage());
                        
                        logger.warn("Async message send failed: {}", exception.getMessage());
                    }
                    
                    resultFuture.complete(result);
                });
                
                return resultFuture.get();
                
            } catch (Exception e) {
                logger.error("Failed to send async message to topic: {}", message.getTopic(), e);
                
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("error", e.getClass().getSimpleName());
                result.put("message", e.getMessage());
                return result;
            }
        });
    }

    /**
     * Get producer metrics for a connection.
     */
    public Map<String, Object> getProducerMetrics(Long connectionId) {
        KafkaProducer<String, String> producer = producerCache.get(connectionId);
        
        if (producer == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("error", "No active producer for connection: " + connectionId);
            return result;
        }
        
        Map<String, Object> metrics = new HashMap<>();
        producer.metrics().forEach((metricName, metric) -> {
            metrics.put(metricName.name(), metric.metricValue());
        });
        
        return metrics;
    }

    /**
     * Close producer for a specific connection.
     */
    public void closeProducer(Long connectionId) {
        KafkaProducer<String, String> producer = producerCache.remove(connectionId);
        if (producer != null) {
            try {
                producer.close();
                logger.info("Closed producer for connection: {}", connectionId);
            } catch (Exception e) {
                logger.warn("Error closing producer for connection {}: {}", connectionId, e.getMessage());
            }
        }
    }

    /**
     * Close all producers.
     */
    public void closeAllProducers() {
        logger.info("Closing all producers");
        
        producerCache.forEach((connectionId, producer) -> {
            try {
                producer.close();
            } catch (Exception e) {
                logger.warn("Error closing producer for connection {}: {}", connectionId, e.getMessage());
            }
        });
        
        producerCache.clear();
    }

    /**
     * Get or create a producer for the given connection.
     */
    private KafkaProducer<String, String> getOrCreateProducer(Connection connection) {
        return producerCache.computeIfAbsent(connection.getId(), id -> {
            Map<String, Object> config = createProducerConfig(connection);
            KafkaProducer<String, String> producer = new KafkaProducer<>(config);
            logger.info("Created new producer for connection: {}", connection.getName());
            return producer;
        });
    }

    /**
     * Create producer configuration from connection.
     */
    private Map<String, Object> createProducerConfig(Connection connection) {
        Map<String, Object> config = new HashMap<>();
        
        // Basic configuration
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connection.getBootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Performance and reliability settings
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Timeout settings
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 
                   connection.getRequestTimeoutMs() != null ? connection.getRequestTimeoutMs() : 30000);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        // Security configuration
        if (connection.getSecurityProtocol() != null) {
            config.put("security.protocol", connection.getSecurityProtocol().getProtocol());
        }
        
        // SASL configuration
        if (connection.getSaslMechanism() != null) {
            config.put("sasl.mechanism", connection.getSaslMechanism().getMechanism());
            
            if (connection.getSaslJaasConfig() != null) {
                config.put("sasl.jaas.config", connection.getSaslJaasConfig());
            } else if (connection.getUsername() != null && connection.getPassword() != null) {
                String jaasConfig = String.format(
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    connection.getUsername(), connection.getPassword()
                );
                config.put("sasl.jaas.config", jaasConfig);
            }
        }
        
        // SSL configuration
        if (connection.getSslTruststoreLocation() != null) {
            config.put("ssl.truststore.location", connection.getSslTruststoreLocation());
        }
        if (connection.getSslTruststorePassword() != null) {
            config.put("ssl.truststore.password", connection.getSslTruststorePassword());
        }
        if (connection.getSslKeystoreLocation() != null) {
            config.put("ssl.keystore.location", connection.getSslKeystoreLocation());
        }
        if (connection.getSslKeystorePassword() != null) {
            config.put("ssl.keystore.password", connection.getSslKeystorePassword());
        }
        if (connection.getSslKeyPassword() != null) {
            config.put("ssl.key.password", connection.getSslKeyPassword());
        }
        
        return config;
    }

    /**
     * Create a producer record from a Kafka message.
     */
    private ProducerRecord<String, String> createProducerRecord(KafkaMessage message) {
        ProducerRecord<String, String> record;
        
        if (message.getPartition() != null) {
            record = new ProducerRecord<>(message.getTopic(), message.getPartition(), 
                                        message.getKey(), message.getValue());
        } else {
            record = new ProducerRecord<>(message.getTopic(), message.getKey(), message.getValue());
        }
        
        // Add headers if present
        if (message.getHeaders() != null && !message.getHeaders().isEmpty()) {
            message.getHeaders().forEach((key, value) -> {
                Header header = new RecordHeader(key, value.getBytes());
                record.headers().add(header);
            });
        }
        
        return record;
    }
}
