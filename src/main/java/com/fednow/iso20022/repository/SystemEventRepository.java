package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.SystemEvent;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Reactive repository for SystemEvent entities
 */
@Repository
public interface SystemEventRepository extends ReactiveCrudRepository<SystemEvent, UUID> {

    /**
     * Find system events by event code
     */
    Flux<SystemEvent> findByEventCode(String eventCode);

    /**
     * Find system events by severity
     */
    Flux<SystemEvent> findBySeverity(String severity);

    /**
     * Find system events by related message ID
     */
    Flux<SystemEvent> findByRelatedMessageId(UUID relatedMessageId);

    /**
     * Find fatal system events
     */
    @Query("SELECT * FROM system_events WHERE severity = 'FATAL' ORDER BY event_time DESC")
    Flux<SystemEvent> findFatalEvents();

    /**
     * Find error events
     */
    @Query("SELECT * FROM system_events WHERE severity IN ('ERROR', 'FATAL') ORDER BY event_time DESC LIMIT :limit")
    Flux<SystemEvent> findErrorEvents(int limit);

    /**
     * Find system events in time range
     */
    @Query("SELECT * FROM system_events WHERE event_time BETWEEN :startTime AND :endTime ORDER BY event_time DESC")
    Flux<SystemEvent> findByEventTimeBetween(Instant startTime, Instant endTime);

    /**
     * Find recent system events
     */
    @Query("SELECT * FROM system_events ORDER BY event_time DESC LIMIT :limit")
    Flux<SystemEvent> findRecentEvents(int limit);

    /**
     * Count system events by severity
     */
    Mono<Long> countBySeverity(String severity);
}
