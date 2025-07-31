package org.marsem.kafka.repository;

import org.marsem.kafka.model.ConsumerMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for ConsumerMessage entities.
 * 
 * Provides data access methods for managing consumed Kafka messages
 * stored for consumer sessions.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Repository
public interface ConsumerMessageRepository extends JpaRepository<ConsumerMessage, Long> {

    /**
     * Find all messages for a specific consumer session.
     */
    List<ConsumerMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    /**
     * Find messages for a session with pagination.
     */
    Page<ConsumerMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);

    /**
     * Find messages for a session ordered by offset.
     */
    List<ConsumerMessage> findBySessionIdOrderByPartitionAscOffsetAsc(String sessionId);

    /**
     * Find messages for a specific topic and session.
     */
    List<ConsumerMessage> findBySessionIdAndTopicOrderByCreatedAtAsc(String sessionId, String topic);

    /**
     * Find messages for a specific partition and session.
     */
    List<ConsumerMessage> findBySessionIdAndTopicAndPartitionOrderByOffsetAsc(
            String sessionId, String topic, Integer partition);

    /**
     * Count messages for a session.
     */
    long countBySessionId(String sessionId);

    /**
     * Count messages for a topic and session.
     */
    long countBySessionIdAndTopic(String sessionId, String topic);

    /**
     * Find latest messages for a session (limit by count).
     */
    @Query("""
        SELECT cm FROM ConsumerMessage cm 
        WHERE cm.sessionId = :sessionId 
        ORDER BY cm.createdAt DESC
        """)
    List<ConsumerMessage> findLatestBySessionId(@Param("sessionId") String sessionId, Pageable pageable);

    /**
     * Find messages created after a specific time.
     */
    List<ConsumerMessage> findBySessionIdAndCreatedAtAfterOrderByCreatedAtAsc(
            String sessionId, LocalDateTime after);

    /**
     * Find messages by offset range.
     */
    @Query("""
        SELECT cm FROM ConsumerMessage cm 
        WHERE cm.sessionId = :sessionId 
        AND cm.topic = :topic 
        AND cm.partition = :partition 
        AND cm.offset BETWEEN :startOffset AND :endOffset
        ORDER BY cm.offset ASC
        """)
    List<ConsumerMessage> findByOffsetRange(
            @Param("sessionId") String sessionId,
            @Param("topic") String topic,
            @Param("partition") Integer partition,
            @Param("startOffset") Long startOffset,
            @Param("endOffset") Long endOffset);

    /**
     * Delete all messages for a session.
     */
    @Modifying
    @Query("DELETE FROM ConsumerMessage cm WHERE cm.sessionId = :sessionId")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * Delete messages older than specified date.
     */
    @Modifying
    @Query("DELETE FROM ConsumerMessage cm WHERE cm.createdAt < :cutoffDate")
    int deleteOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Delete messages for sessions that are older than specified date.
     */
    @Modifying
    @Query("""
        DELETE FROM ConsumerMessage cm 
        WHERE cm.sessionId IN (
            SELECT cs.sessionId FROM ConsumerSession cs 
            WHERE cs.createdAt < :cutoffDate
        )
        """)
    int deleteForOldSessions(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Find sessions with message count.
     */
    @Query("""
        SELECT cm.sessionId, COUNT(cm) 
        FROM ConsumerMessage cm 
        GROUP BY cm.sessionId
        """)
    List<Object[]> findSessionMessageCounts();

    /**
     * Find total message count by topic.
     */
    @Query("""
        SELECT cm.topic, COUNT(cm) 
        FROM ConsumerMessage cm 
        WHERE cm.sessionId = :sessionId 
        GROUP BY cm.topic
        """)
    List<Object[]> findTopicMessageCounts(@Param("sessionId") String sessionId);

    /**
     * Check if a specific message already exists (to prevent duplicates).
     */
    boolean existsBySessionIdAndTopicAndPartitionAndOffset(
            String sessionId, String topic, Integer partition, Long offset);

    /**
     * Find the latest offset for a session, topic, and partition.
     */
    @Query("""
        SELECT MAX(cm.offset) 
        FROM ConsumerMessage cm 
        WHERE cm.sessionId = :sessionId 
        AND cm.topic = :topic 
        AND cm.partition = :partition
        """)
    Long findMaxOffsetBySessionTopicPartition(
            @Param("sessionId") String sessionId,
            @Param("topic") String topic,
            @Param("partition") Integer partition);

    /**
     * Find messages with specific key pattern.
     */
    @Query("""
        SELECT cm FROM ConsumerMessage cm 
        WHERE cm.sessionId = :sessionId 
        AND cm.key LIKE :keyPattern 
        ORDER BY cm.createdAt ASC
        """)
    List<ConsumerMessage> findBySessionIdAndKeyPattern(
            @Param("sessionId") String sessionId,
            @Param("keyPattern") String keyPattern);

    /**
     * Find messages with specific value pattern.
     */
    @Query("""
        SELECT cm FROM ConsumerMessage cm 
        WHERE cm.sessionId = :sessionId 
        AND cm.value LIKE :valuePattern 
        ORDER BY cm.createdAt ASC
        """)
    List<ConsumerMessage> findBySessionIdAndValuePattern(
            @Param("sessionId") String sessionId,
            @Param("valuePattern") String valuePattern);
}
