package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.Reversal;
import com.fednow.iso20022.entity.enums.ReversalStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for Reversal entities
 */
@Repository
public interface ReversalRepository extends ReactiveCrudRepository<Reversal, UUID> {

    /**
     * Find reversal by reversal ID
     */
    Mono<Reversal> findByReversalId(String reversalId);

    /**
     * Find reversals by original message ID
     */
    Flux<Reversal> findByOriginalMessageId(UUID originalMessageId);

    /**
     * Find reversals by original UETR
     */
    Flux<Reversal> findByOriginalUetr(UUID originalUetr);

    /**
     * Find reversals by status
     */
    Flux<Reversal> findByReversalStatus(ReversalStatus reversalStatus);

    /**
     * Find reversals outside 15-second FedNow window
     */
    @Query("SELECT * FROM reversals WHERE within_window = false ORDER BY created_at DESC")
    Flux<Reversal> findReversalsOutsideWindow();

    /**
     * Find recent reversals (last 100)
     */
    @Query("SELECT * FROM reversals ORDER BY created_at DESC LIMIT 100")
    Flux<Reversal> findRecentReversals();

    /**
     * Count reversals by status
     */
    Mono<Long> countByReversalStatus(ReversalStatus reversalStatus);
}
