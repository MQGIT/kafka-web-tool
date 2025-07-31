package org.marsem.kafka.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a Kafka message with metadata.
 * 
 * This class encapsulates a Kafka message including its key, value, headers,
 * partition information, offset, and timestamp. Used for both producing
 * and consuming messages.
 * 
 * Note: This is not a JPA entity as Kafka messages are transient and
 * stored in Kafka topics, not in the application database.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KafkaMessage {

    private String topic;
    private Integer partition;
    private Long offset;
    private String key;
    private String value;
    private Map<String, String> headers;
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;
    
    private String keySerializer;
    private String valueSerializer;
    private String keyDeserializer;
    private String valueDeserializer;
    
    // Additional metadata for consumer messages
    private String consumerGroup;
    private Long timestampType;
    private Integer serializedKeySize;
    private Integer serializedValueSize;
    private Integer leaderEpoch;

    // Constructors
    public KafkaMessage() {}

    public KafkaMessage(String topic, String key, String value) {
        this.topic = topic;
        this.key = key;
        this.value = value;
        this.timestamp = Instant.now();
    }

    public KafkaMessage(String topic, Integer partition, String key, String value) {
        this.topic = topic;
        this.partition = partition;
        this.key = key;
        this.value = value;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
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

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getKeySerializer() {
        return keySerializer;
    }

    public void setKeySerializer(String keySerializer) {
        this.keySerializer = keySerializer;
    }

    public String getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(String valueSerializer) {
        this.valueSerializer = valueSerializer;
    }

    public String getKeyDeserializer() {
        return keyDeserializer;
    }

    public void setKeyDeserializer(String keyDeserializer) {
        this.keyDeserializer = keyDeserializer;
    }

    public String getValueDeserializer() {
        return valueDeserializer;
    }

    public void setValueDeserializer(String valueDeserializer) {
        this.valueDeserializer = valueDeserializer;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public Long getTimestampType() {
        return timestampType;
    }

    public void setTimestampType(Long timestampType) {
        this.timestampType = timestampType;
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

    // Utility methods
    public boolean hasKey() {
        return key != null && !key.trim().isEmpty();
    }

    public boolean hasValue() {
        return value != null && !value.trim().isEmpty();
    }

    public boolean hasHeaders() {
        return headers != null && !headers.isEmpty();
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

    public String getTopicPartition() {
        if (partition != null) {
            return topic + "-" + partition;
        }
        return topic;
    }

    // Builder pattern for easier construction
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final KafkaMessage message = new KafkaMessage();

        public Builder topic(String topic) {
            message.setTopic(topic);
            return this;
        }

        public Builder partition(Integer partition) {
            message.setPartition(partition);
            return this;
        }

        public Builder offset(Long offset) {
            message.setOffset(offset);
            return this;
        }

        public Builder key(String key) {
            message.setKey(key);
            return this;
        }

        public Builder value(String value) {
            message.setValue(value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            message.setHeaders(headers);
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            message.setTimestamp(timestamp);
            return this;
        }

        public Builder consumerGroup(String consumerGroup) {
            message.setConsumerGroup(consumerGroup);
            return this;
        }

        public KafkaMessage build() {
            if (message.getTimestamp() == null) {
                message.setTimestamp(Instant.now());
            }
            return message;
        }
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaMessage that = (KafkaMessage) o;
        return Objects.equals(topic, that.topic) &&
               Objects.equals(partition, that.partition) &&
               Objects.equals(offset, that.offset) &&
               Objects.equals(key, that.key) &&
               Objects.equals(value, that.value) &&
               Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic, partition, offset, key, value, timestamp);
    }

    @Override
    public String toString() {
        return "KafkaMessage{" +
                "topic='" + topic + '\'' +
                ", partition=" + partition +
                ", offset=" + offset +
                ", key='" + key + '\'' +
                ", value='" + (value != null ? value.substring(0, Math.min(value.length(), 100)) + "..." : null) + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
