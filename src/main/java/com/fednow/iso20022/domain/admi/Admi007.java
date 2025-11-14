package com.fednow.iso20022.domain.admi;

import com.fednow.iso20022.domain.common.GroupHeader;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Admi007 - Receipt Acknowledgement
 *
 * ISO 20022 administrative message acknowledging receipt of a message.
 * This is a lightweight technical acknowledgment that confirms a message
 * was received and passed initial validation.
 *
 * Purpose:
 * - Confirms message was received
 * - Confirms message structure is valid
 * - Provides receipt timestamp
 * - Does NOT indicate business acceptance (use pacs.002 for that)
 *
 * Timing:
 * - Must be sent immediately upon receipt (< 500ms)
 * - Sent before business validation
 * - Sent before processing begins
 *
 * Flow:
 * Any ISO 20022 Message → Receiver → admi.007 (receipt ack) → Sender
 *
 * Difference from other responses:
 * - admi.002: System event notification (problems/alerts)
 * - admi.007: Simple receipt acknowledgment (message received OK)
 * - pacs.002: Business acceptance/rejection (after processing)
 */
@Value
@Builder
public class Admi007 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Related reference to the original message being acknowledged.
     */
    RelatedReference relatedReference;

    /**
     * Receipt details.
     */
    ReceiptDetails receiptDetails;

    /**
     * Related reference information.
     */
    @Value
    @Builder
    public static class RelatedReference {

        /**
         * Message identification of the original message.
         */
        String messageId;

        /**
         * Message name identification (e.g., "pacs.008.001.11").
         */
        String messageNameIdentification;

        /**
         * Creation date/time of the original message.
         */
        ZonedDateTime creationDateTime;
    }

    /**
     * Receipt details.
     */
    @Value
    @Builder
    public static class ReceiptDetails {

        /**
         * Receipt timestamp (when message was received).
         */
        ZonedDateTime receiptTimestamp;

        /**
         * Status of the receipt.
         * - ACPT: Accepted (message structure valid)
         * - RJCT: Rejected (message structure invalid)
         */
        String status;

        /**
         * Status reason (if rejected).
         */
        StatusReason statusReason;

        /**
         * Additional receipt information.
         */
        List<String> additionalInformation;
    }

    /**
     * Status reason.
     */
    @Value
    @Builder
    public static class StatusReason {

        /**
         * Reason code.
         * Common codes:
         * - SCHF: Schema validation failed
         * - AUTHF: Authentication failed
         * - ENCF: Encryption/signature validation failed
         * - NOAS: No answer from customer
         * - DUPL: Duplicate message
         */
        String reasonCode;

        /**
         * Additional reason information.
         */
        List<String> additionalReasonInformation;
    }

    /**
     * Helper methods for creating common acknowledgment scenarios.
     */
    public static class AcknowledgmentScenarios {

        /**
         * Creates a successful receipt acknowledgment.
         */
        public static Admi007 successfulReceipt(
                String messageId,
                String messageNameId,
                ZonedDateTime originalCreationTime,
                String ackMessageId) {

            return Admi007.builder()
                    .groupHeader(GroupHeader.builder()
                            .messageId(ackMessageId)
                            .creationDateTime(ZonedDateTime.now())
                            .build())
                    .relatedReference(RelatedReference.builder()
                            .messageId(messageId)
                            .messageNameIdentification(messageNameId)
                            .creationDateTime(originalCreationTime)
                            .build())
                    .receiptDetails(ReceiptDetails.builder()
                            .receiptTimestamp(ZonedDateTime.now())
                            .status("ACPT")
                            .additionalInformation(List.of(
                                    "Message received and structure validated successfully"))
                            .build())
                    .build();
        }

        /**
         * Creates a schema validation failure acknowledgment.
         */
        public static Admi007 schemaValidationFailure(
                String messageId,
                String messageNameId,
                ZonedDateTime originalCreationTime,
                String ackMessageId,
                String validationError) {

            return Admi007.builder()
                    .groupHeader(GroupHeader.builder()
                            .messageId(ackMessageId)
                            .creationDateTime(ZonedDateTime.now())
                            .build())
                    .relatedReference(RelatedReference.builder()
                            .messageId(messageId)
                            .messageNameIdentification(messageNameId)
                            .creationDateTime(originalCreationTime)
                            .build())
                    .receiptDetails(ReceiptDetails.builder()
                            .receiptTimestamp(ZonedDateTime.now())
                            .status("RJCT")
                            .statusReason(StatusReason.builder()
                                    .reasonCode("SCHF")
                                    .additionalReasonInformation(List.of(
                                            "Schema validation failed",
                                            validationError))
                                    .build())
                            .additionalInformation(List.of(
                                    "Message rejected due to schema validation failure"))
                            .build())
                    .build();
        }

        /**
         * Creates an authentication failure acknowledgment.
         */
        public static Admi007 authenticationFailure(
                String messageId,
                String messageNameId,
                ZonedDateTime originalCreationTime,
                String ackMessageId) {

            return Admi007.builder()
                    .groupHeader(GroupHeader.builder()
                            .messageId(ackMessageId)
                            .creationDateTime(ZonedDateTime.now())
                            .build())
                    .relatedReference(RelatedReference.builder()
                            .messageId(messageId)
                            .messageNameIdentification(messageNameId)
                            .creationDateTime(originalCreationTime)
                            .build())
                    .receiptDetails(ReceiptDetails.builder()
                            .receiptTimestamp(ZonedDateTime.now())
                            .status("RJCT")
                            .statusReason(StatusReason.builder()
                                    .reasonCode("AUTHF")
                                    .additionalReasonInformation(List.of(
                                            "Authentication failed - invalid credentials"))
                                    .build())
                            .additionalInformation(List.of(
                                    "Message rejected due to authentication failure"))
                            .build())
                    .build();
        }

        /**
         * Creates a duplicate message acknowledgment.
         */
        public static Admi007 duplicateMessage(
                String messageId,
                String messageNameId,
                ZonedDateTime originalCreationTime,
                String ackMessageId,
                String originalReceiptTime) {

            return Admi007.builder()
                    .groupHeader(GroupHeader.builder()
                            .messageId(ackMessageId)
                            .creationDateTime(ZonedDateTime.now())
                            .build())
                    .relatedReference(RelatedReference.builder()
                            .messageId(messageId)
                            .messageNameIdentification(messageNameId)
                            .creationDateTime(originalCreationTime)
                            .build())
                    .receiptDetails(ReceiptDetails.builder()
                            .receiptTimestamp(ZonedDateTime.now())
                            .status("RJCT")
                            .statusReason(StatusReason.builder()
                                    .reasonCode("DUPL")
                                    .additionalReasonInformation(List.of(
                                            "Duplicate message - already received at " + originalReceiptTime))
                                    .build())
                            .additionalInformation(List.of(
                                    "Message rejected as duplicate"))
                            .build())
                    .build();
        }
    }
}
