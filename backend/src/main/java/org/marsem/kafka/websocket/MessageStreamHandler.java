package org.marsem.kafka.websocket;

import org.marsem.kafka.model.KafkaMessage;
import org.marsem.kafka.service.KafkaConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket message handler for real-time Kafka message streaming.
 * 
 * This handler manages WebSocket connections for real-time message streaming,
 * consumer session management, and live updates for the frontend.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Controller
public class MessageStreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(MessageStreamHandler.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final KafkaConsumerService consumerService;
    
    // Track active WebSocket sessions
    private final Map<String, String> sessionToConsumer = new ConcurrentHashMap<>();
    private final Map<String, Instant> sessionHeartbeats = new ConcurrentHashMap<>();

    @Autowired
    public MessageStreamHandler(SimpMessagingTemplate messagingTemplate, 
                               KafkaConsumerService consumerService) {
        this.messagingTemplate = messagingTemplate;
        this.consumerService = consumerService;
    }

    /**
     * Handle subscription to consumer message stream.
     */
    @SubscribeMapping("/consumer/{sessionId}")
    public Map<String, Object> subscribeToConsumer(@DestinationVariable String sessionId) {
        logger.info("WebSocket subscription to consumer session: {}", sessionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "subscription");
        response.put("sessionId", sessionId);
        response.put("timestamp", Instant.now());
        response.put("message", "Subscribed to consumer session");
        
        // Track the subscription
        sessionHeartbeats.put(sessionId, Instant.now());
        
        return response;
    }

    /**
     * Handle subscription to producer feedback.
     */
    @SubscribeMapping("/producer/{connectionId}")
    public Map<String, Object> subscribeToProducer(@DestinationVariable String connectionId) {
        logger.info("WebSocket subscription to producer feedback for connection: {}", connectionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "subscription");
        response.put("connectionId", connectionId);
        response.put("timestamp", Instant.now());
        response.put("message", "Subscribed to producer feedback");
        
        return response;
    }

    /**
     * Handle subscription to topic metrics.
     */
    @SubscribeMapping("/metrics/{connectionId}")
    public Map<String, Object> subscribeToMetrics(@DestinationVariable String connectionId) {
        logger.info("WebSocket subscription to metrics for connection: {}", connectionId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "subscription");
        response.put("connectionId", connectionId);
        response.put("timestamp", Instant.now());
        response.put("message", "Subscribed to metrics");
        
        return response;
    }

    /**
     * Handle consumer control messages.
     */
    @MessageMapping("/consumer/{sessionId}/control")
    @SendTo("/topic/consumer/{sessionId}/status")
    public Map<String, Object> handleConsumerControl(
            @DestinationVariable String sessionId,
            @Payload Map<String, Object> controlMessage) {
        
        logger.info("Received consumer control message for session: {} - {}", sessionId, controlMessage);
        
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("timestamp", Instant.now());
        
        try {
            String action = (String) controlMessage.get("action");
            
            switch (action) {
                case "start":
                    consumerService.startConsumer(sessionId);
                    response.put("status", "started");
                    response.put("message", "Consumer started successfully");
                    break;
                    
                case "stop":
                    consumerService.stopConsumer(sessionId);
                    response.put("status", "stopped");
                    response.put("message", "Consumer stopped successfully");
                    break;
                    
                case "pause":
                    consumerService.pauseConsumer(sessionId);
                    response.put("status", "paused");
                    response.put("message", "Consumer paused successfully");
                    break;
                    
                case "resume":
                    consumerService.resumeConsumer(sessionId);
                    response.put("status", "resumed");
                    response.put("message", "Consumer resumed successfully");
                    break;
                    
                default:
                    response.put("status", "error");
                    response.put("message", "Unknown action: " + action);
                    logger.warn("Unknown consumer control action: {}", action);
            }
            
        } catch (Exception e) {
            logger.error("Error handling consumer control message for session: {}", sessionId, e);
            response.put("status", "error");
            response.put("message", "Failed to execute action: " + e.getMessage());
        }
        
        return response;
    }

    /**
     * Handle heartbeat messages to keep connections alive.
     */
    @MessageMapping("/heartbeat")
    @SendTo("/topic/heartbeat")
    public Map<String, Object> handleHeartbeat(@Payload Map<String, Object> heartbeat) {
        String sessionId = (String) heartbeat.get("sessionId");
        
        if (sessionId != null) {
            sessionHeartbeats.put(sessionId, Instant.now());
            logger.debug("Received heartbeat from session: {}", sessionId);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("type", "heartbeat");
        response.put("timestamp", Instant.now());
        response.put("message", "pong");
        
        return response;
    }

    /**
     * Handle producer feedback messages.
     */
    @MessageMapping("/producer/{connectionId}/feedback")
    public void handleProducerFeedback(
            @DestinationVariable String connectionId,
            @Payload Map<String, Object> feedback) {
        
        logger.debug("Received producer feedback for connection: {}", connectionId);
        
        // Broadcast feedback to subscribers
        messagingTemplate.convertAndSend("/topic/producer/" + connectionId + "/feedback", feedback);
    }

    /**
     * Send a Kafka message via WebSocket to subscribers.
     */
    public void sendKafkaMessage(String sessionId, KafkaMessage message) {
        try {
            Map<String, Object> messageWrapper = new HashMap<>();
            messageWrapper.put("type", "message");
            messageWrapper.put("sessionId", sessionId);
            messageWrapper.put("timestamp", Instant.now());
            messageWrapper.put("message", message);
            
            messagingTemplate.convertAndSend("/topic/consumer/" + sessionId, messageWrapper);
            
            logger.debug("Sent Kafka message via WebSocket for session: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("Failed to send Kafka message via WebSocket for session: {}", sessionId, e);
        }
    }

    /**
     * Send consumer session status update.
     */
    public void sendConsumerStatus(String sessionId, String status, String message) {
        try {
            Map<String, Object> statusUpdate = new HashMap<>();
            statusUpdate.put("type", "status");
            statusUpdate.put("sessionId", sessionId);
            statusUpdate.put("status", status);
            statusUpdate.put("message", message);
            statusUpdate.put("timestamp", Instant.now());
            
            messagingTemplate.convertAndSend("/topic/consumer/" + sessionId + "/status", statusUpdate);
            
            logger.debug("Sent consumer status update for session: {} - {}", sessionId, status);
            
        } catch (Exception e) {
            logger.error("Failed to send consumer status update for session: {}", sessionId, e);
        }
    }

    /**
     * Send error message to WebSocket subscribers.
     */
    public void sendError(String sessionId, String error, String details) {
        try {
            Map<String, Object> errorMessage = new HashMap<>();
            errorMessage.put("type", "error");
            errorMessage.put("sessionId", sessionId);
            errorMessage.put("error", error);
            errorMessage.put("details", details);
            errorMessage.put("timestamp", Instant.now());
            
            messagingTemplate.convertAndSend("/topic/consumer/" + sessionId + "/error", errorMessage);
            
            logger.debug("Sent error message via WebSocket for session: {}", sessionId);
            
        } catch (Exception e) {
            logger.error("Failed to send error message via WebSocket for session: {}", sessionId, e);
        }
    }

    /**
     * Send producer metrics update.
     */
    public void sendProducerMetrics(String connectionId, Map<String, Object> metrics) {
        try {
            Map<String, Object> metricsUpdate = new HashMap<>();
            metricsUpdate.put("type", "metrics");
            metricsUpdate.put("connectionId", connectionId);
            metricsUpdate.put("metrics", metrics);
            metricsUpdate.put("timestamp", Instant.now());
            
            messagingTemplate.convertAndSend("/topic/metrics/" + connectionId + "/producer", metricsUpdate);
            
            logger.debug("Sent producer metrics update for connection: {}", connectionId);
            
        } catch (Exception e) {
            logger.error("Failed to send producer metrics for connection: {}", connectionId, e);
        }
    }

    /**
     * Send topic statistics update.
     */
    public void sendTopicStatistics(String connectionId, Map<String, Object> statistics) {
        try {
            Map<String, Object> statsUpdate = new HashMap<>();
            statsUpdate.put("type", "statistics");
            statsUpdate.put("connectionId", connectionId);
            statsUpdate.put("statistics", statistics);
            statsUpdate.put("timestamp", Instant.now());
            
            messagingTemplate.convertAndSend("/topic/metrics/" + connectionId + "/topics", statsUpdate);
            
            logger.debug("Sent topic statistics update for connection: {}", connectionId);
            
        } catch (Exception e) {
            logger.error("Failed to send topic statistics for connection: {}", connectionId, e);
        }
    }

    /**
     * Broadcast system notification to all connected clients.
     */
    public void broadcastSystemNotification(String message, String level) {
        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", "notification");
            notification.put("level", level); // info, warning, error
            notification.put("message", message);
            notification.put("timestamp", Instant.now());
            
            messagingTemplate.convertAndSend("/topic/system/notifications", notification);
            
            logger.info("Broadcasted system notification: {} [{}]", message, level);
            
        } catch (Exception e) {
            logger.error("Failed to broadcast system notification", e);
        }
    }

    /**
     * Clean up inactive sessions.
     */
    public void cleanupInactiveSessions() {
        Instant cutoff = Instant.now().minusSeconds(300); // 5 minutes
        
        sessionHeartbeats.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(cutoff)) {
                String sessionId = entry.getKey();
                logger.info("Cleaning up inactive WebSocket session: {}", sessionId);
                
                // Remove from consumer mapping
                sessionToConsumer.remove(sessionId);
                
                return true;
            }
            return false;
        });
    }

    /**
     * Get active WebSocket session count.
     */
    public int getActiveSessionCount() {
        return sessionHeartbeats.size();
    }

    /**
     * Get session heartbeat status.
     */
    public Map<String, Instant> getSessionHeartbeats() {
        return new HashMap<>(sessionHeartbeats);
    }
}
