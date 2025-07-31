package org.marsem.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Kafka Web App v2 - Enterprise Kafka Management Tool
 *
 * A modern, high-performance web application for managing Apache Kafka clusters.
 * Built with Java Spring Boot and React TypeScript for enterprise-grade
 * performance, security, and scalability.
 *
 * Features:
 * - Multi-cluster connection management
 * - Real-time message production and consumption
 * - Topic browsing and management
 * - WebSocket-based live streaming
 * - Enterprise authentication and authorization
 * - Comprehensive monitoring and metrics
 *
 * @author MarSem.org
 * @version 2.0.0
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class KafkaWebAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaWebAppApplication.class, args);
    }
}
