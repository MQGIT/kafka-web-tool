package org.marsem.kafka.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI configuration for API documentation.
 * 
 * This configuration sets up comprehensive API documentation
 * for the Kafka Web App REST endpoints.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Configure OpenAPI documentation.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                    new Server()
                        .url("http://localhost:" + serverPort + contextPath)
                        .description("Development server"),
                    new Server()
                        .url("https://kafkatool.marsem.org/api/v1")
                        .description("Production server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("JWT"))
                .components(new io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes("JWT", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT token for authentication")
                    )
                );
    }

    /**
     * API information for documentation.
     */
    private Info apiInfo() {
        return new Info()
                .title("Kafka Web App v2 API")
                .description("""
                    Enterprise Kafka Management Tool API
                    
                    This API provides comprehensive functionality for managing Apache Kafka clusters including:
                    - Connection management with multiple security protocols
                    - Real-time message production and consumption
                    - Topic browsing and management
                    - WebSocket-based live streaming
                    - User authentication and authorization
                    - Comprehensive monitoring and metrics
                    
                    ## Authentication
                    Most endpoints require authentication using JWT tokens. Include the token in the Authorization header:
                    ```
                    Authorization: Bearer <your-jwt-token>
                    ```
                    
                    ## WebSocket Endpoints
                    Real-time functionality is available through WebSocket connections:
                    - `/ws/kafka` - Main WebSocket endpoint
                    - `/ws/consumer` - Consumer message streaming
                    - `/ws/producer` - Producer feedback
                    - `/ws/metrics` - Real-time metrics
                    - `/ws/topics` - Topic exploration
                    
                    ## Rate Limiting
                    API endpoints are rate-limited to ensure fair usage and system stability.
                    
                    ## Error Handling
                    All endpoints return consistent error responses with appropriate HTTP status codes and detailed error messages.
                    """)
                .version("2.0.0")
                .contact(new Contact()
                    .name("MarSem.org")
                    .email("support@marsem.org")
                    .url("https://marsem.org")
                )
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")
                );
    }
}
