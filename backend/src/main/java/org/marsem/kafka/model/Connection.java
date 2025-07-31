package org.marsem.kafka.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity representing a Kafka cluster connection configuration.
 * 
 * This entity stores connection details for Kafka clusters including
 * bootstrap servers, security configurations, and authentication details.
 * Supports multiple authentication mechanisms including PLAINTEXT, SASL_SSL, etc.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Entity
@Table(name = "connections", indexes = {
    @Index(name = "idx_connection_name", columnList = "name"),
    @Index(name = "idx_connection_active", columnList = "active"),
    @Index(name = "idx_connection_created", columnList = "createdAt")
})
public class Connection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Connection name is required")
    @Size(min = 1, max = 100, message = "Connection name must be between 1 and 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @NotBlank(message = "Bootstrap servers are required")
    @Size(min = 1, max = 1000, message = "Bootstrap servers must be between 1 and 1000 characters")
    @Column(name = "bootstrap_servers", nullable = false, length = 1000)
    private String bootstrapServers;

    @NotNull(message = "Security protocol is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "security_protocol", nullable = false)
    private SecurityProtocol securityProtocol = SecurityProtocol.PLAINTEXT;

    @Enumerated(EnumType.STRING)
    @Column(name = "sasl_mechanism")
    private SaslMechanism saslMechanism;

    @Size(max = 1000, message = "SASL JAAS config cannot exceed 1000 characters")
    @Column(name = "sasl_jaas_config", length = 1000)
    private String saslJaasConfig;

    @Size(max = 255, message = "Username cannot exceed 255 characters")
    @Column(name = "username")
    private String username;

    @Size(max = 255, message = "Password cannot exceed 255 characters")
    @Column(name = "password")
    private String password;

    @Size(max = 500, message = "SSL truststore location cannot exceed 500 characters")
    @Column(name = "ssl_truststore_location", length = 500)
    private String sslTruststoreLocation;

    @Size(max = 255, message = "SSL truststore password cannot exceed 255 characters")
    @Column(name = "ssl_truststore_password")
    private String sslTruststorePassword;

    @Size(max = 500, message = "SSL keystore location cannot exceed 500 characters")
    @Column(name = "ssl_keystore_location", length = 500)
    private String sslKeystoreLocation;

    @Size(max = 255, message = "SSL keystore password cannot exceed 255 characters")
    @Column(name = "ssl_keystore_password")
    private String sslKeystorePassword;

    @Size(max = 255, message = "SSL key password cannot exceed 255 characters")
    @Column(name = "ssl_key_password")
    private String sslKeyPassword;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "connection_timeout_ms")
    private Integer connectionTimeoutMs = 30000;

    @Column(name = "request_timeout_ms")
    private Integer requestTimeoutMs = 30000;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Size(max = 100, message = "Created by cannot exceed 100 characters")
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Size(max = 100, message = "Updated by cannot exceed 100 characters")
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // Constructors
    public Connection() {}

    public Connection(String name, String bootstrapServers, SecurityProtocol securityProtocol) {
        this.name = name;
        this.bootstrapServers = bootstrapServers;
        this.securityProtocol = securityProtocol;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public SecurityProtocol getSecurityProtocol() {
        return securityProtocol;
    }

    public void setSecurityProtocol(SecurityProtocol securityProtocol) {
        this.securityProtocol = securityProtocol;
    }

    public SaslMechanism getSaslMechanism() {
        return saslMechanism;
    }

    public void setSaslMechanism(SaslMechanism saslMechanism) {
        this.saslMechanism = saslMechanism;
    }

    public String getSaslJaasConfig() {
        return saslJaasConfig;
    }

    public void setSaslJaasConfig(String saslJaasConfig) {
        this.saslJaasConfig = saslJaasConfig;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSslTruststoreLocation() {
        return sslTruststoreLocation;
    }

    public void setSslTruststoreLocation(String sslTruststoreLocation) {
        this.sslTruststoreLocation = sslTruststoreLocation;
    }

    public String getSslTruststorePassword() {
        return sslTruststorePassword;
    }

    public void setSslTruststorePassword(String sslTruststorePassword) {
        this.sslTruststorePassword = sslTruststorePassword;
    }

    public String getSslKeystoreLocation() {
        return sslKeystoreLocation;
    }

    public void setSslKeystoreLocation(String sslKeystoreLocation) {
        this.sslKeystoreLocation = sslKeystoreLocation;
    }

    public String getSslKeystorePassword() {
        return sslKeystorePassword;
    }

    public void setSslKeystorePassword(String sslKeystorePassword) {
        this.sslKeystorePassword = sslKeystorePassword;
    }

    public String getSslKeyPassword() {
        return sslKeyPassword;
    }

    public void setSslKeyPassword(String sslKeyPassword) {
        this.sslKeyPassword = sslKeyPassword;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(Integer connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public Integer getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(Integer requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    // Utility methods
    public boolean isSecure() {
        return securityProtocol == SecurityProtocol.SASL_SSL || 
               securityProtocol == SecurityProtocol.SSL;
    }

    public boolean requiresAuthentication() {
        return securityProtocol == SecurityProtocol.SASL_PLAINTEXT || 
               securityProtocol == SecurityProtocol.SASL_SSL;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return Objects.equals(id, that.id) && 
               Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Connection{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", bootstrapServers='" + bootstrapServers + '\'' +
                ", securityProtocol=" + securityProtocol +
                ", active=" + active +
                ", createdAt=" + createdAt +
                '}';
    }
}
