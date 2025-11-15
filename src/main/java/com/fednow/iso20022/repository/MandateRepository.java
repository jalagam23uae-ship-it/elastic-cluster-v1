package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.Mandate;
import com.fednow.iso20022.entity.enums.MandateStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reactive repository for Mandate entities
 */
@Repository
public interface MandateRepository extends ReactiveCrudRepository<Mandate, UUID> {

    /**
     * Find mandate by mandate ID
     */
    Mono<Mandate> findByMandateId(String mandateId);

    /**
     * Find mandates by status
     */
    Flux<Mandate> findByMandateStatus(MandateStatus mandateStatus);

    /**
     * Find mandates by debtor account
     */
    Flux<Mandate> findByDebtorAccount(String debtorAccount);

    /**
     * Find mandates by creditor account
     */
    Flux<Mandate> findByCreditorAccount(String creditorAccount);

    /**
     * Find expiring mandates (within next N days)
     */
    @Query("SELECT * FROM mandates WHERE expiration_date IS NOT NULL AND expiration_date BETWEEN CURRENT_DATE AND CURRENT_DATE + :days ORDER BY expiration_date")
    Flux<Mandate> findExpiringMandates(int days);

    /**
     * Find active mandates for debtor
     */
    Flux<Mandate> findByDebtorAccountAndMandateStatus(String debtorAccount, MandateStatus mandateStatus);
}
