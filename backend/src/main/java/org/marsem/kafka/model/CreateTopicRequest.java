package org.marsem.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request model for creating a new Kafka topic.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Schema(description = "Request to create a new Kafka topic")
public class CreateTopicRequest {

    @NotBlank(message = "Topic name is required")
    @Schema(description = "Name of the topic to create", example = "my-topic")
    private String name;

    @NotNull(message = "Number of partitions is required")
    @Min(value = 1, message = "Number of partitions must be at least 1")
    @Schema(description = "Number of partitions for the topic", example = "3")
    private Integer partitions;

    @NotNull(message = "Replication factor is required")
    @Min(value = 1, message = "Replication factor must be at least 1")
    @Schema(description = "Replication factor for the topic", example = "1")
    @JsonProperty("replicationFactor")
    private Integer replicationFactor;

    @Schema(description = "Additional topic configuration properties")
    private Map<String, String> configs;

    // Default constructor
    public CreateTopicRequest() {}

    // Constructor with required fields
    public CreateTopicRequest(String name, Integer partitions, Integer replicationFactor) {
        this.name = name;
        this.partitions = partitions;
        this.replicationFactor = replicationFactor;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPartitions() {
        return partitions;
    }

    public void setPartitions(Integer partitions) {
        this.partitions = partitions;
    }

    public Integer getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(Integer replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }

    @Override
    public String toString() {
        return "CreateTopicRequest{" +
                "name='" + name + '\'' +
                ", partitions=" + partitions +
                ", replicationFactor=" + replicationFactor +
                ", configs=" + configs +
                '}';
    }
}
