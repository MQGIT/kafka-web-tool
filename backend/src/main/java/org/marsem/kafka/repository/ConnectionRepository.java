package org.marsem.kafka.repository;

import org.marsem.kafka.model.Connection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Connection entities.
 * 
 * Provides data access methods for managing Kafka connection configurations
 * with custom queries for common operations.
 * 
 * @author MarSem.org
 * @version 2.0.0
 */
@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    /**
     * Find a connection by its name.
     */
    Optional<Connection> findByName(String name);

    /**
     * Find all active connections.
     */
    List<Connection> findByActiveTrue();

    /**
     * Find all inactive connections.
     */
    List<Connection> findByActiveFalse();

    /**
     * Find connections by security protocol.
     */
    List<Connection> findBySecurityProtocol(String securityProtocol);

    /**
     * Find connections created by a specific user.
     */
    List<Connection> findByCreatedBy(String createdBy);

    /**
     * Find connections created after a specific date.
     */
    List<Connection> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find top 5 most recent connections.
     */
    List<Connection> findTop5ByOrderByCreatedAtDesc();

    /**
     * Check if a connection name already exists (case-insensitive).
     */
    @Query("SELECT COUNT(c) > 0 FROM Connection c WHERE LOWER(c.name) = LOWER(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);

    /**
     * Check if a connection name exists excluding a specific ID (for updates).
     */
    @Query("SELECT COUNT(c) > 0 FROM Connection c WHERE LOWER(c.name) = LOWER(:name) AND c.id != :id")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("id") Long id);

    /**
     * Find connections with similar bootstrap servers.
     */
    @Query("SELECT c FROM Connection c WHERE c.bootstrapServers LIKE %:servers%")
    List<Connection> findByBootstrapServersContaining(@Param("servers") String servers);

    /**
     * Get connection statistics.
     */
    @Query("""
        SELECT new map(
            COUNT(c) as total,
            SUM(CASE WHEN c.active = true THEN 1 ELSE 0 END) as active,
            SUM(CASE WHEN c.active = false THEN 1 ELSE 0 END) as inactive,
            COUNT(DISTINCT c.securityProtocol) as securityProtocols
        )
        FROM Connection c
        """)
    List<Object> getConnectionStatistics();

    /**
     * Find recently updated connections.
     */
    @Query("SELECT c FROM Connection c WHERE c.updatedAt >= :since ORDER BY c.updatedAt DESC")
    List<Connection> findRecentlyUpdated(@Param("since") LocalDateTime since);

    /**
     * Find connections by multiple criteria.
     */
    @Query("""
        SELECT c FROM Connection c 
        WHERE (:active IS NULL OR c.active = :active)
        AND (:securityProtocol IS NULL OR c.securityProtocol = :securityProtocol)
        AND (:createdBy IS NULL OR c.createdBy = :createdBy)
        ORDER BY c.name
        """)
    List<Connection> findByCriteria(
        @Param("active") Boolean active,
        @Param("securityProtocol") String securityProtocol,
        @Param("createdBy") String createdBy
    );
}
