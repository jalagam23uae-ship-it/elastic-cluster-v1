package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.RfpRequest;
import com.fednow.iso20022.entity.enums.RfpStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for RfpRequest entities
 */
@Repository
public interface RfpRequestRepository extends ReactiveCrudRepository<RfpRequest, UUID> {

    /**
     * Find RFP by RFP ID
     */
    Mono<RfpRequest> findByRfpId(String rfpId);

    /**
     * Find RFPs by status
     */
    Flux<RfpRequest> findByRfpStatus(RfpStatus rfpStatus);

    /**
     * Find RFPs by creditor account
     */
    Flux<RfpRequest> findByCreditorAccount(String creditorAccount);

    /**
     * Find RFPs by debtor account
     */
    Flux<RfpRequest> findByDebtorAccount(String debtorAccount);

    /**
     * Find pending RFPs for debtor
     */
    Flux<RfpRequest> findByDebtorAccountAndRfpStatus(String debtorAccount, RfpStatus rfpStatus);

    /**
     * Find expired RFPs
     */
    @Query("SELECT * FROM rfp_requests WHERE expiration_date < CURRENT_TIMESTAMP AND rfp_status = 'REQUESTED' ORDER BY expiration_date DESC")
    Flux<RfpRequest> findExpiredRfps();

    /**
     * Find RFPs expiring soon (within next N hours)
     */
    @Query("SELECT * FROM rfp_requests WHERE expiration_date IS NOT NULL AND expiration_date BETWEEN CURRENT_TIMESTAMP AND CURRENT_TIMESTAMP + INTERVAL ':hours hours' AND rfp_status = 'REQUESTED' ORDER BY expiration_date")
    Flux<RfpRequest> findRfpsExpiringSoon(int hours);
}
