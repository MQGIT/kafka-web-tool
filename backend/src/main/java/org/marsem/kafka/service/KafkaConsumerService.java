package org.marsem.kafka.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.marsem.kafka.config.KafkaConfig;
import org.marsem.kafka.model.Connection;
import org.marsem.kafka.model.ConsumerMessage;
import org.marsem.kafka.model.ConsumerSession;
import org.marsem.kafka.model.ConsumerStatus;
import org.marsem.kafka.model.KafkaMessage;
import org.marsem.kafka.repository.ConsumerMessageRepository;
import org.marsem.kafka.repository.ConsumerSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for consuming messages from Kafka topics.
 * 
 * This service handles consumer session management, real-time message consumption,
 * and WebSocket streaming for live message delivery to the frontend.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Service
@Transactional
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final KafkaConfig kafkaConfig;
    private final KafkaConnectionService connectionService;
    private final ConsumerSessionRepository consumerSessionRepository;
    private final ConsumerMessageRepository consumerMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    
    // Active consumer sessions
    private final Map<String, ConsumerSessionContext> activeConsumers = new ConcurrentHashMap<>();

    // Store consumed messages for each session
    private final Map<String, List<KafkaMessage>> sessionMessages = new ConcurrentHashMap<>();

    @Autowired
    public KafkaConsumerService(
            KafkaConfig kafkaConfig,
            KafkaConnectionService connectionService,
            ConsumerSessionRepository consumerSessionRepository,
            ConsumerMessageRepository consumerMessageRepository,
            SimpMessagingTemplate messagingTemplate) {
        this.kafkaConfig = kafkaConfig;
        this.connectionService = connectionService;
        this.consumerSessionRepository = consumerSessionRepository;
        this.consumerMessageRepository = consumerMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Create a new consumer session.
     */
    public ConsumerSession createConsumerSession(ConsumerSession session) {
        logger.info("Creating consumer session: {} for topic: {}", session.getSessionId(), session.getTopic());

        // Validate connection exists
        connectionService.getConnectionById(session.getConnectionId())
                .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + session.getConnectionId()));

        // Generate session ID if not provided
        if (session.getSessionId() == null || session.getSessionId().isEmpty()) {
            session.setSessionId(UUID.randomUUID().toString());
        }

        // Set initial status and timestamps
        session.setStatus(ConsumerStatus.CREATED);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        ConsumerSession savedSession = consumerSessionRepository.save(session);
        logger.info("Created consumer session with ID: {}", savedSession.getId());

        return savedSession;
    }

    /**
     * Start consuming messages for a session.
     */
    @Async("kafkaTaskExecutor")
    public CompletableFuture<Map<String, Object>> startConsumer(String sessionId) {
        logger.info("Starting consumer session: {}", sessionId);

        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = new HashMap<>();
            
            try {
                ConsumerSession session = consumerSessionRepository.findBySessionId(sessionId)
                        .orElseThrow(() -> new IllegalArgumentException("Consumer session not found: " + sessionId));

                if (session.isActive()) {
                    result.put("success", false);
                    result.put("message", "Consumer session is already running");
                    return result;
                }

                Connection connection = connectionService.getConnectionById(session.getConnectionId())
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + session.getConnectionId()));

                // Create consumer
                KafkaConsumer<String, String> consumer = createConsumer(connection, session);
                
                // Create consumer context
                ConsumerSessionContext context = new ConsumerSessionContext(session, consumer);
                activeConsumers.put(sessionId, context);

                // Update session status
                session.markAsStarted();
                consumerSessionRepository.save(session);

                // Start consuming in background
                startConsumerLoop(context);

                result.put("success", true);
                result.put("sessionId", sessionId);
                result.put("message", "Consumer started successfully");
                
                logger.info("Consumer session started: {}", sessionId);

            } catch (Exception e) {
                logger.error("Failed to start consumer session: {}", sessionId, e);
                result.put("success", false);
                result.put("error", e.getMessage());
            }

            return result;
        });
    }

    /**
     * Stop a consumer session.
     */
    public Map<String, Object> stopConsumer(String sessionId) {
        logger.info("Stopping consumer session: {}", sessionId);

        Map<String, Object> result = new HashMap<>();

        try {
            ConsumerSessionContext context = activeConsumers.remove(sessionId);
            
            if (context != null) {
                context.stop();
                
                // Update session status
                ConsumerSession session = context.getSession();
                session.markAsStopped();
                consumerSessionRepository.save(session);
                
                result.put("success", true);
                result.put("message", "Consumer stopped successfully");
                
                logger.info("Consumer session stopped: {}", sessionId);
            } else {
                result.put("success", false);
                result.put("message", "Consumer session not found or not running");
            }

        } catch (Exception e) {
            logger.error("Failed to stop consumer session: {}", sessionId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Pause a consumer session.
     */
    public Map<String, Object> pauseConsumer(String sessionId) {
        logger.info("Pausing consumer session: {}", sessionId);

        Map<String, Object> result = new HashMap<>();

        try {
            ConsumerSessionContext context = activeConsumers.get(sessionId);
            
            if (context != null && context.isRunning()) {
                context.pause();
                
                // Update session status
                ConsumerSession session = context.getSession();
                session.setStatus(ConsumerStatus.PAUSED);
                consumerSessionRepository.save(session);
                
                result.put("success", true);
                result.put("message", "Consumer paused successfully");
                
                logger.info("Consumer session paused: {}", sessionId);
            } else {
                result.put("success", false);
                result.put("message", "Consumer session not found or not running");
            }

        } catch (Exception e) {
            logger.error("Failed to pause consumer session: {}", sessionId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Resume a paused consumer session.
     */
    public Map<String, Object> resumeConsumer(String sessionId) {
        logger.info("Resuming consumer session: {}", sessionId);

        Map<String, Object> result = new HashMap<>();

        try {
            ConsumerSessionContext context = activeConsumers.get(sessionId);
            
            if (context != null && context.isPaused()) {
                context.resume();
                
                // Update session status
                ConsumerSession session = context.getSession();
                session.setStatus(ConsumerStatus.RUNNING);
                consumerSessionRepository.save(session);
                
                result.put("success", true);
                result.put("message", "Consumer resumed successfully");
                
                logger.info("Consumer session resumed: {}", sessionId);
            } else {
                result.put("success", false);
                result.put("message", "Consumer session not found or not paused");
            }

        } catch (Exception e) {
            logger.error("Failed to resume consumer session: {}", sessionId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        }

        return result;
    }

    /**
     * Get consumer session status.
     */
    @Transactional(readOnly = true)
    public Optional<ConsumerSession> getConsumerSession(String sessionId) {
        return consumerSessionRepository.findBySessionId(sessionId);
    }

    /**
     * Get all consumer sessions.
     */
    @Transactional(readOnly = true)
    public List<ConsumerSession> getAllConsumerSessions() {
        return consumerSessionRepository.findAll();
    }

    /**
     * Get active consumer sessions.
     */
    @Transactional(readOnly = true)
    public List<ConsumerSession> getActiveConsumerSessions() {
        return consumerSessionRepository.findByStatus(ConsumerStatus.RUNNING);
    }

    /**
     * Get consumer sessions by connection.
     */
    @Transactional(readOnly = true)
    public List<ConsumerSession> getConsumerSessionsByConnection(Long connectionId) {
        return consumerSessionRepository.findByConnectionId(connectionId);
    }

    /**
     * Delete a consumer session.
     */
    public void deleteConsumerSession(String sessionId) {
        logger.info("Deleting consumer session: {}", sessionId);

        // Stop consumer if running
        stopConsumer(sessionId);

        // Delete from database
        consumerSessionRepository.findBySessionId(sessionId)
                .ifPresent(consumerSessionRepository::delete);
    }

    /**
     * Stop all active consumers.
     */
    public void stopAllConsumers() {
        logger.info("Stopping all active consumers");

        activeConsumers.keySet().forEach(this::stopConsumer);
    }

    /**
     * Get all running consumer sessions with details.
     */
    public List<Map<String, Object>> getRunningConsumerSessions() {
        logger.info("Getting running consumer sessions");

        List<Map<String, Object>> runningConsumers = new ArrayList<>();

        for (Map.Entry<String, ConsumerSessionContext> entry : activeConsumers.entrySet()) {
            String sessionId = entry.getKey();
            ConsumerSessionContext context = entry.getValue();
            ConsumerSession session = context.getSession();

            Map<String, Object> consumerInfo = new HashMap<>();
            consumerInfo.put("sessionId", sessionId);
            consumerInfo.put("topic", session.getTopic());
            consumerInfo.put("consumerGroup", session.getConsumerGroup());
            consumerInfo.put("connectionId", session.getConnectionId());
            consumerInfo.put("connectionName", session.getConnection() != null ? session.getConnection().getName() : "Unknown");
            consumerInfo.put("status", session.getStatus().toString());
            consumerInfo.put("messagesConsumed", session.getMessagesConsumed());
            consumerInfo.put("maxMessages", session.getMaxMessages());
            consumerInfo.put("startedAt", session.getStartedAt());
            consumerInfo.put("isRunning", context.isRunning());
            consumerInfo.put("isPaused", context.isPaused());

            runningConsumers.add(consumerInfo);
        }

        logger.info("Found {} running consumer sessions", runningConsumers.size());
        return runningConsumers;
    }

    /**
     * Create a Kafka consumer for the session.
     */
    private KafkaConsumer<String, String> createConsumer(Connection connection, ConsumerSession session) {
        Map<String, Object> config = createConsumerConfig(connection, session);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config);

        // Subscribe to topic or assign specific partition
        if (session.getPartitionId() != null) {
            TopicPartition partition = new TopicPartition(session.getTopic(), session.getPartitionId());
            consumer.assign(Collections.singletonList(partition));
            
            // Seek to specific offset if provided
            if (session.getStartOffset() != null) {
                consumer.seek(partition, session.getStartOffset());
            }
        } else {
            consumer.subscribe(Collections.singletonList(session.getTopic()));
        }

        return consumer;
    }

    /**
     * Create consumer configuration.
     */
    private Map<String, Object> createConsumerConfig(Connection connection, ConsumerSession session) {
        Map<String, Object> config = new HashMap<>();
        
        // Basic configuration
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connection.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, session.getConsumerGroup());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // Consumer behavior
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, session.getAutoCommit());
        config.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        
        // Performance settings
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, session.getPollTimeoutMs());
        config.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);
        
        // Session settings
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        
        // Security configuration (same as producer)
        if (connection.getSecurityProtocol() != null) {
            config.put("security.protocol", connection.getSecurityProtocol().getProtocol());
        }
        
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
     * Start the consumer loop for a session.
     */
    @Async("kafkaTaskExecutor")
    private void startConsumerLoop(ConsumerSessionContext context) {
        ConsumerSession session = context.getSession();
        KafkaConsumer<String, String> consumer = context.getConsumer();
        String sessionId = session.getSessionId();
        
        logger.info("Starting consumer loop for session: {}", sessionId);

        // Timeout mechanism: stop if no messages for 30 seconds
        long lastMessageTime = System.currentTimeMillis();
        final long CONSUMER_TIMEOUT_MS = 30000; // 30 seconds

        try {
            while (context.isRunning()) {
                if (context.isPaused()) {
                    Thread.sleep(1000);
                    continue;
                }

                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(session.getPollTimeoutMs()));

                // Check for timeout if no messages received
                if (records.isEmpty()) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastMessageTime > CONSUMER_TIMEOUT_MS) {
                        logger.info("Consumer timeout reached for session: {} (no messages for {}ms)",
                            sessionId, CONSUMER_TIMEOUT_MS);
                        session.markAsStopped();
                        consumerSessionRepository.save(session);
                        context.stop();
                        break;
                    }
                } else {
                    // Reset timeout when messages are received
                    lastMessageTime = System.currentTimeMillis();
                }

                for (ConsumerRecord<String, String> record : records) {
                    if (!context.isRunning()) {
                        break;
                    }

                    // Convert to KafkaMessage
                    KafkaMessage message = convertToKafkaMessage(record);

                    // Store message for session
                    storeMessageForSession(sessionId, message);

                    // Send via WebSocket
                    sendMessageViaWebSocket(sessionId, message);

                    // Update session statistics
                    session.incrementMessagesConsumed();
                    session.setCurrentOffset(record.offset());
                    
                    // Check if max messages reached
                    if (session.getMaxMessages() != null &&
                        session.getMessagesConsumed() >= session.getMaxMessages()) {
                        logger.info("Max messages reached for session: {}", sessionId);
                        session.markAsStopped();
                        consumerSessionRepository.save(session);
                        context.stop();
                        break;
                    }
                }

                // Periodically update session in database
                if (session.getMessagesConsumed() % 100 == 0) {
                    consumerSessionRepository.save(session);
                }
            }

        } catch (Exception e) {
            logger.error("Error in consumer loop for session: {}", sessionId, e);
            
            session.markAsError(e.getMessage());
            consumerSessionRepository.save(session);
            
            // Send error via WebSocket
            Map<String, Object> errorMessage = new HashMap<>();
            errorMessage.put("type", "error");
            errorMessage.put("sessionId", sessionId);
            errorMessage.put("error", e.getMessage());
            
            sendErrorViaWebSocket(sessionId, errorMessage);
            
        } finally {
            try {
                consumer.close();
                activeConsumers.remove(sessionId);
                logger.info("Consumer loop ended for session: {}", sessionId);
            } catch (Exception e) {
                logger.warn("Error closing consumer for session: {}", sessionId, e);
            }
        }
    }

    /**
     * Convert ConsumerRecord to KafkaMessage.
     */
    private KafkaMessage convertToKafkaMessage(ConsumerRecord<String, String> record) {
        KafkaMessage message = new KafkaMessage();
        message.setTopic(record.topic());
        message.setPartition(record.partition());
        message.setOffset(record.offset());
        message.setKey(record.key());
        message.setValue(record.value());
        message.setTimestamp(Instant.ofEpochMilli(record.timestamp()));
        message.setSerializedKeySize(record.serializedKeySize());
        message.setSerializedValueSize(record.serializedValueSize());
        message.setLeaderEpoch(record.leaderEpoch().orElse(null));
        
        // Convert headers
        if (record.headers() != null) {
            Map<String, String> headers = new HashMap<>();
            record.headers().forEach(header -> 
                headers.put(header.key(), new String(header.value()))
            );
            message.setHeaders(headers);
        }
        
        return message;
    }

    /**
     * Send message via WebSocket.
     */
    private void sendMessageViaWebSocket(String sessionId, KafkaMessage message) {
        try {
            messagingTemplate.convertAndSend("/topic/consumer/" + sessionId, message);
        } catch (Exception e) {
            logger.warn("Failed to send message via WebSocket for session: {}", sessionId, e);
        }
    }

    /**
     * Send error via WebSocket.
     */
    private void sendErrorViaWebSocket(String sessionId, Map<String, Object> errorMessage) {
        try {
            messagingTemplate.convertAndSend("/topic/consumer/" + sessionId + "/error", errorMessage);
        } catch (Exception e) {
            logger.warn("Failed to send error via WebSocket for session: {}", sessionId, e);
        }
    }

    /**
     * Consumer session context for managing active consumers.
     */
    private static class ConsumerSessionContext {
        private final ConsumerSession session;
        private final KafkaConsumer<String, String> consumer;
        private final AtomicBoolean running = new AtomicBoolean(true);
        private final AtomicBoolean paused = new AtomicBoolean(false);

        public ConsumerSessionContext(ConsumerSession session, KafkaConsumer<String, String> consumer) {
            this.session = session;
            this.consumer = consumer;
        }

        public ConsumerSession getSession() { return session; }
        public KafkaConsumer<String, String> getConsumer() { return consumer; }
        public boolean isRunning() { return running.get(); }
        public boolean isPaused() { return paused.get(); }
        
        public void stop() { 
            running.set(false); 
            paused.set(false);
        }
        
        public void pause() { paused.set(true); }
        public void resume() { paused.set(false); }
    }

    /**
     * Store consumed message for a session in the database.
     */
    @Transactional
    private void storeMessageForSession(String sessionId, KafkaMessage message) {
        try {
            // Check if message already exists to prevent duplicates
            boolean exists = consumerMessageRepository.existsBySessionIdAndTopicAndPartitionAndOffset(
                sessionId, message.getTopic(), message.getPartition(), message.getOffset());

            if (!exists) {
                ConsumerMessage consumerMessage = new ConsumerMessage(sessionId, message);
                consumerMessageRepository.save(consumerMessage);
                logger.debug("Stored message for session {}: topic={}, partition={}, offset={}",
                    sessionId, message.getTopic(), message.getPartition(), message.getOffset());
            } else {
                logger.debug("Message already exists for session {}: topic={}, partition={}, offset={}",
                    sessionId, message.getTopic(), message.getPartition(), message.getOffset());
            }
        } catch (Exception e) {
            logger.error("Failed to store message for session {}: {}", sessionId, e.getMessage(), e);
            // Fall back to in-memory storage if database fails
            sessionMessages.computeIfAbsent(sessionId, k -> new ArrayList<>()).add(message);
        }
    }

    /**
     * Get consumed messages for a session from the database.
     */
    public List<KafkaMessage> getConsumedMessages(String sessionId) {
        try {
            List<ConsumerMessage> consumerMessages = consumerMessageRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId);

            List<KafkaMessage> kafkaMessages = consumerMessages.stream()
                .map(ConsumerMessage::toKafkaMessage)
                .collect(Collectors.toList());

            logger.debug("Retrieved {} messages for session {} from database", kafkaMessages.size(), sessionId);
            return kafkaMessages;

        } catch (Exception e) {
            logger.error("Failed to retrieve messages for session {} from database: {}", sessionId, e.getMessage(), e);
            // Fall back to in-memory storage if database fails
            return sessionMessages.getOrDefault(sessionId, new ArrayList<>());
        }
    }

    /**
     * Clear consumed messages for a session from both database and memory.
     */
    public void clearSessionMessages(String sessionId) {
        try {
            int deletedCount = consumerMessageRepository.deleteBySessionId(sessionId);
            logger.info("Deleted {} messages for session {} from database", deletedCount, sessionId);
        } catch (Exception e) {
            logger.error("Failed to delete messages for session {} from database: {}", sessionId, e.getMessage(), e);
        }

        // Also clear from in-memory storage (fallback)
        sessionMessages.remove(sessionId);
    }
}
