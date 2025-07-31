package org.marsem.kafka.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.marsem.kafka.model.Connection;
import org.marsem.kafka.model.KafkaMessage;
import org.marsem.kafka.model.TopicInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing Kafka topics and retrieving topic information.
 * 
 * This service provides comprehensive topic management functionality including
 * topic discovery, metadata retrieval, partition information, and consumer group details.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Service
public class TopicManagementService {

    private static final Logger logger = LoggerFactory.getLogger(TopicManagementService.class);

    private final KafkaConnectionService connectionService;

    @Autowired
    public TopicManagementService(KafkaConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    /**
     * Get all topics for a connection.
     */
    public CompletableFuture<List<TopicInfo>> getTopics(Long connectionId) {
        logger.info("Getting topics for connection: {}", connectionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                try (AdminClient adminClient = connectionService.createAdminClient(connection)) {
                    // List topics
                    ListTopicsResult listTopicsResult = adminClient.listTopics(new ListTopicsOptions().listInternal(false));
                    Set<String> topicNames = listTopicsResult.names().get(30, TimeUnit.SECONDS);

                    // Get topic descriptions
                    DescribeTopicsResult describeResult = adminClient.describeTopics(topicNames);
                    Map<String, TopicDescription> topicDescriptions = describeResult.allTopicNames().get(30, TimeUnit.SECONDS);

                    // Get topic configurations
                    Collection<ConfigResource> configResources = topicNames.stream()
                            .map(name -> new ConfigResource(ConfigResource.Type.TOPIC, name))
                            .collect(Collectors.toList());
                    
                    DescribeConfigsResult configResult = adminClient.describeConfigs(configResources);
                    Map<ConfigResource, Config> configs = configResult.all().get(30, TimeUnit.SECONDS);

                    // Convert to TopicInfo objects
                    List<TopicInfo> topics = new ArrayList<>();
                    for (String topicName : topicNames) {
                        TopicInfo topicInfo = createTopicInfo(topicName, topicDescriptions.get(topicName), configs, connection);
                        topics.add(topicInfo);
                    }

                    logger.info("Retrieved {} topics for connection: {}", topics.size(), connectionId);
                    return topics;
                }

            } catch (Exception e) {
                logger.error("Failed to get topics for connection: {}", connectionId, e);
                throw new RuntimeException("Failed to retrieve topics: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get detailed information for a specific topic.
     */
    public CompletableFuture<TopicInfo> getTopicInfo(Long connectionId, String topicName) {
        logger.info("Getting topic info for: {} on connection: {}", topicName, connectionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                try (AdminClient adminClient = connectionService.createAdminClient(connection)) {
                    // Get topic description
                    DescribeTopicsResult describeResult = adminClient.describeTopics(Collections.singletonList(topicName));
                    TopicDescription topicDescription = describeResult.topicNameValues().get(topicName).get(30, TimeUnit.SECONDS);

                    // Get topic configuration
                    ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
                    DescribeConfigsResult configResult = adminClient.describeConfigs(Collections.singletonList(configResource));
                    Map<ConfigResource, Config> configs = configResult.all().get(30, TimeUnit.SECONDS);

                    // Create detailed topic info
                    TopicInfo topicInfo = createTopicInfo(topicName, topicDescription, configs, connection);
                    
                    // Add partition-level details
                    addPartitionDetails(topicInfo, connection);
                    
                    // Add consumer group information
                    addConsumerGroupInfo(topicInfo, adminClient);

                    logger.info("Retrieved detailed info for topic: {}", topicName);
                    return topicInfo;
                }

            } catch (Exception e) {
                logger.error("Failed to get topic info for: {} on connection: {}", topicName, connectionId, e);
                throw new RuntimeException("Failed to retrieve topic info: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get consumer groups for a topic.
     */
    public CompletableFuture<List<TopicInfo.ConsumerGroupInfo>> getTopicConsumerGroups(Long connectionId, String topicName) {
        logger.info("Getting consumer groups for topic: {} on connection: {}", topicName, connectionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                try (AdminClient adminClient = connectionService.createAdminClient(connection)) {
                    return getConsumerGroupsForTopic(adminClient, topicName);
                }

            } catch (Exception e) {
                logger.error("Failed to get consumer groups for topic: {} on connection: {}", topicName, connectionId, e);
                throw new RuntimeException("Failed to retrieve consumer groups: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Get partition offsets for a topic.
     */
    public CompletableFuture<Map<Integer, Map<String, Long>>> getTopicOffsets(Long connectionId, String topicName) {
        logger.info("Getting offsets for topic: {} on connection: {}", topicName, connectionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                Map<String, Object> consumerConfig = createConsumerConfig(connection);
                
                try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerConfig)) {
                    // Get topic partitions
                    List<org.apache.kafka.common.PartitionInfo> partitionInfos = consumer.partitionsFor(topicName);
                    
                    Map<Integer, Map<String, Long>> offsetInfo = new HashMap<>();
                    
                    for (org.apache.kafka.common.PartitionInfo partitionInfo : partitionInfos) {
                        TopicPartition partition = new TopicPartition(topicName, partitionInfo.partition());
                        
                        // Get earliest and latest offsets
                        Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(Collections.singletonList(partition));
                        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(partition));
                        
                        Map<String, Long> partitionOffsets = new HashMap<>();
                        partitionOffsets.put("earliest", beginningOffsets.get(partition));
                        partitionOffsets.put("latest", endOffsets.get(partition));
                        partitionOffsets.put("messageCount", endOffsets.get(partition) - beginningOffsets.get(partition));
                        
                        offsetInfo.put(partitionInfo.partition(), partitionOffsets);
                    }

                    logger.info("Retrieved offset info for {} partitions of topic: {}", offsetInfo.size(), topicName);
                    return offsetInfo;
                }

            } catch (Exception e) {
                logger.error("Failed to get offsets for topic: {} on connection: {}", topicName, connectionId, e);
                throw new RuntimeException("Failed to retrieve topic offsets: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Create a new topic.
     */
    public CompletableFuture<Map<String, Object>> createTopic(Long connectionId, String topicName, 
                                                             int partitions, short replicationFactor, 
                                                             Map<String, String> configs) {
        logger.info("Creating topic: {} with {} partitions on connection: {}", topicName, partitions, connectionId);

        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = new HashMap<>();
            
            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                try (AdminClient adminClient = connectionService.createAdminClient(connection)) {
                    NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
                    
                    if (configs != null && !configs.isEmpty()) {
                        newTopic.configs(configs);
                    }

                    CreateTopicsResult createResult = adminClient.createTopics(Collections.singletonList(newTopic));
                    createResult.all().get(30, TimeUnit.SECONDS);

                    result.put("success", true);
                    result.put("topicName", topicName);
                    result.put("partitions", partitions);
                    result.put("replicationFactor", replicationFactor);
                    result.put("message", "Topic created successfully");

                    logger.info("Created topic: {} successfully", topicName);
                }

            } catch (Exception e) {
                logger.error("Failed to create topic: {} on connection: {}", topicName, connectionId, e);
                result.put("success", false);
                result.put("error", e.getMessage());
            }

            return result;
        });
    }

    /**
     * Delete a topic.
     */
    public CompletableFuture<Map<String, Object>> deleteTopic(Long connectionId, String topicName) {
        logger.info("Deleting topic: {} on connection: {}", topicName, connectionId);

        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = new HashMap<>();
            
            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                try (AdminClient adminClient = connectionService.createAdminClient(connection)) {
                    DeleteTopicsResult deleteResult = adminClient.deleteTopics(Collections.singletonList(topicName));
                    deleteResult.all().get(30, TimeUnit.SECONDS);

                    result.put("success", true);
                    result.put("topicName", topicName);
                    result.put("message", "Topic deleted successfully");

                    logger.info("Deleted topic: {} successfully", topicName);
                }

            } catch (Exception e) {
                logger.error("Failed to delete topic: {} on connection: {}", topicName, connectionId, e);
                result.put("success", false);
                result.put("error", e.getMessage());
            }

            return result;
        });
    }

    /**
     * Update topic configuration.
     */
    public CompletableFuture<Map<String, Object>> updateTopicConfig(Long connectionId, String topicName,
                                                                   Map<String, String> configs) {
        logger.info("Updating configuration for topic: {} on connection: {}", topicName, connectionId);

        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = new HashMap<>();

            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                try (AdminClient adminClient = connectionService.createAdminClient(connection)) {
                    ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);

                    // Convert configs to AlterConfigOp
                    Collection<AlterConfigOp> alterOps = configs.entrySet().stream()
                            .map(entry -> new AlterConfigOp(
                                new ConfigEntry(entry.getKey(), entry.getValue()),
                                AlterConfigOp.OpType.SET))
                            .collect(Collectors.toList());

                    Map<ConfigResource, Collection<AlterConfigOp>> alterConfigs =
                        Collections.singletonMap(configResource, alterOps);

                    AlterConfigsResult alterResult = adminClient.incrementalAlterConfigs(alterConfigs);
                    alterResult.all().get(30, TimeUnit.SECONDS);

                    result.put("success", true);
                    result.put("topicName", topicName);
                    result.put("updatedConfigs", configs);
                    result.put("message", "Topic configuration updated successfully");

                    logger.info("Updated configuration for topic: {} successfully", topicName);
                }

            } catch (Exception e) {
                logger.error("Failed to update configuration for topic: {} on connection: {}", topicName, connectionId, e);
                result.put("success", false);
                result.put("error", e.getMessage());
            }

            return result;
        });
    }

    /**
     * Create TopicInfo from Kafka metadata.
     */
    private TopicInfo createTopicInfo(String topicName, TopicDescription description, 
                                     Map<ConfigResource, Config> configs, Connection connection) {
        TopicInfo topicInfo = new TopicInfo();
        topicInfo.setName(topicName);
        topicInfo.setPartitionCount(description.partitions().size());
        topicInfo.setInternal(description.isInternal());

        // Set replication factor from first partition
        if (!description.partitions().isEmpty()) {
            topicInfo.setReplicationFactor((short) description.partitions().get(0).replicas().size());
        }

        // Convert partition information
        List<TopicInfo.PartitionInfo> partitionInfos = description.partitions().stream()
                .map(this::convertPartitionInfo)
                .collect(Collectors.toList());
        topicInfo.setPartitions(partitionInfos);

        // Convert configuration
        ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
        Config config = configs.get(configResource);
        if (config != null) {
            Map<String, String> configMap = config.entries().stream()
                    .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value));
            topicInfo.setConfigs(configMap);
        }

        return topicInfo;
    }

    /**
     * Convert Kafka PartitionInfo to our PartitionInfo.
     */
    private TopicInfo.PartitionInfo convertPartitionInfo(TopicPartitionInfo kafkaPartitionInfo) {
        TopicInfo.PartitionInfo partitionInfo = new TopicInfo.PartitionInfo();
        partitionInfo.setPartition(kafkaPartitionInfo.partition());
        partitionInfo.setLeader(kafkaPartitionInfo.leader().id());
        
        List<Integer> replicas = kafkaPartitionInfo.replicas().stream()
                .map(node -> node.id())
                .collect(Collectors.toList());
        partitionInfo.setReplicas(replicas);
        
        List<Integer> inSyncReplicas = kafkaPartitionInfo.isr().stream()
                .map(node -> node.id())
                .collect(Collectors.toList());
        partitionInfo.setInSyncReplicas(inSyncReplicas);
        
        return partitionInfo;
    }

    /**
     * Add partition-level details including offsets.
     */
    private void addPartitionDetails(TopicInfo topicInfo, Connection connection) {
        try {
            Map<String, Object> consumerConfig = createConsumerConfig(connection);
            
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerConfig)) {
                List<org.apache.kafka.common.PartitionInfo> partitionInfos = consumer.partitionsFor(topicInfo.getName());
                
                long totalMessages = 0;
                long earliestOffset = Long.MAX_VALUE;
                long latestOffset = 0;
                
                for (org.apache.kafka.common.PartitionInfo partitionInfo : partitionInfos) {
                    TopicPartition partition = new TopicPartition(topicInfo.getName(), partitionInfo.partition());
                    
                    Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(Collections.singletonList(partition));
                    Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(partition));
                    
                    long partitionEarliest = beginningOffsets.get(partition);
                    long partitionLatest = endOffsets.get(partition);
                    long partitionMessages = partitionLatest - partitionEarliest;
                    
                    // Update partition info
                    TopicInfo.PartitionInfo partInfo = topicInfo.getPartitions().stream()
                            .filter(p -> p.getPartition().equals(partitionInfo.partition()))
                            .findFirst()
                            .orElse(null);
                    
                    if (partInfo != null) {
                        partInfo.setEarliestOffset(partitionEarliest);
                        partInfo.setLatestOffset(partitionLatest);
                        partInfo.setMessageCount(partitionMessages);
                    }
                    
                    // Update topic totals
                    totalMessages += partitionMessages;
                    earliestOffset = Math.min(earliestOffset, partitionEarliest);
                    latestOffset = Math.max(latestOffset, partitionLatest);
                }
                
                topicInfo.setTotalMessages(totalMessages);
                topicInfo.setEarliestOffset(earliestOffset == Long.MAX_VALUE ? 0 : earliestOffset);
                topicInfo.setLatestOffset(latestOffset);
            }
            
        } catch (Exception e) {
            logger.warn("Failed to add partition details for topic: {}", topicInfo.getName(), e);
        }
    }

    /**
     * Add consumer group information.
     */
    private void addConsumerGroupInfo(TopicInfo topicInfo, AdminClient adminClient) {
        try {
            List<TopicInfo.ConsumerGroupInfo> consumerGroups = getConsumerGroupsForTopic(adminClient, topicInfo.getName());
            topicInfo.setConsumerGroups(consumerGroups);
        } catch (Exception e) {
            logger.warn("Failed to add consumer group info for topic: {}", topicInfo.getName(), e);
        }
    }

    /**
     * Get consumer groups for a specific topic.
     */
    private List<TopicInfo.ConsumerGroupInfo> getConsumerGroupsForTopic(AdminClient adminClient, String topicName) throws Exception {
        // List all consumer groups
        ListConsumerGroupsResult listGroupsResult = adminClient.listConsumerGroups();
        Collection<ConsumerGroupListing> groups = listGroupsResult.all().get(30, TimeUnit.SECONDS);

        List<TopicInfo.ConsumerGroupInfo> consumerGroups = new ArrayList<>();

        for (ConsumerGroupListing group : groups) {
            try {
                // Describe consumer group
                DescribeConsumerGroupsResult describeResult = adminClient.describeConsumerGroups(
                        Collections.singletonList(group.groupId()));
                ConsumerGroupDescription groupDescription = describeResult.all().get(30, TimeUnit.SECONDS).get(group.groupId());

                // Check if group is consuming from this topic
                boolean consumesFromTopic = groupDescription.members().stream()
                        .anyMatch(member -> member.assignment().topicPartitions().stream()
                                .anyMatch(tp -> tp.topic().equals(topicName)));

                if (consumesFromTopic) {
                    TopicInfo.ConsumerGroupInfo consumerGroupInfo = new TopicInfo.ConsumerGroupInfo();
                    consumerGroupInfo.setGroupId(group.groupId());
                    consumerGroupInfo.setState(groupDescription.state().toString());
                    consumerGroupInfo.setMemberCount(groupDescription.members().size());

                    // Get member IDs
                    List<String> members = groupDescription.members().stream()
                            .map(MemberDescription::consumerId)
                            .collect(Collectors.toList());
                    consumerGroupInfo.setMembers(members);

                    // Calculate lag for this topic
                    long totalLag = calculateConsumerLag(adminClient, group.groupId(), topicName);
                    consumerGroupInfo.setLag(totalLag);

                    consumerGroups.add(consumerGroupInfo);
                }

            } catch (Exception e) {
                logger.warn("Failed to get info for consumer group: {}", group.groupId(), e);
            }
        }

        return consumerGroups;
    }

    /**
     * Calculate consumer lag for a group and topic.
     */
    private long calculateConsumerLag(AdminClient adminClient, String groupId, String topicName) {
        try {
            // Get consumer group offsets
            ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(groupId);
            Map<TopicPartition, OffsetAndMetadata> offsets = offsetsResult.partitionsToOffsetAndMetadata().get(30, TimeUnit.SECONDS);

            long totalLag = 0;

            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
                TopicPartition partition = entry.getKey();
                
                if (partition.topic().equals(topicName)) {
                    // Calculate lag (simplified implementation)
                    // In a real implementation, you would need to get the latest offsets
                    // and compare with consumer offsets. For now, we'll return 0.
                    totalLag += 0;
                }
            }

            return totalLag;

        } catch (Exception e) {
            logger.warn("Failed to calculate lag for group: {} topic: {}", groupId, topicName, e);
            return 0;
        }
    }

    /**
     * Create consumer configuration for offset queries.
     */
    private Map<String, Object> createConsumerConfig(Connection connection) {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connection.getBootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-web-app-offset-reader-" + UUID.randomUUID());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // Security configuration
        if (connection.getSecurityProtocol() != null) {
            config.put("security.protocol", connection.getSecurityProtocol().getProtocol());
        }
        
        if (connection.getSaslMechanism() != null) {
            config.put("sasl.mechanism", connection.getSaslMechanism().getMechanism());
            
            if (connection.getSaslJaasConfig() != null) {
                config.put("sasl.jaas.config", connection.getSaslJaasConfig());
            } else if (connection.getUsername() != null && connection.getPassword() != null) {
                String jaasConfig = String.format(
                    "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
                    connection.getUsername(), connection.getPassword()
                );
                config.put("sasl.jaas.config", jaasConfig);
            }
        }
        
        return config;
    }

    /**
     * Browse messages in a topic.
     */
    public CompletableFuture<List<KafkaMessage>> browseMessages(Long connectionId, String topicName,
                                                               Integer partition, String startOffset, Integer limit) {
        logger.info("Browsing messages for topic: {} on connection: {}", topicName, connectionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                Map<String, Object> props = createConsumerConfig(connection);
                props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-web-app-browser-" + System.currentTimeMillis());
                props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
                props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, limit);

                try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                    List<TopicPartition> partitions = new ArrayList<>();

                    if (partition != null) {
                        // Browse specific partition
                        partitions.add(new TopicPartition(topicName, partition));
                    } else {
                        // Browse all partitions
                        List<PartitionInfo> partitionInfos = consumer.partitionsFor(topicName);
                        if (partitionInfos != null) {
                            for (PartitionInfo partitionInfo : partitionInfos) {
                                partitions.add(new TopicPartition(topicName, partitionInfo.partition()));
                            }
                        }
                    }

                    if (partitions.isEmpty()) {
                        logger.warn("No partitions found for topic: {}", topicName);
                        return new ArrayList<>();
                    }

                    consumer.assign(partitions);

                    // Set offset position
                    if ("earliest".equals(startOffset)) {
                        consumer.seekToBeginning(partitions);
                    } else if ("latest".equals(startOffset)) {
                        consumer.seekToEnd(partitions);
                    } else {
                        try {
                            long offset = Long.parseLong(startOffset);
                            for (TopicPartition tp : partitions) {
                                consumer.seek(tp, offset);
                            }
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid offset format: {}, using latest", startOffset);
                            consumer.seekToEnd(partitions);
                        }
                    }

                    List<KafkaMessage> messages = new ArrayList<>();
                    int pollAttempts = 0;
                    int maxPollAttempts = 10; // Maximum number of poll attempts

                    while (messages.size() < limit && pollAttempts < maxPollAttempts) {
                        ConsumerRecords<String, String> records = consumer.poll(java.time.Duration.ofSeconds(2));

                        if (records.isEmpty()) {
                            pollAttempts++;
                            continue;
                        }

                        for (ConsumerRecord<String, String> record : records) {
                            KafkaMessage message = convertToKafkaMessage(record);
                            messages.add(message);

                            if (messages.size() >= limit) {
                                break;
                            }
                        }

                        pollAttempts++;
                    }

                    logger.info("Retrieved {} messages from topic: {}", messages.size(), topicName);
                    return messages;
                }

            } catch (Exception e) {
                logger.error("Failed to browse messages for topic: {} on connection: {}", topicName, connectionId, e);
                throw new RuntimeException("Failed to browse messages: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Convert ConsumerRecord to KafkaMessage.
     */
    private KafkaMessage convertToKafkaMessage(ConsumerRecord<String, String> record) {
        KafkaMessage message = new KafkaMessage();
        message.setTopic(record.topic());
        message.setPartition(record.partition());
        message.setOffset(record.offset());
        message.setKey(record.key());
        message.setValue(record.value());
        message.setTimestamp(java.time.Instant.ofEpochMilli(record.timestamp()));
        message.setSerializedKeySize(record.serializedKeySize());
        message.setSerializedValueSize(record.serializedValueSize());
        message.setLeaderEpoch(record.leaderEpoch().orElse(null));
        return message;
    }

    /**
     * Get topic statistics including message count.
     */
    public CompletableFuture<Map<String, Object>> getTopicStats(Long connectionId, String topicName) {
        logger.info("Getting topic statistics for: {} on connection: {}", topicName, connectionId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = connectionService.getConnectionById(connectionId)
                        .orElseThrow(() -> new IllegalArgumentException("Connection not found: " + connectionId));

                Map<String, Object> config = createConsumerConfig(connection);
                config.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-web-app-stats-" + System.currentTimeMillis());
                config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

                try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(config)) {
                    List<PartitionInfo> partitionInfos = consumer.partitionsFor(topicName);
                    if (partitionInfos == null || partitionInfos.isEmpty()) {
                        logger.warn("No partitions found for topic: {}", topicName);
                        return createEmptyStats(topicName);
                    }

                    List<TopicPartition> partitions = new ArrayList<>();
                    for (PartitionInfo partitionInfo : partitionInfos) {
                        partitions.add(new TopicPartition(topicName, partitionInfo.partition()));
                    }

                    consumer.assign(partitions);

                    // Get earliest offsets
                    Map<TopicPartition, Long> earliestOffsets = consumer.beginningOffsets(partitions);

                    // Get latest offsets
                    Map<TopicPartition, Long> latestOffsets = consumer.endOffsets(partitions);

                    // Calculate total message count
                    long totalMessages = 0;
                    Map<Integer, Map<String, Long>> partitionStats = new HashMap<>();

                    for (TopicPartition partition : partitions) {
                        long earliest = earliestOffsets.getOrDefault(partition, 0L);
                        long latest = latestOffsets.getOrDefault(partition, 0L);
                        long messageCount = latest - earliest;
                        totalMessages += messageCount;

                        Map<String, Long> partitionInfo = new HashMap<>();
                        partitionInfo.put("earliestOffset", earliest);
                        partitionInfo.put("latestOffset", latest);
                        partitionInfo.put("messageCount", messageCount);
                        partitionStats.put(partition.partition(), partitionInfo);
                    }

                    Map<String, Object> stats = new HashMap<>();
                    stats.put("topic", topicName);
                    stats.put("partitionCount", partitions.size());
                    stats.put("totalMessages", totalMessages);
                    stats.put("partitionStats", partitionStats);
                    stats.put("timestamp", java.time.Instant.now());

                    logger.info("Retrieved statistics for topic: {} - {} total messages across {} partitions",
                               topicName, totalMessages, partitions.size());
                    return stats;
                }

            } catch (Exception e) {
                logger.error("Failed to get topic statistics for: {} on connection: {}", topicName, connectionId, e);
                throw new RuntimeException("Failed to get topic statistics: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Create empty stats when topic is not found.
     */
    private Map<String, Object> createEmptyStats(String topicName) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("topic", topicName);
        stats.put("partitionCount", 0);
        stats.put("totalMessages", 0L);
        stats.put("partitionStats", new HashMap<>());
        stats.put("timestamp", java.time.Instant.now());
        return stats;
    }
}
