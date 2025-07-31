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
 * JPA Entity representing an active Kafka consumer session.
 * 
 * This entity tracks consumer sessions created through the web application,
 * allowing for management and monitoring of active consumers.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Entity
@Table(name = "consumer_sessions", indexes = {
    @Index(name = "idx_consumer_session_id", columnList = "sessionId"),
    @Index(name = "idx_consumer_connection", columnList = "connectionId"),
    @Index(name = "idx_consumer_status", columnList = "status"),
    @Index(name = "idx_consumer_created", columnList = "createdAt")
})
public class ConsumerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 100, message = "Session ID cannot exceed 100 characters")
    @Column(name = "session_id", nullable = false, unique = true, length = 100)
    private String sessionId;

    @NotNull(message = "Connection ID is required")
    @Column(name = "connection_id", nullable = false)
    private Long connectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "connection_id", insertable = false, updatable = false)
    private Connection connection;

    @NotBlank(message = "Topic is required")
    @Size(min = 1, max = 255, message = "Topic must be between 1 and 255 characters")
    @Column(name = "topic", nullable = false)
    private String topic;

    @NotBlank(message = "Consumer group is required")
    @Size(min = 1, max = 255, message = "Consumer group must be between 1 and 255 characters")
    @Column(name = "consumer_group", nullable = false)
    private String consumerGroup;

    @Column(name = "partition_id")
    private Integer partitionId;

    @Column(name = "start_offset")
    private Long startOffset;

    @Column(name = "current_offset")
    private Long currentOffset;

    @Column(name = "max_messages")
    private Integer maxMessages;

    @Column(name = "messages_consumed")
    private Long messagesConsumed = 0L;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConsumerStatus status = ConsumerStatus.CREATED;

    @Size(max = 100, message = "WebSocket session ID cannot exceed 100 characters")
    @Column(name = "websocket_session_id", length = 100)
    private String websocketSessionId;

    @Column(name = "auto_commit", nullable = false)
    private Boolean autoCommit = true;

    @Column(name = "poll_timeout_ms")
    private Integer pollTimeoutMs = 1000;

    @Size(max = 500, message = "Error message cannot exceed 500 characters")
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "stopped_at")
    private LocalDateTime stoppedAt;

    @Size(max = 100, message = "Created by cannot exceed 100 characters")
    @Column(name = "created_by", length = 100)
    private String createdBy;

    // Constructors
    public ConsumerSession() {}

    public ConsumerSession(String sessionId, Long connectionId, String topic, String consumerGroup) {
        this.sessionId = sessionId;
        this.connectionId = connectionId;
        this.topic = topic;
        this.consumerGroup = consumerGroup;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public Integer getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(Integer partitionId) {
        this.partitionId = partitionId;
    }

    public Long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(Long startOffset) {
        this.startOffset = startOffset;
    }

    public Long getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(Long currentOffset) {
        this.currentOffset = currentOffset;
    }

    public Integer getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(Integer maxMessages) {
        this.maxMessages = maxMessages;
    }

    public Long getMessagesConsumed() {
        return messagesConsumed;
    }

    public void setMessagesConsumed(Long messagesConsumed) {
        this.messagesConsumed = messagesConsumed;
    }

    public ConsumerStatus getStatus() {
        return status;
    }

    public void setStatus(ConsumerStatus status) {
        this.status = status;
    }

    public String getWebsocketSessionId() {
        return websocketSessionId;
    }

    public void setWebsocketSessionId(String websocketSessionId) {
        this.websocketSessionId = websocketSessionId;
    }

    public Boolean getAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(Boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    public Integer getPollTimeoutMs() {
        return pollTimeoutMs;
    }

    public void setPollTimeoutMs(Integer pollTimeoutMs) {
        this.pollTimeoutMs = pollTimeoutMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getStoppedAt() {
        return stoppedAt;
    }

    public void setStoppedAt(LocalDateTime stoppedAt) {
        this.stoppedAt = stoppedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // Utility methods
    public boolean isActive() {
        return status == ConsumerStatus.RUNNING;
    }

    public boolean isFinished() {
        return status == ConsumerStatus.STOPPED || status == ConsumerStatus.ERROR;
    }

    public void markAsStarted() {
        this.status = ConsumerStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void markAsStopped() {
        this.status = ConsumerStatus.STOPPED;
        this.stoppedAt = LocalDateTime.now();
    }

    public void markAsError(String errorMessage) {
        this.status = ConsumerStatus.ERROR;
        this.errorMessage = errorMessage;
        this.stoppedAt = LocalDateTime.now();
    }

    public void incrementMessagesConsumed() {
        this.messagesConsumed = (this.messagesConsumed == null ? 0 : this.messagesConsumed) + 1;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsumerSession that = (ConsumerSession) o;
        return Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId);
    }

    @Override
    public String toString() {
        return "ConsumerSession{" +
                "id=" + id +
                ", sessionId='" + sessionId + '\'' +
                ", topic='" + topic + '\'' +
                ", consumerGroup='" + consumerGroup + '\'' +
                ", status=" + status +
                ", messagesConsumed=" + messagesConsumed +
                '}';
    }
}
