package com.fednow.iso20022.domain.camt;

import com.fednow.iso20022.domain.common.*;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Camt056 - FI To FI Payment Cancellation Request
 *
 * ISO 20022 message for requesting the return/cancellation of a payment.
 * Used when a customer or financial institution needs to cancel a payment
 * that has already been sent.
 *
 * Common Scenarios:
 * - Customer requests payment cancellation
 * - Error detected after payment sent
 * - Fraud detected requiring immediate return
 * - Duplicate payment sent by mistake
 *
 * Flow:
 * Customer/Bank → camt.056 (cancellation request) → Bank/FedNow
 * Response: camt.029 (accept) or camt.087 (reject cancellation)
 * If accepted: pacs.004 (payment return) is generated
 */
@Value
@Builder
public class Camt056 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Underlying payment transaction to be cancelled.
     */
    List<UnderlyingTransaction> underlying;

    /**
     * Represents the underlying payment transaction to be cancelled.
     */
    @Value
    @Builder
    public static class UnderlyingTransaction {

        /**
         * Original group information from the payment being cancelled.
         */
        OriginalGroupInformation originalGroupInformation;

        /**
         * Original payment information ID.
         */
        String originalPaymentInformationId;

        /**
         * Original instruction ID.
         */
        String originalInstructionId;

        /**
         * Original end-to-end ID.
         */
        String originalEndToEndId;

        /**
         * Original transaction ID.
         */
        String originalTransactionId;

        /**
         * Original UETR.
         */
        String originalUetr;

        /**
         * Original interbank settlement amount.
         */
        Amount originalInterbankSettlementAmount;

        /**
         * Original interbank settlement date.
         */
        String originalInterbankSettlementDate;

        /**
         * Instructing agent (sender of original payment).
         */
        AgentIdentification instructingAgent;

        /**
         * Instructed agent (receiver of original payment).
         */
        AgentIdentification instructedAgent;

        /**
         * Reason for the cancellation request.
         */
        List<CancellationReasonInformation> cancellationReasonInformation;
    }

    /**
     * Original group information.
     */
    @Value
    @Builder
    public static class OriginalGroupInformation {
        String originalMessageId;
        String originalMessageNameIdentification;
        ZonedDateTime originalCreationDateTime;
    }

    /**
     * Cancellation reason information.
     */
    @Value
    @Builder
    public static class CancellationReasonInformation {

        /**
         * Originator of the cancellation request.
         */
        PartyIdentification originator;

        /**
         * Reason for cancellation.
         */
        CancellationReason reason;

        /**
         * Additional information explaining the cancellation.
         */
        List<String> additionalInformation;
    }

    /**
     * Cancellation reason.
     */
    @Value
    @Builder
    public static class CancellationReason {

        /**
         * Reason code.
         * Common codes:
         * - CUST: Customer request
         * - DUPL: Duplicate payment
         * - FRAD: Fraudulent transaction
         * - TECH: Technical error
         * - AGNT: Incorrect agent
         * - CURR: Incorrect currency
         * - CUTA: Requested by customer
         */
        String reasonCode;

        /**
         * Additional reason information.
         */
        List<String> additionalReasonInformation;
    }

    /**
     * Helper methods for creating common cancellation scenarios.
     */
    public static class CancellationScenarios {

        /**
         * Creates a customer-requested cancellation.
         */
        public static CancellationReasonInformation customerRequest(
                PartyIdentification customer, String explanation) {
            return CancellationReasonInformation.builder()
                    .originator(customer)
                    .reason(CancellationReason.builder()
                            .reasonCode("CUST")
                            .additionalReasonInformation(List.of(
                                    "Cancellation requested by customer"))
                            .build())
                    .additionalInformation(List.of(explanation))
                    .build();
        }

        /**
         * Creates a duplicate payment cancellation.
         */
        public static CancellationReasonInformation duplicatePayment(
                PartyIdentification originator) {
            return CancellationReasonInformation.builder()
                    .originator(originator)
                    .reason(CancellationReason.builder()
                            .reasonCode("DUPL")
                            .additionalReasonInformation(List.of(
                                    "Duplicate payment detected"))
                            .build())
                    .additionalInformation(List.of(
                            "This payment was sent twice by mistake"))
                    .build();
        }

        /**
         * Creates a fraud-detected cancellation.
         */
        public static CancellationReasonInformation fraudDetected(
                PartyIdentification originator, String fraudDetails) {
            return CancellationReasonInformation.builder()
                    .originator(originator)
                    .reason(CancellationReason.builder()
                            .reasonCode("FRAD")
                            .additionalReasonInformation(List.of(
                                    "Fraudulent transaction detected"))
                            .build())
                    .additionalInformation(List.of(fraudDetails))
                    .build();
        }

        /**
         * Creates a technical error cancellation.
         */
        public static CancellationReasonInformation technicalError(
                PartyIdentification originator, String errorDescription) {
            return CancellationReasonInformation.builder()
                    .originator(originator)
                    .reason(CancellationReason.builder()
                            .reasonCode("TECH")
                            .additionalReasonInformation(List.of(
                                    "Technical error in payment processing"))
                            .build())
                    .additionalInformation(List.of(errorDescription))
                    .build();
        }
    }
}
