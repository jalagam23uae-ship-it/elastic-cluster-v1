package com.fednow.iso20022.domain.pacs;

import com.fednow.iso20022.domain.common.*;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Pacs007 - FI To FI Payment Reversal
 *
 * ISO 20022 message for reversing a payment at the interbank level.
 * Used when a financial institution needs to reverse a payment that was
 * previously sent through FedNow.
 *
 * Difference from pacs.004 (Payment Return):
 * - pacs.007: Reversal request initiated by the debtor agent (originating bank)
 * - pacs.004: Return initiated by the creditor agent (receiving bank)
 *
 * Common Scenarios:
 * - Bank detects error after sending payment
 * - Customer requests immediate reversal
 * - Compliance issue identified
 * - Fraud detected by originating bank
 *
 * Flow:
 * Debtor Agent → pacs.007 (reversal request) → FedNow → Creditor Agent
 * Response: pacs.002 (acceptance/rejection of reversal)
 */
@Value
@Builder
public class Pacs007 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Original group information from the payment being reversed.
     */
    OriginalGroupInformation originalGroupInformation;

    /**
     * Reversal transaction information.
     */
    List<ReversalTransactionInformation> reversalTransaction;

    /**
     * Original group information.
     */
    @Value
    @Builder
    public static class OriginalGroupInformation {
        String originalMessageId;
        String originalMessageNameIdentification;
        ZonedDateTime originalCreationDateTime;
        Integer numberOfTransactions;
        Amount controlSum;
    }

    /**
     * Reversal transaction information.
     */
    @Value
    @Builder
    public static class ReversalTransactionInformation {

        /**
         * Reversal identification assigned by the instructing party.
         */
        String reversalId;

        /**
         * Original instruction ID from pacs.008.
         */
        String originalInstructionId;

        /**
         * Original end-to-end ID from pacs.008.
         */
        String originalEndToEndId;

        /**
         * Original transaction ID from pacs.008.
         */
        String originalTransactionId;

        /**
         * Original UETR from pacs.008.
         */
        String originalUetr;

        /**
         * Amount to be reversed (typically same as original).
         */
        Amount reversedInterbankSettlementAmount;

        /**
         * Interbank settlement date for the reversal.
         */
        String interbankSettlementDate;

        /**
         * Settlement information.
         */
        SettlementInformation settlementInformation;

        /**
         * Payment type information.
         */
        PaymentTypeInformation paymentTypeInformation;

        /**
         * Instructing agent (originating bank requesting reversal).
         */
        AgentIdentification instructingAgent;

        /**
         * Instructed agent (receiving bank).
         */
        AgentIdentification instructedAgent;

        /**
         * Reason for the reversal.
         */
        List<ReversalReasonInformation> reversalReasonInformation;

        /**
         * Original transaction reference.
         */
        OriginalTransactionReference originalTransactionReference;
    }

    /**
     * Payment type information.
     */
    @Value
    @Builder
    public static class PaymentTypeInformation {
        String instructionPriority;
        String clearingChannel;
        String serviceLevel;
        String localInstrument;
        String categoryPurpose;
    }

    /**
     * Reversal reason information.
     */
    @Value
    @Builder
    public static class ReversalReasonInformation {

        /**
         * Party that originated the reversal request.
         */
        PartyIdentification originator;

        /**
         * Reason for reversal.
         */
        ReversalReason reason;

        /**
         * Additional information.
         */
        List<String> additionalInformation;
    }

    /**
     * Reversal reason.
     */
    @Value
    @Builder
    public static class ReversalReason {

        /**
         * Reason code.
         * Common codes:
         * - CUST: Customer request
         * - DUPL: Duplicate payment
         * - FRAD: Fraudulent origin
         * - TECH: Technical problems
         * - AC01: Incorrect account number
         * - AC04: Closed account
         * - AG01: Transaction forbidden
         * - AM04: Insufficient funds
         */
        String reasonCode;

        /**
         * Additional reason information.
         */
        List<String> additionalReasonInformation;
    }

    /**
     * Original transaction reference.
     */
    @Value
    @Builder
    public static class OriginalTransactionReference {
        Amount interbankSettlementAmount;
        String interbankSettlementDate;
        PaymentTypeInformation paymentTypeInformation;
        PartyIdentification debtor;
        AccountIdentification debtorAccount;
        AgentIdentification debtorAgent;
        PartyIdentification creditor;
        AccountIdentification creditorAccount;
        AgentIdentification creditorAgent;
        RemittanceInformation remittanceInformation;
    }

    /**
     * Helper methods for creating common reversal scenarios.
     */
    public static class ReversalScenarios {

        /**
         * Creates a customer-requested reversal.
         */
        public static ReversalReasonInformation customerRequest(
                PartyIdentification customer, String explanation) {
            return ReversalReasonInformation.builder()
                    .originator(customer)
                    .reason(ReversalReason.builder()
                            .reasonCode("CUST")
                            .additionalReasonInformation(List.of(
                                    "Reversal requested by customer"))
                            .build())
                    .additionalInformation(List.of(explanation))
                    .build();
        }

        /**
         * Creates a fraud-detected reversal.
         */
        public static ReversalReasonInformation fraudDetected(
                PartyIdentification originator, String fraudDetails) {
            return ReversalReasonInformation.builder()
                    .originator(originator)
                    .reason(ReversalReason.builder()
                            .reasonCode("FRAD")
                            .additionalReasonInformation(List.of(
                                    "Fraudulent transaction - immediate reversal required"))
                            .build())
                    .additionalInformation(List.of(fraudDetails))
                    .build();
        }

        /**
         * Creates a duplicate payment reversal.
         */
        public static ReversalReasonInformation duplicatePayment(
                PartyIdentification originator) {
            return ReversalReasonInformation.builder()
                    .originator(originator)
                    .reason(ReversalReason.builder()
                            .reasonCode("DUPL")
                            .additionalReasonInformation(List.of(
                                    "Duplicate payment sent"))
                            .build())
                    .additionalInformation(List.of(
                            "Payment was processed twice due to system error"))
                    .build();
        }

        /**
         * Creates a technical error reversal.
         */
        public static ReversalReasonInformation technicalError(
                PartyIdentification originator, String errorDescription) {
            return ReversalReasonInformation.builder()
                    .originator(originator)
                    .reason(ReversalReason.builder()
                            .reasonCode("TECH")
                            .additionalReasonInformation(List.of(
                                    "Technical processing error"))
                            .build())
                    .additionalInformation(List.of(errorDescription))
                    .build();
        }
    }
}
