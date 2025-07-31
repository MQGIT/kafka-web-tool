package org.marsem.kafka.repository;

import org.marsem.kafka.model.ConsumerSession;
import org.marsem.kafka.model.ConsumerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ConsumerSession entities.
 * 
 * Provides data access methods for managing Kafka consumer sessions
 * with custom queries for session lifecycle management.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Repository
public interface ConsumerSessionRepository extends JpaRepository<ConsumerSession, Long> {

    /**
     * Find a consumer session by its session ID.
     */
    Optional<ConsumerSession> findBySessionId(String sessionId);

    /**
     * Find all active consumer sessions.
     */
    List<ConsumerSession> findByStatus(ConsumerStatus status);

    /**
     * Find consumer sessions by connection ID.
     */
    List<ConsumerSession> findByConnectionId(Long connectionId);

    /**
     * Find consumer sessions by topic.
     */
    List<ConsumerSession> findByTopic(String topic);

    /**
     * Find consumer sessions by consumer group.
     */
    List<ConsumerSession> findByConsumerGroup(String consumerGroup);

    /**
     * Find consumer sessions by WebSocket session ID.
     */
    Optional<ConsumerSession> findByWebsocketSessionId(String websocketSessionId);

    /**
     * Find consumer sessions created by a specific user.
     */
    List<ConsumerSession> findByCreatedBy(String createdBy);

    /**
     * Find active consumer sessions for a specific connection.
     */
    @Query("SELECT cs FROM ConsumerSession cs WHERE cs.connectionId = :connectionId AND cs.status = 'RUNNING'")
    List<ConsumerSession> findActiveByConnectionId(@Param("connectionId") Long connectionId);

    /**
     * Find active consumer sessions for a specific topic.
     */
    @Query("SELECT cs FROM ConsumerSession cs WHERE cs.topic = :topic AND cs.status = 'RUNNING'")
    List<ConsumerSession> findActiveByTopic(@Param("topic") String topic);

    /**
     * Find consumer sessions that have been running for more than specified minutes.
     */
    @Query("""
        SELECT cs FROM ConsumerSession cs 
        WHERE cs.status = 'RUNNING' 
        AND cs.startedAt < :cutoffTime
        """)
    List<ConsumerSession> findLongRunningConsumers(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find consumer sessions created after a specific date.
     */
    List<ConsumerSession> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find top 10 most recent consumer sessions.
     */
    List<ConsumerSession> findTop10ByOrderByCreatedAtDesc();

    /**
     * Count active consumer sessions by connection.
     */
    @Query("SELECT COUNT(cs) FROM ConsumerSession cs WHERE cs.connectionId = :connectionId AND cs.status = 'RUNNING'")
    long countActiveByConnectionId(@Param("connectionId") Long connectionId);

    /**
     * Count consumer sessions by status.
     */
    long countByStatus(ConsumerStatus status);

    /**
     * Get consumer session statistics.
     */
    @Query("""
        SELECT new map(
            COUNT(cs) as total,
            SUM(CASE WHEN cs.status = 'RUNNING' THEN 1 ELSE 0 END) as running,
            SUM(CASE WHEN cs.status = 'PAUSED' THEN 1 ELSE 0 END) as paused,
            SUM(CASE WHEN cs.status = 'STOPPED' THEN 1 ELSE 0 END) as stopped,
            SUM(CASE WHEN cs.status = 'ERROR' THEN 1 ELSE 0 END) as error,
            SUM(cs.messagesConsumed) as totalMessagesConsumed
        )
        FROM ConsumerSession cs
        """)
    List<Object> getConsumerSessionStatistics();

    /**
     * Find consumer sessions with error status and their error messages.
     */
    @Query("SELECT cs FROM ConsumerSession cs WHERE cs.status = 'ERROR' AND cs.errorMessage IS NOT NULL")
    List<ConsumerSession> findErrorSessions();

    /**
     * Update consumer session status.
     */
    @Modifying
    @Query("UPDATE ConsumerSession cs SET cs.status = :status, cs.updatedAt = CURRENT_TIMESTAMP WHERE cs.sessionId = :sessionId")
    int updateStatusBySessionId(@Param("sessionId") String sessionId, @Param("status") ConsumerStatus status);

    /**
     * Update consumer session current offset.
     */
    @Modifying
    @Query("""
        UPDATE ConsumerSession cs 
        SET cs.currentOffset = :offset, cs.updatedAt = CURRENT_TIMESTAMP 
        WHERE cs.sessionId = :sessionId
        """)
    int updateCurrentOffsetBySessionId(@Param("sessionId") String sessionId, @Param("offset") Long offset);

    /**
     * Increment messages consumed count.
     */
    @Modifying
    @Query("""
        UPDATE ConsumerSession cs 
        SET cs.messagesConsumed = cs.messagesConsumed + 1, cs.updatedAt = CURRENT_TIMESTAMP 
        WHERE cs.sessionId = :sessionId
        """)
    int incrementMessagesConsumed(@Param("sessionId") String sessionId);

    /**
     * Clean up old finished consumer sessions.
     */
    @Modifying
    @Query("""
        DELETE FROM ConsumerSession cs 
        WHERE cs.status IN ('STOPPED', 'ERROR') 
        AND cs.stoppedAt < :cutoffTime
        """)
    int deleteOldFinishedSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find consumer sessions by multiple criteria.
     */
    @Query("""
        SELECT cs FROM ConsumerSession cs 
        WHERE (:connectionId IS NULL OR cs.connectionId = :connectionId)
        AND (:topic IS NULL OR cs.topic = :topic)
        AND (:status IS NULL OR cs.status = :status)
        AND (:createdBy IS NULL OR cs.createdBy = :createdBy)
        ORDER BY cs.createdAt DESC
        """)
    List<ConsumerSession> findByCriteria(
        @Param("connectionId") Long connectionId,
        @Param("topic") String topic,
        @Param("status") ConsumerStatus status,
        @Param("createdBy") String createdBy
    );
}
