package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.Investigation;
import com.fednow.iso20022.entity.enums.InvestigationStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for Investigation entities
 */
@Repository
public interface InvestigationRepository extends ReactiveCrudRepository<Investigation, UUID> {

    /**
     * Find investigation by investigation ID
     */
    Mono<Investigation> findByInvestigationId(String investigationId);

    /**
     * Find investigations by status
     */
    Flux<Investigation> findByInvestigationStatus(InvestigationStatus investigationStatus);

    /**
     * Find investigations by type
     */
    Flux<Investigation> findByInvestigationType(String investigationType);

    /**
     * Find investigations by original UETR
     */
    Flux<Investigation> findByOriginalUetr(UUID originalUetr);

    /**
     * Find investigations by requester BIC
     */
    Flux<Investigation> findByRequesterBic(String requesterBic);

    /**
     * Find investigations by responder BIC
     */
    Flux<Investigation> findByResponderBic(String responderBic);

    /**
     * Find investigations by priority
     */
    Flux<Investigation> findByPriority(String priority);

    /**
     * Find investigations with SLA breach
     */
    @Query("SELECT * FROM investigations WHERE sla_deadline < CURRENT_TIMESTAMP AND investigation_status NOT IN ('RESOLVED', 'CLOSED') ORDER BY sla_deadline")
    Flux<Investigation> findInvestigationsWithSlaB reach();

    /**
     * Find open investigations (OPEN or PENDING status)
     */
    @Query("SELECT * FROM investigations WHERE investigation_status IN ('OPEN', 'PENDING') ORDER BY opened_at DESC")
    Flux<Investigation> findOpenInvestigations();

    /**
     * Count investigations by status
     */
    Mono<Long> countByInvestigationStatus(InvestigationStatus investigationStatus);
}
