package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.BicDirectory;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for BIC Directory entities
 */
@Repository
public interface BicDirectoryRepository extends ReactiveCrudRepository<BicDirectory, UUID> {

    /**
     * Find by BIC code
     */
    Mono<BicDirectory> findByBic(String bic);

    /**
     * Find by routing number
     */
    Mono<BicDirectory> findByRoutingNumber(String routingNumber);

    /**
     * Find all FedNow participants
     */
    @Query("SELECT * FROM bic_directory WHERE fednow_participant = true AND active = true ORDER BY institution_name")
    Flux<BicDirectory> findFedNowParticipants();

    /**
     * Find by country code
     */
    Flux<BicDirectory> findByCountryCodeAndActiveTrue(String countryCode);

    /**
     * Find active institutions
     */
    Flux<BicDirectory> findByActiveTrue();

    /**
     * Search by institution name
     */
    Flux<BicDirectory> findByInstitutionNameContainingIgnoreCaseAndActiveTrue(String institutionName);
}
