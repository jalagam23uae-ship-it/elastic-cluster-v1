package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.ValidationError;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for ValidationError entities
 */
@Repository
public interface ValidationErrorRepository extends ReactiveCrudRepository<ValidationError, UUID> {

    /**
     * Find validation errors by message ID
     */
    Flux<ValidationError> findByMessageId(UUID messageId);

    /**
     * Find validation errors by error code
     */
    Flux<ValidationError> findByErrorCode(String errorCode);

    /**
     * Find validation errors by severity
     */
    Flux<ValidationError> findBySeverity(String severity);

    /**
     * Find validation errors by error category
     */
    Flux<ValidationError> findByErrorCategory(String errorCategory);

    /**
     * Find critical validation errors
     */
    @Query("SELECT * FROM validation_errors WHERE severity = 'CRITICAL' ORDER BY created_at DESC")
    Flux<ValidationError> findCriticalErrors();

    /**
     * Find validation errors for a message by severity
     */
    Flux<ValidationError> findByMessageIdAndSeverity(UUID messageId, String severity);

    /**
     * Count validation errors by message ID
     */
    Mono<Long> countByMessageId(UUID messageId);

    /**
     * Count validation errors by severity
     */
    Mono<Long> countBySeverity(String severity);
}
