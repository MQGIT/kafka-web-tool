package org.marsem.kafka.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents Kafka topic information and metadata.
 * 
 * This class encapsulates comprehensive information about a Kafka topic
 * including partitions, replicas, configuration, and statistics.
 * 
 * Note: This is not a JPA entity as topic information is retrieved
 * dynamically from Kafka clusters and not stored in the application database.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TopicInfo {

    private String name;
    private Integer partitionCount;
    private Short replicationFactor;
    private List<PartitionInfo> partitions;
    private Map<String, String> configs;
    private Boolean internal;
    
    // Statistics
    private Long totalMessages;
    private Long totalSize;
    private Long earliestOffset;
    private Long latestOffset;
    
    // Consumer group information
    private List<ConsumerGroupInfo> consumerGroups;

    // Constructors
    public TopicInfo() {}

    public TopicInfo(String name) {
        this.name = name;
    }

    public TopicInfo(String name, Integer partitionCount, Short replicationFactor) {
        this.name = name;
        this.partitionCount = partitionCount;
        this.replicationFactor = replicationFactor;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getPartitionCount() {
        return partitionCount;
    }

    public void setPartitionCount(Integer partitionCount) {
        this.partitionCount = partitionCount;
    }

    public Short getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(Short replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public List<PartitionInfo> getPartitions() {
        return partitions;
    }

    public void setPartitions(List<PartitionInfo> partitions) {
        this.partitions = partitions;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }

    public Boolean getInternal() {
        return internal;
    }

    public void setInternal(Boolean internal) {
        this.internal = internal;
    }

    public Long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(Long totalMessages) {
        this.totalMessages = totalMessages;
    }

    public Long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(Long totalSize) {
        this.totalSize = totalSize;
    }

    public Long getEarliestOffset() {
        return earliestOffset;
    }

    public void setEarliestOffset(Long earliestOffset) {
        this.earliestOffset = earliestOffset;
    }

    public Long getLatestOffset() {
        return latestOffset;
    }

    public void setLatestOffset(Long latestOffset) {
        this.latestOffset = latestOffset;
    }

    public List<ConsumerGroupInfo> getConsumerGroups() {
        return consumerGroups;
    }

    public void setConsumerGroups(List<ConsumerGroupInfo> consumerGroups) {
        this.consumerGroups = consumerGroups;
    }

    // Utility methods
    public boolean isInternal() {
        return internal != null && internal;
    }

    public boolean hasMessages() {
        return totalMessages != null && totalMessages > 0;
    }

    public String getRetentionMs() {
        if (configs != null) {
            return configs.get("retention.ms");
        }
        return null;
    }

    public String getCleanupPolicy() {
        if (configs != null) {
            return configs.get("cleanup.policy");
        }
        return null;
    }

    public String getCompressionType() {
        if (configs != null) {
            return configs.get("compression.type");
        }
        return null;
    }

    // Nested classes for partition and consumer group information
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PartitionInfo {
        private Integer partition;
        private Integer leader;
        private List<Integer> replicas;
        private List<Integer> inSyncReplicas;
        private Long earliestOffset;
        private Long latestOffset;
        private Long messageCount;

        // Constructors
        public PartitionInfo() {}

        public PartitionInfo(Integer partition, Integer leader) {
            this.partition = partition;
            this.leader = leader;
        }

        // Getters and Setters
        public Integer getPartition() {
            return partition;
        }

        public void setPartition(Integer partition) {
            this.partition = partition;
        }

        public Integer getLeader() {
            return leader;
        }

        public void setLeader(Integer leader) {
            this.leader = leader;
        }

        public List<Integer> getReplicas() {
            return replicas;
        }

        public void setReplicas(List<Integer> replicas) {
            this.replicas = replicas;
        }

        public List<Integer> getInSyncReplicas() {
            return inSyncReplicas;
        }

        public void setInSyncReplicas(List<Integer> inSyncReplicas) {
            this.inSyncReplicas = inSyncReplicas;
        }

        public Long getEarliestOffset() {
            return earliestOffset;
        }

        public void setEarliestOffset(Long earliestOffset) {
            this.earliestOffset = earliestOffset;
        }

        public Long getLatestOffset() {
            return latestOffset;
        }

        public void setLatestOffset(Long latestOffset) {
            this.latestOffset = latestOffset;
        }

        public Long getMessageCount() {
            return messageCount;
        }

        public void setMessageCount(Long messageCount) {
            this.messageCount = messageCount;
        }

        public boolean isInSync() {
            return inSyncReplicas != null && replicas != null && 
                   inSyncReplicas.size() == replicas.size();
        }

        @Override
        public String toString() {
            return "PartitionInfo{" +
                    "partition=" + partition +
                    ", leader=" + leader +
                    ", replicas=" + replicas +
                    ", inSyncReplicas=" + inSyncReplicas +
                    '}';
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ConsumerGroupInfo {
        private String groupId;
        private String state;
        private Integer memberCount;
        private Long lag;
        private List<String> members;

        // Constructors
        public ConsumerGroupInfo() {}

        public ConsumerGroupInfo(String groupId, String state) {
            this.groupId = groupId;
            this.state = state;
        }

        // Getters and Setters
        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public Integer getMemberCount() {
            return memberCount;
        }

        public void setMemberCount(Integer memberCount) {
            this.memberCount = memberCount;
        }

        public Long getLag() {
            return lag;
        }

        public void setLag(Long lag) {
            this.lag = lag;
        }

        public List<String> getMembers() {
            return members;
        }

        public void setMembers(List<String> members) {
            this.members = members;
        }

        public boolean isActive() {
            return "Stable".equalsIgnoreCase(state);
        }

        @Override
        public String toString() {
            return "ConsumerGroupInfo{" +
                    "groupId='" + groupId + '\'' +
                    ", state='" + state + '\'' +
                    ", memberCount=" + memberCount +
                    ", lag=" + lag +
                    '}';
        }
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicInfo topicInfo = (TopicInfo) o;
        return Objects.equals(name, topicInfo.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "TopicInfo{" +
                "name='" + name + '\'' +
                ", partitionCount=" + partitionCount +
                ", replicationFactor=" + replicationFactor +
                ", internal=" + internal +
                ", totalMessages=" + totalMessages +
                '}';
    }
}
