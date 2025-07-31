package org.marsem.kafka.model;

/**
 * Enumeration of consumer session statuses.
 * 
 * Defines the lifecycle states of a Kafka consumer session
 * from creation through execution to completion or error.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
public enum ConsumerStatus {
    
    /**
     * Consumer session has been created but not yet started.
     * Initial state when a consumer session is first created.
     */
    CREATED("Created", "Consumer session created but not started"),
    
    /**
     * Consumer session is currently running and consuming messages.
     * Active state where the consumer is polling for messages.
     */
    RUNNING("Running", "Consumer is actively consuming messages"),
    
    /**
     * Consumer session has been paused temporarily.
     * Consumer is not polling but can be resumed.
     */
    PAUSED("Paused", "Consumer is temporarily paused"),
    
    /**
     * Consumer session has been stopped normally.
     * Final state when consumer is stopped by user or reaches max messages.
     */
    STOPPED("Stopped", "Consumer has been stopped"),
    
    /**
     * Consumer session encountered an error and stopped.
     * Final state when consumer fails due to an error condition.
     */
    ERROR("Error", "Consumer stopped due to an error");

    private final String displayName;
    private final String description;

    ConsumerStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the human-readable display name for the status.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets a description of what this status means.
     * 
     * @return the status description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status represents an active consumer.
     * 
     * @return true if the consumer is actively consuming messages
     */
    public boolean isActive() {
        return this == RUNNING;
    }

    /**
     * Checks if this status represents a finished consumer.
     * 
     * @return true if the consumer has finished (stopped or error)
     */
    public boolean isFinished() {
        return this == STOPPED || this == ERROR;
    }

    /**
     * Checks if this status represents a consumer that can be started.
     * 
     * @return true if the consumer can be started from this state
     */
    public boolean canStart() {
        return this == CREATED || this == PAUSED;
    }

    /**
     * Checks if this status represents a consumer that can be stopped.
     * 
     * @return true if the consumer can be stopped from this state
     */
    public boolean canStop() {
        return this == RUNNING || this == PAUSED;
    }

    /**
     * Checks if this status represents a consumer that can be paused.
     * 
     * @return true if the consumer can be paused from this state
     */
    public boolean canPause() {
        return this == RUNNING;
    }

    /**
     * Checks if this status represents a consumer that can be resumed.
     * 
     * @return true if the consumer can be resumed from this state
     */
    public boolean canResume() {
        return this == PAUSED;
    }

    /**
     * Gets the consumer status from a string value.
     * 
     * @param status the status string
     * @return the corresponding ConsumerStatus enum
     * @throws IllegalArgumentException if the status is not recognized
     */
    public static ConsumerStatus fromString(String status) {
        if (status == null) {
            return CREATED;
        }
        
        for (ConsumerStatus cs : values()) {
            if (cs.name().equalsIgnoreCase(status) || 
                cs.displayName.equalsIgnoreCase(status)) {
                return cs;
            }
        }
        
        throw new IllegalArgumentException("Unknown consumer status: " + status);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
