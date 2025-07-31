package org.marsem.kafka.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * WebSocket configuration for real-time message streaming.
 * 
 * This configuration enables WebSocket support for real-time communication
 * between the frontend and backend, particularly for streaming Kafka messages
 * and providing live updates.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:http://localhost:3000,https://kafkatool.marsem.org}")
    private String[] allowedOrigins;

    /**
     * Configure the message broker for WebSocket communication.
     * 
     * Sets up topic prefixes and application destination prefixes for
     * organizing WebSocket message routing.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker for broadcasting messages
        // Topic destinations for broadcasting to multiple subscribers
        config.enableSimpleBroker("/topic", "/queue", "/user");
        
        // Application destination prefix for messages bound for @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
        
        // Heartbeat settings for connection health
        // Note: setHeartbeatValue is not available in newer Spring versions
        // Heartbeat is handled automatically by the framework
    }

    /**
     * Register STOMP endpoints for WebSocket connections.
     * 
     * Defines the WebSocket endpoints that clients can connect to,
     * with SockJS fallback support for browsers that don't support WebSocket.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Main WebSocket endpoint for real-time message streaming
        registry.addEndpoint("/ws/kafka")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000)
                .setStreamBytesLimit(128 * 1024)
                .setHttpMessageCacheSize(1000)
                .setSessionCookieNeeded(false);

        // Consumer-specific endpoint for message consumption
        registry.addEndpoint("/ws/consumer")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000);

        // Producer-specific endpoint for message production feedback
        registry.addEndpoint("/ws/producer")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000);

        // Metrics endpoint for real-time monitoring
        registry.addEndpoint("/ws/metrics")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS()
                .setHeartbeatTime(30000)
                .setDisconnectDelay(5000);

        // Topic browser endpoint for real-time topic exploration
        registry.addEndpoint("/ws/topics")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS()
                .setHeartbeatTime(30000)
                .setDisconnectDelay(5000);
    }

    /**
     * Configure WebSocket transport options.
     * 
     * Sets limits and timeouts for WebSocket connections to ensure
     * optimal performance and resource management.
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Message size limits
        registration.setMessageSizeLimit(64 * 1024); // 64KB per message
        registration.setSendBufferSizeLimit(512 * 1024); // 512KB send buffer
        registration.setSendTimeLimit(20000); // 20 seconds send timeout
        
        // Connection limits
        registration.setTimeToFirstMessage(30000); // 30 seconds to first message
    }

    /**
     * Get the allowed origins for CORS.
     */
    public String[] getAllowedOrigins() {
        return allowedOrigins;
    }
}
