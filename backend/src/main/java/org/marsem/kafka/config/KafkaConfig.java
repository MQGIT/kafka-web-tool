package org.marsem.kafka.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for the application.
 * 
 * This configuration class sets up Kafka producers, consumers, and admin clients
 * with default settings. Individual connections will override these settings
 * based on their specific configurations.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Configuration
// @EnableKafka - Disabled to prevent startup connection attempts
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:kafka-web-app-v2}")
    private String defaultGroupId;

    @Value("${app.kafka.default-timeout:30s}")
    private String defaultTimeout;

    @Value("${app.kafka.max-consumers:100}")
    private int maxConsumers;

    @Value("${app.kafka.max-producers:50}")
    private int maxProducers;

    @Value("${app.kafka.message-batch-size:1000}")
    private int messageBatchSize;

    /**
     * Default Kafka producer configuration.
     * Used as a template for creating producers with specific connection settings.
     */
    @Bean
    @Lazy
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Performance and reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, 1048576);
        
        // Timeout settings
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        // Idempotence for exactly-once semantics
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Kafka template for sending messages.
     */
    @Bean
    @Lazy
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Default Kafka consumer configuration.
     * Used as a template for creating consumers with specific connection settings.
     */
    @Bean
    @Lazy
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Consumer behavior settings
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        
        // Performance settings
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        configProps.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, messageBatchSize);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        // Session and heartbeat settings
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Kafka listener container factory for @KafkaListener annotations.
     */
    @Bean
    @Lazy
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Container settings
        factory.setConcurrency(3);
        factory.getContainerProperties().setPollTimeout(3000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        
        // Error handling
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler());
        
        return factory;
    }

    /**
     * Default Kafka admin client configuration.
     * Used for topic management and cluster administration.
     * This bean is removed to prevent startup connection attempts.
     * Admin clients are created on-demand by services with specific connection settings.
     */
    // @Bean - Removed to prevent startup connection attempts
    // public KafkaAdmin kafkaAdmin() { ... }

    /**
     * Admin client bean for direct Kafka administration operations.
     * This bean is removed to prevent startup connection attempts.
     * Admin clients are created on-demand by services with specific connection settings.
     */
    // @Bean - Removed to prevent startup connection attempts
    // public AdminClient adminClient() { ... }

    /**
     * Creates a producer factory with custom configuration.
     * Used by services to create producers with specific connection settings.
     */
    public ProducerFactory<String, String> createProducerFactory(Map<String, Object> customConfig) {
        Map<String, Object> configProps = new HashMap<>();
        
        // Start with default configuration
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        
        // Override with custom configuration
        configProps.putAll(customConfig);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Creates a consumer factory with custom configuration.
     * Used by services to create consumers with specific connection settings.
     */
    public ConsumerFactory<String, String> createConsumerFactory(Map<String, Object> customConfig) {
        Map<String, Object> configProps = new HashMap<>();
        
        // Start with default configuration
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        configProps.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, messageBatchSize);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        
        // Override with custom configuration
        configProps.putAll(customConfig);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Creates an admin client with custom configuration.
     * Used by services to create admin clients with specific connection settings.
     */
    public AdminClient createAdminClient(Map<String, Object> customConfig) {
        Map<String, Object> configProps = new HashMap<>();
        
        // Start with default configuration
        configProps.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 60000);
        
        // Override with custom configuration
        configProps.putAll(customConfig);
        
        return AdminClient.create(configProps);
    }

    // Getters for configuration values
    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public String getDefaultGroupId() {
        return defaultGroupId;
    }

    public int getMaxConsumers() {
        return maxConsumers;
    }

    public int getMaxProducers() {
        return maxProducers;
    }

    public int getMessageBatchSize() {
        return messageBatchSize;
    }
}
