package org.marsem.kafka.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * JPA Entity representing a consumed Kafka message stored for a consumer session.
 * 
 * This entity stores the actual consumed messages for retrieval and display
 * in the frontend, providing persistence across application restarts.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Entity
@Table(name = "consumer_messages", indexes = {
    @Index(name = "idx_consumer_message_session", columnList = "sessionId"),
    @Index(name = "idx_consumer_message_topic", columnList = "topic"),
    @Index(name = "idx_consumer_message_offset", columnList = "topic,partition,offset"),
    @Index(name = "idx_consumer_message_created", columnList = "createdAt")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsumerMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Session ID is required")
    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @NotBlank(message = "Topic is required")
    @Column(name = "topic", nullable = false)
    private String topic;

    @NotNull(message = "Partition is required")
    @Column(name = "partition", nullable = false)
    private Integer partition;

    @NotNull(message = "Offset is required")
    @Column(name = "message_offset", nullable = false)
    private Long offset;

    @Column(name = "message_key", columnDefinition = "TEXT")
    private String key;

    @Column(name = "message_value", columnDefinition = "TEXT")
    private String value;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    @Column(name = "message_timestamp")
    private Instant timestamp;

    @Column(name = "serialized_key_size")
    private Integer serializedKeySize;

    @Column(name = "serialized_value_size")
    private Integer serializedValueSize;

    @Column(name = "leader_epoch")
    private Integer leaderEpoch;

    @Column(name = "consumer_group")
    private String consumerGroup;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Constructors
    public ConsumerMessage() {}

    public ConsumerMessage(String sessionId, KafkaMessage kafkaMessage) {
        this.sessionId = sessionId;
        this.topic = kafkaMessage.getTopic();
        this.partition = kafkaMessage.getPartition();
        this.offset = kafkaMessage.getOffset();
        this.key = kafkaMessage.getKey();
        this.value = kafkaMessage.getValue();
        this.timestamp = kafkaMessage.getTimestamp();
        this.serializedKeySize = kafkaMessage.getSerializedKeySize();
        this.serializedValueSize = kafkaMessage.getSerializedValueSize();
        this.leaderEpoch = kafkaMessage.getLeaderEpoch();
        this.consumerGroup = kafkaMessage.getConsumerGroup();
    }

    // Convert to KafkaMessage for API responses
    public KafkaMessage toKafkaMessage() {
        KafkaMessage message = new KafkaMessage();
        message.setTopic(this.topic);
        message.setPartition(this.partition);
        message.setOffset(this.offset);
        message.setKey(this.key);
        message.setValue(this.value);
        message.setTimestamp(this.timestamp);
        message.setSerializedKeySize(this.serializedKeySize);
        message.setSerializedValueSize(this.serializedValueSize);
        message.setLeaderEpoch(this.leaderEpoch);
        message.setConsumerGroup(this.consumerGroup);
        return message;
    }

    // Getters and setters
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getSerializedKeySize() {
        return serializedKeySize;
    }

    public void setSerializedKeySize(Integer serializedKeySize) {
        this.serializedKeySize = serializedKeySize;
    }

    public Integer getSerializedValueSize() {
        return serializedValueSize;
    }

    public void setSerializedValueSize(Integer serializedValueSize) {
        this.serializedValueSize = serializedValueSize;
    }

    public Integer getLeaderEpoch() {
        return leaderEpoch;
    }

    public void setLeaderEpoch(Integer leaderEpoch) {
        this.leaderEpoch = leaderEpoch;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Utility methods
    public String getTopicPartition() {
        return topic + "-" + partition;
    }

    public int getMessageSize() {
        int size = 0;
        if (serializedKeySize != null) {
            size += serializedKeySize;
        }
        if (serializedValueSize != null) {
            size += serializedValueSize;
        }
        return size;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsumerMessage that = (ConsumerMessage) o;
        return Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(topic, that.topic) &&
               Objects.equals(partition, that.partition) &&
               Objects.equals(offset, that.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, topic, partition, offset);
    }

    @Override
    public String toString() {
        return "ConsumerMessage{" +
                "id=" + id +
                ", sessionId='" + sessionId + '\'' +
                ", topic='" + topic + '\'' +
                ", partition=" + partition +
                ", offset=" + offset +
                ", key='" + key + '\'' +
                ", timestamp=" + timestamp +
                ", createdAt=" + createdAt +
                '}';
    }
}
