package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.AccountTransaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Reactive repository for AccountTransaction entities
 */
@Repository
public interface AccountTransactionRepository extends ReactiveCrudRepository<AccountTransaction, UUID> {

    /**
     * Find transactions by account ID
     */
    Flux<AccountTransaction> findByAccountId(UUID accountId);

    /**
     * Find transactions by account ID and date range
     */
    @Query("SELECT * FROM account_transactions WHERE account_id = :accountId AND booking_date BETWEEN :startDate AND :endDate ORDER BY transaction_time DESC")
    Flux<AccountTransaction> findByAccountIdAndDateRange(UUID accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Find transactions by UETR
     */
    Flux<AccountTransaction> findByUetr(UUID uetr);

    /**
     * Find transactions by end-to-end ID
     */
    Flux<AccountTransaction> findByEndToEndId(String endToEndId);

    /**
     * Find transactions by transaction type
     */
    Flux<AccountTransaction> findByAccountIdAndTransactionType(UUID accountId, String transactionType);

    /**
     * Get recent transactions for an account (last 100)
     */
    @Query("SELECT * FROM account_transactions WHERE account_id = :accountId ORDER BY transaction_time DESC LIMIT :limit")
    Flux<AccountTransaction> findRecentTransactions(UUID accountId, int limit);

    /**
     * Calculate total debits for account in date range
     */
    @Query("SELECT COALESCE(SUM(ABS(amount)), 0) FROM account_transactions WHERE account_id = :accountId AND transaction_type = 'DEBIT' AND booking_date BETWEEN :startDate AND :endDate")
    Mono<Double> calculateTotalDebits(UUID accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculate total credits for account in date range
     */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM account_transactions WHERE account_id = :accountId AND transaction_type = 'CREDIT' AND booking_date BETWEEN :startDate AND :endDate")
    Mono<Double> calculateTotalCredits(UUID accountId, LocalDate startDate, LocalDate endDate);
}
