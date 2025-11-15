package com.fednow/iso20022.repository;

import com.fednow.iso20022.entity.AuditLog;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Reactive repository for AuditLog entities
 */
@Repository
public interface AuditLogRepository extends ReactiveCrudRepository<AuditLog, UUID> {

    /**
     * Find audit logs by message ID
     */
    Flux<AuditLog> findByMessageId(UUID messageId);

    /**
     * Find audit logs by action
     */
    Flux<AuditLog> findByAction(String action);

    /**
     * Find audit logs by actor
     */
    Flux<AuditLog> findByActor(String actor);

    /**
     * Find audit logs in time range
     */
    @Query("SELECT * FROM audit_log WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    Flux<AuditLog> findByTimestampBetween(Instant startTime, Instant endTime);

    /**
     * Find recent audit logs
     */
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    Flux<AuditLog> findRecentAuditLogs(int limit);

    /**
     * Find audit trail for a message (ordered by timestamp)
     */
    @Query("SELECT * FROM audit_log WHERE message_id = :messageId ORDER BY timestamp ASC")
    Flux<AuditLog> findMessageAuditTrail(UUID messageId);

    /**
     * Count audit logs by action
     */
    Mono<Long> countByAction(String action);
}
