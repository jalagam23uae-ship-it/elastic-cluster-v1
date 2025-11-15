package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.Conversion;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for Conversion entities
 */
@Repository
public interface ConversionRepository extends ReactiveCrudRepository<Conversion, UUID> {

    /**
     * Find conversions by source message ID
     */
    Flux<Conversion> findBySourceMessageId(UUID sourceMessageId);

    /**
     * Find conversions by target message ID
     */
    Flux<Conversion> findByTargetMessageId(UUID targetMessageId);

    /**
     * Find conversions by converter name
     */
    Flux<Conversion> findByConverterName(String converterName);

    /**
     * Find conversions by status
     */
    Flux<Conversion> findByStatus(String status);

    /**
     * Find failed conversions
     */
    @Query("SELECT * FROM conversions WHERE status = 'FAILED' ORDER BY created_at DESC LIMIT :limit")
    Flux<Conversion> findFailedConversions(int limit);

    /**
     * Find recent conversions
     */
    @Query("SELECT * FROM conversions ORDER BY created_at DESC LIMIT :limit")
    Flux<Conversion> findRecentConversions(int limit);

    /**
     * Calculate average conversion time for a converter
     */
    @Query("SELECT AVG(conversion_time_ms) FROM conversions WHERE converter_name = :converterName AND status = 'SUCCESS'")
    Mono<Double> calculateAverageConversionTime(String converterName);

    /**
     * Count conversions by converter name and status
     */
    Mono<Long> countByConverterNameAndStatus(String converterName, String status);
}
