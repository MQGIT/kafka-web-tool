package org.marsem.kafka.service;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaException;
import org.marsem.kafka.config.KafkaConfig;
import org.marsem.kafka.model.Connection;
import org.marsem.kafka.repository.ConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing Kafka connections.
 * 
 * This service handles CRUD operations for Kafka connection configurations
 * and provides connection testing functionality.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Service
@Transactional
public class KafkaConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConnectionService.class);

    private final ConnectionRepository connectionRepository;
    private final KafkaConfig kafkaConfig;

    @Autowired
    public KafkaConnectionService(ConnectionRepository connectionRepository, KafkaConfig kafkaConfig) {
        this.connectionRepository = connectionRepository;
        this.kafkaConfig = kafkaConfig;
    }

    /**
     * Get all connections.
     */
    @Transactional(readOnly = true)
    public List<Connection> getAllConnections() {
        return connectionRepository.findAll();
    }

    /**
     * Get all active connections.
     */
    @Transactional(readOnly = true)
    public List<Connection> getActiveConnections() {
        return connectionRepository.findByActiveTrue();
    }

    /**
     * Get connection by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Connection> getConnectionById(Long id) {
        return connectionRepository.findById(id);
    }

    /**
     * Get connection by name.
     */
    @Transactional(readOnly = true)
    public Optional<Connection> getConnectionByName(String name) {
        return connectionRepository.findByName(name);
    }

    /**
     * Create a new connection.
     */
    public Connection createConnection(Connection connection) {
        logger.info("Creating new connection: {}", connection.getName());

        // Validate connection name uniqueness
        if (connectionRepository.existsByNameIgnoreCase(connection.getName())) {
            throw new IllegalArgumentException("Connection name already exists: " + connection.getName());
        }

        // Set creation timestamp and user
        connection.setCreatedAt(LocalDateTime.now());
        connection.setUpdatedAt(LocalDateTime.now());

        // Save the connection
        Connection savedConnection = connectionRepository.save(connection);
        logger.info("Created connection with ID: {}", savedConnection.getId());

        return savedConnection;
    }

    /**
     * Update an existing connection.
     */
    public Connection updateConnection(Long id, Connection connectionUpdate) {
        logger.info("Updating connection with ID: {}", id);

        Connection existingConnection = connectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found with ID: " + id));

        // Validate connection name uniqueness (excluding current connection)
        if (!existingConnection.getName().equals(connectionUpdate.getName()) &&
            connectionRepository.existsByNameIgnoreCaseAndIdNot(connectionUpdate.getName(), id)) {
            throw new IllegalArgumentException("Connection name already exists: " + connectionUpdate.getName());
        }

        // Update fields
        existingConnection.setName(connectionUpdate.getName());
        existingConnection.setDescription(connectionUpdate.getDescription());
        existingConnection.setBootstrapServers(connectionUpdate.getBootstrapServers());
        existingConnection.setSecurityProtocol(connectionUpdate.getSecurityProtocol());
        existingConnection.setSaslMechanism(connectionUpdate.getSaslMechanism());
        existingConnection.setSaslJaasConfig(connectionUpdate.getSaslJaasConfig());
        existingConnection.setUsername(connectionUpdate.getUsername());
        existingConnection.setPassword(connectionUpdate.getPassword());
        existingConnection.setSslTruststoreLocation(connectionUpdate.getSslTruststoreLocation());
        existingConnection.setSslTruststorePassword(connectionUpdate.getSslTruststorePassword());
        existingConnection.setSslKeystoreLocation(connectionUpdate.getSslKeystoreLocation());
        existingConnection.setSslKeystorePassword(connectionUpdate.getSslKeystorePassword());
        existingConnection.setSslKeyPassword(connectionUpdate.getSslKeyPassword());
        existingConnection.setActive(connectionUpdate.getActive());
        existingConnection.setConnectionTimeoutMs(connectionUpdate.getConnectionTimeoutMs());
        existingConnection.setRequestTimeoutMs(connectionUpdate.getRequestTimeoutMs());
        existingConnection.setUpdatedBy(connectionUpdate.getUpdatedBy());

        Connection savedConnection = connectionRepository.save(existingConnection);
        logger.info("Updated connection: {}", savedConnection.getName());

        return savedConnection;
    }

    /**
     * Delete a connection.
     */
    public void deleteConnection(Long id) {
        logger.info("Deleting connection with ID: {}", id);

        Connection connection = connectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found with ID: " + id));

        connectionRepository.delete(connection);
        logger.info("Deleted connection: {}", connection.getName());
    }

    /**
     * Test a connection to verify it can connect to Kafka.
     */
    public Map<String, Object> testConnection(Connection connection) {
        logger.info("Testing connection: {}", connection.getName());

        Map<String, Object> result = new HashMap<>();
        result.put("connectionName", connection.getName());
        result.put("testStartTime", LocalDateTime.now());

        try {
            // Create admin client configuration
            Map<String, Object> adminConfig = createAdminClientConfig(connection);
            
            // Create admin client and test connection
            try (AdminClient adminClient = AdminClient.create(adminConfig)) {
                DescribeClusterResult clusterResult = adminClient.describeCluster();
                
                // Try to get cluster information with timeout
                String clusterId = clusterResult.clusterId().get(30, TimeUnit.SECONDS);
                int nodeCount = clusterResult.nodes().get(30, TimeUnit.SECONDS).size();
                
                result.put("success", true);
                result.put("clusterId", clusterId);
                result.put("nodeCount", nodeCount);
                result.put("message", "Connection successful");
                
                logger.info("Connection test successful for: {}", connection.getName());
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getClass().getSimpleName());
            result.put("message", e.getMessage());
            
            logger.warn("Connection test failed for: {} - {}", connection.getName(), e.getMessage());
        }

        result.put("testEndTime", LocalDateTime.now());
        return result;
    }

    /**
     * Get connection statistics.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getConnectionStatistics() {
        List<Object> stats = connectionRepository.getConnectionStatistics();
        
        if (!stats.isEmpty() && stats.get(0) instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) stats.get(0);
            return result;
        }
        
        // Return default statistics if no data
        Map<String, Object> defaultStats = new HashMap<>();
        defaultStats.put("total", 0L);
        defaultStats.put("active", 0L);
        defaultStats.put("inactive", 0L);
        defaultStats.put("securityProtocols", 0L);
        return defaultStats;
    }

    /**
     * Find connections by criteria.
     */
    @Transactional(readOnly = true)
    public List<Connection> findConnectionsByCriteria(Boolean active, String securityProtocol, String createdBy) {
        return connectionRepository.findByCriteria(active, securityProtocol, createdBy);
    }

    /**
     * Create admin client configuration from connection.
     */
    private Map<String, Object> createAdminClientConfig(Connection connection) {
        Map<String, Object> config = new HashMap<>();
        
        // Basic configuration
        config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, connection.getBootstrapServers());
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 
                   connection.getRequestTimeoutMs() != null ? connection.getRequestTimeoutMs() : 30000);
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 60000);
        
        // Security configuration
        if (connection.getSecurityProtocol() != null) {
            config.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, connection.getSecurityProtocol().getProtocol());
        }
        
        // SASL configuration
        if (connection.getSaslMechanism() != null) {
            config.put("sasl.mechanism", connection.getSaslMechanism().getMechanism());
            
            if (connection.getSaslJaasConfig() != null) {
                config.put("sasl.jaas.config", connection.getSaslJaasConfig());
            } else if (connection.getUsername() != null && connection.getPassword() != null) {
                // Build JAAS config for PLAIN mechanism
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
     * Create an admin client for a specific connection.
     */
    public AdminClient createAdminClient(Connection connection) {
        Map<String, Object> config = createAdminClientConfig(connection);
        return AdminClient.create(config);
    }
}
