package org.marsem.kafka.service;

import org.marsem.kafka.model.Connection;
import org.marsem.kafka.repository.ConnectionRepository;
import org.marsem.kafka.repository.ConsumerSessionRepository;
import org.marsem.kafka.model.ConsumerSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for dashboard metrics and statistics.
 * 
 * Provides methods to collect and aggregate metrics from various sources
 * including Kafka clusters, application health, and system performance.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final ConnectionRepository connectionRepository;
    private final ConsumerSessionRepository consumerSessionRepository;
    private final TopicManagementService topicService;

    @Autowired
    public DashboardService(ConnectionRepository connectionRepository,
                           ConsumerSessionRepository consumerSessionRepository,
                           TopicManagementService topicService) {
        this.connectionRepository = connectionRepository;
        this.consumerSessionRepository = consumerSessionRepository;
        this.topicService = topicService;
    }

    /**
     * Get overall dashboard metrics.
     */
    public CompletableFuture<Map<String, Object>> getDashboardMetrics() {
        logger.info("Collecting dashboard metrics");

        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, Object> metrics = new HashMap<>();
                
                // Connection metrics
                List<Connection> connections = connectionRepository.findByActiveTrue();
                metrics.put("totalConnections", connections.size());
                metrics.put("activeConnections", connections.size());
                
                // Consumer session metrics
                long totalSessions = consumerSessionRepository.count();
                long activeSessions = consumerSessionRepository.countByStatus(org.marsem.kafka.model.ConsumerStatus.RUNNING) +
                                    consumerSessionRepository.countByStatus(org.marsem.kafka.model.ConsumerStatus.CREATED);
                
                metrics.put("totalConsumerSessions", totalSessions);
                metrics.put("activeConsumerSessions", activeSessions);
                
                // Topic metrics (aggregate from all connections)
                int totalTopics = 0;
                long totalMessages = 0;
                
                for (Connection connection : connections) {
                    try {
                        List<org.marsem.kafka.model.TopicInfo> topics = topicService.getTopics(connection.getId()).get();
                        totalTopics += topics.size();
                        
                        // Get message count for each topic (sample a few)
                        int sampleCount = Math.min(5, topics.size());
                        for (int i = 0; i < sampleCount; i++) {
                            try {
                                Map<String, Object> stats = topicService.getTopicStats(connection.getId(), topics.get(i).getName()).get();
                                totalMessages += (Long) stats.getOrDefault("totalMessages", 0L);
                            } catch (Exception e) {
                                logger.debug("Failed to get stats for topic: {}", topics.get(i).getName());
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to get topics for connection: {}", connection.getId());
                    }
                }
                
                metrics.put("totalTopics", totalTopics);
                metrics.put("totalMessages", totalMessages);
                
                // System metrics
                metrics.put("systemMetrics", getSystemMetrics());
                
                // Timestamp
                metrics.put("timestamp", Instant.now());
                
                logger.info("Collected dashboard metrics: {} connections, {} topics, {} sessions", 
                           connections.size(), totalTopics, totalSessions);
                
                return metrics;
                
            } catch (Exception e) {
                logger.error("Failed to collect dashboard metrics", e);
                throw new RuntimeException("Failed to collect dashboard metrics: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get metrics for a specific connection.
     */
    public CompletableFuture<Map<String, Object>> getConnectionMetrics(Long connectionId) {
        logger.info("Collecting metrics for connection: {}", connectionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = connectionRepository.findById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                Map<String, Object> metrics = new HashMap<>();
                metrics.put("connectionId", connectionId);
                metrics.put("connectionName", connection.getName());
                metrics.put("connectionStatus", connection.getActive() ? "ACTIVE" : "INACTIVE");

                // Get topics for this connection
                List<org.marsem.kafka.model.TopicInfo> topics = topicService.getTopics(connectionId).get();
                metrics.put("topicCount", topics.size());

                // Get consumer sessions for this connection
                long sessionCount = consumerSessionRepository.findByConnectionId(connectionId).size();
                metrics.put("consumerSessions", sessionCount);
                
                // Sample topic statistics
                long totalMessages = 0;
                int partitionCount = 0;
                
                int sampleSize = Math.min(10, topics.size());
                for (int i = 0; i < sampleSize; i++) {
                    try {
                        org.marsem.kafka.model.TopicInfo topic = topics.get(i);
                        partitionCount += topic.getPartitionCount();
                        
                        Map<String, Object> stats = topicService.getTopicStats(connectionId, topic.getName()).get();
                        totalMessages += (Long) stats.getOrDefault("totalMessages", 0L);
                    } catch (Exception e) {
                        logger.debug("Failed to get stats for topic: {}", topics.get(i).getName());
                    }
                }
                
                metrics.put("totalMessages", totalMessages);
                metrics.put("totalPartitions", partitionCount);
                metrics.put("timestamp", Instant.now());
                
                logger.info("Collected metrics for connection {}: {} topics, {} messages", 
                           connectionId, topics.size(), totalMessages);
                
                return metrics;
                
            } catch (Exception e) {
                logger.error("Failed to collect metrics for connection: {}", connectionId, e);
                throw new RuntimeException("Failed to collect connection metrics: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get application health metrics.
     */
    public Map<String, Object> getHealthMetrics() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // JVM metrics
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            
            health.put("uptime", runtimeBean.getUptime());
            health.put("startTime", runtimeBean.getStartTime());
            
            // Memory metrics
            Map<String, Object> memory = new HashMap<>();
            memory.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
            memory.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
            memory.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
            memory.put("nonHeapMax", memoryBean.getNonHeapMemoryUsage().getMax());
            health.put("memory", memory);
            
            // Application metrics
            health.put("activeConnections", connectionRepository.findByActiveTrue().size());
            health.put("activeSessions",
                consumerSessionRepository.countByStatus(org.marsem.kafka.model.ConsumerStatus.RUNNING) +
                consumerSessionRepository.countByStatus(org.marsem.kafka.model.ConsumerStatus.CREATED));
            
            health.put("status", "UP");
            health.put("timestamp", Instant.now());
            
        } catch (Exception e) {
            logger.error("Failed to collect health metrics", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
        }
        
        return health;
    }

    /**
     * Get recent activity for dashboard.
     */
    public List<Map<String, Object>> getRecentActivity() {
        logger.info("Getting recent activity");

        List<Map<String, Object>> activities = new ArrayList<>();

        try {
            // Get recent consumer sessions
            List<ConsumerSession> recentSessions = consumerSessionRepository
                .findTop10ByOrderByCreatedAtDesc();

            for (ConsumerSession session : recentSessions) {
                Map<String, Object> activity = new HashMap<>();

                if (session.getStatus() == org.marsem.kafka.model.ConsumerStatus.RUNNING) {
                    activity.put("type", "consumer_started");
                    activity.put("message", "Consumer started on topic " + session.getTopic());
                    activity.put("color", "blue");
                } else if (session.getStatus() == org.marsem.kafka.model.ConsumerStatus.STOPPED) {
                    activity.put("type", "consumer_stopped");
                    activity.put("message", "Consumer finished on topic " + session.getTopic() +
                        " (" + session.getMessagesConsumed() + " messages)");
                    activity.put("color", "green");
                } else if (session.getStatus() == org.marsem.kafka.model.ConsumerStatus.ERROR) {
                    activity.put("type", "consumer_error");
                    activity.put("message", "Consumer error on topic " + session.getTopic());
                    activity.put("color", "red");
                }

                activity.put("timestamp", session.getCreatedAt());
                activity.put("sessionId", session.getSessionId());
                activity.put("topic", session.getTopic());

                activities.add(activity);
            }

            // Get recent connections
            List<Connection> recentConnections = connectionRepository
                .findTop5ByOrderByCreatedAtDesc();

            for (Connection connection : recentConnections) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("type", "connection_created");
                activity.put("message", "Connection created: " + connection.getName());
                activity.put("color", "green");
                activity.put("timestamp", connection.getCreatedAt());
                activity.put("connectionId", connection.getId());
                activity.put("connectionName", connection.getName());

                activities.add(activity);
            }

            // Sort by timestamp descending
            activities.sort((a, b) -> {
                Instant timestampA = (Instant) a.get("timestamp");
                Instant timestampB = (Instant) b.get("timestamp");
                return timestampB.compareTo(timestampA);
            });

            // Return top 10 most recent
            return activities.stream().limit(10).collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Failed to get recent activity", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get system metrics.
     */
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> system = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            system.put("availableProcessors", runtime.availableProcessors());
            system.put("totalMemory", runtime.totalMemory());
            system.put("freeMemory", runtime.freeMemory());
            system.put("maxMemory", runtime.maxMemory());
            system.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
            
        } catch (Exception e) {
            logger.warn("Failed to collect system metrics", e);
        }
        
        return system;
    }
}
