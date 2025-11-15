package com.fednow.iso20022.repository;

import com.fednow.iso20022.entity.PaymentStatusReport;
import com.fednow.iso20022.entity.enums.PaymentStatusCode;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Reactive repository for PaymentStatusReport entities
 */
@Repository
public interface PaymentStatusReportRepository extends ReactiveCrudRepository<PaymentStatusReport, UUID> {

    /**
     * Find payment status report by message ID
     */
    Mono<PaymentStatusReport> findByMessageId(UUID messageId);

    /**
     * Find payment status reports by original message ID
     */
    Flux<PaymentStatusReport> findByOriginalMessageId(UUID originalMessageId);

    /**
     * Find payment status reports by status code
     */
    Flux<PaymentStatusReport> findByStatusCode(PaymentStatusCode statusCode);

    /**
     * Find rejected payments
     */
    @Query("SELECT * FROM payment_status_reports WHERE status_code = 'RJCT' ORDER BY created_at DESC")
    Flux<PaymentStatusReport> findRejectedPayments();

    /**
     * Find pending payments
     */
    @Query("SELECT * FROM payment_status_reports WHERE status_code = 'PDNG' ORDER BY created_at DESC")
    Flux<PaymentStatusReport> findPendingPayments();

    /**
     * Find payment status reports by reason code
     */
    Flux<PaymentStatusReport> findByReasonCode(String reasonCode);

    /**
     * Count payment status reports by status code
     */
    Mono<Long> countByStatusCode(PaymentStatusCode statusCode);
}
