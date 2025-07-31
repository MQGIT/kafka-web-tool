package org.marsem.kafka.model;

/**
 * Enumeration of Kafka security protocols.
 * 
 * Defines the available security protocols for Kafka connections,
 * including plaintext and encrypted options with various authentication mechanisms.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
public enum SecurityProtocol {
    
    /**
     * Plain text communication without encryption or authentication.
     * Used for development and testing environments.
     */
    PLAINTEXT("PLAINTEXT", "Plain text (no encryption)", false, false),
    
    /**
     * SSL/TLS encrypted communication without SASL authentication.
     * Provides encryption but relies on SSL certificates for authentication.
     */
    SSL("SSL", "SSL/TLS encryption", true, false),
    
    /**
     * SASL authentication over plain text communication.
     * Provides authentication but no encryption.
     */
    SASL_PLAINTEXT("SASL_PLAINTEXT", "SASL authentication (no encryption)", false, true),
    
    /**
     * SASL authentication over SSL/TLS encrypted communication.
     * Provides both authentication and encryption - recommended for production.
     */
    SASL_SSL("SASL_SSL", "SASL authentication with SSL/TLS encryption", true, true);

    private final String protocol;
    private final String description;
    private final boolean encrypted;
    private final boolean requiresAuthentication;

    SecurityProtocol(String protocol, String description, boolean encrypted, boolean requiresAuthentication) {
        this.protocol = protocol;
        this.description = description;
        this.encrypted = encrypted;
        this.requiresAuthentication = requiresAuthentication;
    }

    /**
     * Gets the Kafka protocol string value.
     * 
     * @return the protocol string used by Kafka clients
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Gets a human-readable description of the security protocol.
     * 
     * @return description of the protocol
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this protocol provides encryption.
     * 
     * @return true if the protocol uses SSL/TLS encryption
     */
    public boolean isEncrypted() {
        return encrypted;
    }

    /**
     * Checks if this protocol requires authentication.
     * 
     * @return true if the protocol requires SASL authentication
     */
    public boolean requiresAuthentication() {
        return requiresAuthentication;
    }

    /**
     * Checks if this protocol is suitable for production use.
     * 
     * @return true if the protocol provides adequate security for production
     */
    public boolean isProductionReady() {
        return this == SSL || this == SASL_SSL;
    }

    /**
     * Gets the security protocol from a string value.
     * 
     * @param protocol the protocol string
     * @return the corresponding SecurityProtocol enum
     * @throws IllegalArgumentException if the protocol is not recognized
     */
    public static SecurityProtocol fromString(String protocol) {
        if (protocol == null) {
            return PLAINTEXT;
        }
        
        for (SecurityProtocol sp : values()) {
            if (sp.protocol.equalsIgnoreCase(protocol)) {
                return sp;
            }
        }
        
        throw new IllegalArgumentException("Unknown security protocol: " + protocol);
    }

    @Override
    public String toString() {
        return protocol;
    }
}
