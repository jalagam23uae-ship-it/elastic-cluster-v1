package com.fednow.iso20022.domain.pacs;

import com.fednow.iso20022.domain.common.*;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Pacs028 - FI To FI Payment Status Request
 *
 * ISO 20022 message for requesting the status of a previously sent payment.
 * Used when a financial institution needs to query the current status of a payment
 * that was sent through FedNow.
 *
 * Purpose:
 * - Track payment status in real-time
 * - Investigate delayed or missing payments
 * - Customer inquiry support
 * - Reconciliation and exception handling
 *
 * Response Message:
 * - pacs.002 (Payment Status Report) is sent in response
 *
 * Common Scenarios:
 * 1. Customer calls asking "where is my payment?"
 * 2. Expected pacs.002 response not received
 * 3. System recovery after outage
 * 4. End-of-day reconciliation discrepancies
 * 5. Fraud investigation requiring payment tracking
 *
 * FedNow Timing:
 * - Status requests can be sent anytime
 * - Responses typically within seconds
 * - Historical status available for 90+ days
 */
@Value
@Builder
public class Pacs028 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Original group information for the payment being queried.
     */
    OriginalGroupInformation originalGroupInformation;

    /**
     * Transaction information status (details of payment to query).
     */
    List<PaymentTransactionInformation> transactionInformationStatus;

    /**
     * Original group information.
     */
    @Value
    @Builder
    public static class OriginalGroupInformation {

        /**
         * Original message ID of the payment being queried.
         */
        String originalMessageId;

        /**
         * Original message name identification (e.g., "pacs.008.001.11").
         */
        String originalMessageNameIdentification;

        /**
         * Original creation date/time.
         */
        ZonedDateTime originalCreationDateTime;
    }

    /**
     * Payment transaction information to query.
     */
    @Value
    @Builder
    public static class PaymentTransactionInformation {

        /**
         * Status request identification.
         */
        String statusRequestId;

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
         * Instructing agent (original sender).
         */
        AgentIdentification instructingAgent;

        /**
         * Instructed agent (original receiver).
         */
        AgentIdentification instructedAgent;

        /**
         * Original transaction reference (optional additional details).
         */
        OriginalTransactionReference originalTransactionReference;
    }

    /**
     * Original transaction reference.
     */
    @Value
    @Builder
    public static class OriginalTransactionReference {

        /**
         * Original amount.
         */
        Amount interbankSettlementAmount;

        /**
         * Original settlement date.
         */
        String interbankSettlementDate;

        /**
         * Original debtor.
         */
        PartyIdentification debtor;

        /**
         * Original debtor account.
         */
        AccountIdentification debtorAccount;

        /**
         * Original creditor.
         */
        PartyIdentification creditor;

        /**
         * Original creditor account.
         */
        AccountIdentification creditorAccount;
    }

    /**
     * Helper methods for creating common status request scenarios.
     */
    public static class StatusRequestScenarios {

        /**
         * Creates a status request by UETR.
         */
        public static Pacs028 requestByUetr(
                String uetr,
                AgentIdentification requestingAgent,
                String requestMessageId) {

            return Pacs028.builder()
                    .groupHeader(GroupHeader.builder()
                            .messageId(requestMessageId)
                            .creationDateTime(ZonedDateTime.now())
                            .instructingAgent(requestingAgent)
                            .build())
                    .transactionInformationStatus(List.of(
                            PaymentTransactionInformation.builder()
                                    .statusRequestId("SREQ-" + System.currentTimeMillis())
                                    .originalUetr(uetr)
                                    .instructingAgent(requestingAgent)
                                    .build()))
                    .build();
        }

        /**
         * Creates a status request by End-to-End ID.
         */
        public static Pacs028 requestByEndToEndId(
                String endToEndId,
                String originalMessageId,
                ZonedDateTime originalCreationTime,
                AgentIdentification requestingAgent,
                String requestMessageId) {

            return Pacs028.builder()
                    .groupHeader(GroupHeader.builder()
                            .messageId(requestMessageId)
                            .creationDateTime(ZonedDateTime.now())
                            .instructingAgent(requestingAgent)
                            .build())
                    .originalGroupInformation(OriginalGroupInformation.builder()
                            .originalMessageId(originalMessageId)
                            .originalMessageNameIdentification("pacs.008.001.11")
                            .originalCreationDateTime(originalCreationTime)
                            .build())
                    .transactionInformationStatus(List.of(
                            PaymentTransactionInformation.builder()
                                    .statusRequestId("SREQ-" + System.currentTimeMillis())
                                    .originalEndToEndId(endToEndId)
                                    .instructingAgent(requestingAgent)
                                    .build()))
                    .build();
        }

        /**
         * Creates a status request by Transaction ID.
         */
        public static Pacs028 requestByTransactionId(
                String transactionId,
                String instructionId,
                AgentIdentification requestingAgent,
                AgentIdentification respondingAgent,
                String requestMessageId) {

            return Pacs028.builder()
                    .groupHeader(GroupHeader.builder()
                            .messageId(requestMessageId)
                            .creationDateTime(ZonedDateTime.now())
                            .instructingAgent(requestingAgent)
                            .instructedAgent(respondingAgent)
                            .build())
                    .transactionInformationStatus(List.of(
                            PaymentTransactionInformation.builder()
                                    .statusRequestId("SREQ-" + System.currentTimeMillis())
                                    .originalTransactionId(transactionId)
                                    .originalInstructionId(instructionId)
                                    .instructingAgent(requestingAgent)
                                    .instructedAgent(respondingAgent)
                                    .build()))
                    .build();
        }

        /**
         * Creates a bulk status request for multiple payments.
         */
        public static Pacs028 bulkRequest(
                List<String> uetrs,
                AgentIdentification requestingAgent,
                String requestMessageId) {

            List<PaymentTransactionInformation> requests = uetrs.stream()
                    .map(uetr -> PaymentTransactionInformation.builder()
                            .statusRequestId("SREQ-" + System.currentTimeMillis())
                            .originalUetr(uetr)
                            .instructingAgent(requestingAgent)
                            .build())
                    .toList();

            return Pacs028.builder()
                    .groupHeader(GroupHeader.builder()
                            .messageId(requestMessageId)
                            .creationDateTime(ZonedDateTime.now())
                            .instructingAgent(requestingAgent)
                            .build())
                    .transactionInformationStatus(requests)
                    .build();
        }
    }
}
