package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.Account;
import com.fednow.iso20022.entity.enums.AccountStatus;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for Account entities
 */
@Repository
public interface AccountRepository extends ReactiveCrudRepository<Account, UUID> {

    /**
     * Find account by account number (IBAN)
     */
    Mono<Account> findByAccountNumber(String accountNumber);

    /**
     * Find accounts by institution BIC
     */
    Flux<Account> findByInstitutionBic(String institutionBic);

    /**
     * Find accounts by status
     */
    Flux<Account> findByAccountStatus(AccountStatus accountStatus);

    /**
     * Find accounts by account holder name
     */
    Flux<Account> findByAccountHolderNameContainingIgnoreCase(String accountHolderName);

    /**
     * Find accounts with OFAC issues
     */
    @Query("SELECT * FROM accounts WHERE ofac_status != 'CLEAR' ORDER BY ofac_last_check DESC")
    Flux<Account> findAccountsWithOfacIssues();

    /**
     * Find accounts with high fraud scores
     */
    @Query("SELECT * FROM accounts WHERE fraud_score >= :minScore ORDER BY fraud_score DESC")
    Flux<Account> findAccountsWithHighFraudScore(int minScore);

    /**
     * Find active accounts by institution
     */
    Flux<Account> findByInstitutionBicAndAccountStatus(String institutionBic, AccountStatus accountStatus);
}
