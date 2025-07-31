package org.marsem.kafka.model;

/**
 * Enumeration of SASL authentication mechanisms supported by Kafka.
 * 
 * Defines the available SASL mechanisms for authenticating with Kafka brokers
 * when using SASL_PLAINTEXT or SASL_SSL security protocols.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
public enum SaslMechanism {
    
    /**
     * PLAIN SASL mechanism.
     * Simple username/password authentication transmitted in plain text.
     * Should only be used with SSL encryption in production.
     */
    PLAIN("PLAIN", "Username/password authentication", true),
    
    /**
     * SCRAM-SHA-256 SASL mechanism.
     * Salted Challenge Response Authentication Mechanism using SHA-256.
     * More secure than PLAIN as passwords are not transmitted directly.
     */
    SCRAM_SHA_256("SCRAM-SHA-256", "SCRAM with SHA-256 hashing", true),
    
    /**
     * SCRAM-SHA-512 SASL mechanism.
     * Salted Challenge Response Authentication Mechanism using SHA-512.
     * More secure than SCRAM-SHA-256 with stronger hashing.
     */
    SCRAM_SHA_512("SCRAM-SHA-512", "SCRAM with SHA-512 hashing", true),
    
    /**
     * GSSAPI (Kerberos) SASL mechanism.
     * Enterprise authentication using Kerberos tickets.
     * Requires Kerberos infrastructure and configuration.
     */
    GSSAPI("GSSAPI", "Kerberos authentication", false),
    
    /**
     * OAUTHBEARER SASL mechanism.
     * OAuth 2.0 bearer token authentication.
     * Used for modern OAuth-based authentication flows.
     */
    OAUTHBEARER("OAUTHBEARER", "OAuth 2.0 bearer token authentication", false),
    
    /**
     * AWS_MSK_IAM SASL mechanism.
     * Amazon MSK IAM-based authentication.
     * Specific to Amazon Managed Streaming for Apache Kafka.
     */
    AWS_MSK_IAM("AWS_MSK_IAM", "AWS MSK IAM authentication", false);

    private final String mechanism;
    private final String description;
    private final boolean supportsUsernamePassword;

    SaslMechanism(String mechanism, String description, boolean supportsUsernamePassword) {
        this.mechanism = mechanism;
        this.description = description;
        this.supportsUsernamePassword = supportsUsernamePassword;
    }

    /**
     * Gets the SASL mechanism string value.
     * 
     * @return the mechanism string used by Kafka clients
     */
    public String getMechanism() {
        return mechanism;
    }

    /**
     * Gets a human-readable description of the SASL mechanism.
     * 
     * @return description of the mechanism
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this mechanism supports simple username/password authentication.
     * 
     * @return true if the mechanism uses username/password credentials
     */
    public boolean supportsUsernamePassword() {
        return supportsUsernamePassword;
    }

    /**
     * Checks if this mechanism is recommended for production use.
     * 
     * @return true if the mechanism provides adequate security for production
     */
    public boolean isProductionRecommended() {
        return this == SCRAM_SHA_256 || this == SCRAM_SHA_512 || this == GSSAPI;
    }

    /**
     * Checks if this mechanism requires additional configuration beyond username/password.
     * 
     * @return true if the mechanism requires additional setup (Kerberos, OAuth, etc.)
     */
    public boolean requiresAdditionalConfig() {
        return this == GSSAPI || this == OAUTHBEARER || this == AWS_MSK_IAM;
    }

    /**
     * Gets the SASL mechanism from a string value.
     * 
     * @param mechanism the mechanism string
     * @return the corresponding SaslMechanism enum
     * @throws IllegalArgumentException if the mechanism is not recognized
     */
    public static SaslMechanism fromString(String mechanism) {
        if (mechanism == null) {
            return PLAIN;
        }
        
        for (SaslMechanism sm : values()) {
            if (sm.mechanism.equalsIgnoreCase(mechanism)) {
                return sm;
            }
        }
        
        throw new IllegalArgumentException("Unknown SASL mechanism: " + mechanism);
    }

    @Override
    public String toString() {
        return mechanism;
    }
}
