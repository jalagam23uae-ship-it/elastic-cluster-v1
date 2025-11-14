package com.fednow.iso20022.converter.phase2;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.admi.Admi007;
import com.fednow.iso20022.domain.common.GroupHeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * AnyMessageToAdmi007Converter - Generic Receipt Acknowledgment Generator
 *
 * Converts any ISO 20022 message to admi.007 (Receipt Acknowledgement).
 *
 * This is a universal acknowledgment generator that confirms receipt of any
 * ISO 20022 message. It's the first response sent upon message receipt,
 * before any business processing begins.
 *
 * Purpose of admi.007:
 * 1. Confirms message was received
 * 2. Validates message structure (schema, format)
 * 3. Confirms authentication passed
 * 4. Provides receipt timestamp
 * 5. Does NOT indicate business acceptance (that's pacs.002)
 *
 * Response Timing:
 * - MUST send within 500ms of receipt
 * - Sent before business validation
 * - Sent before processing begins
 * - Critical for non-repudiation and audit trail
 *
 * Status Codes:
 * - ACPT: Message received and structure valid
 * - RJCT: Message rejected (schema, auth, duplicate, etc.)
 *
 * Message Flow:
 * Any Message → Receiver validates → admi.007 (receipt ack) → Sender
 *
 * Difference from Other Acknowledgments:
 * - admi.002: System event notification (technical problems)
 * - admi.007: Receipt acknowledgment (message received OK)
 * - pacs.002: Business response (payment accepted/rejected)
 */
@Slf4j
@Component
public class AnyMessageToAdmi007Converter extends AbstractMessageConverter<Object, Admi007> {

    public AnyMessageToAdmi007Converter() {
        super(Object.class, Admi007.class, "AnyMessageToAdmi007Converter");
    }

    @Override
    protected Mono<Admi007> doConvert(Object source, ConverterContext context) {
        logStep("Starting generic message → admi.007 conversion (receipt acknowledgment)");

        // Generate message ID for admi.007
        enrichContextWithIds(context, "ACK", false, false);

        return Mono.fromCallable(() -> {
            // Extract message information from context
            String originalMessageId = extractMessageId(source, context);
            String messageNameId = extractMessageNameId(source, context);
            ZonedDateTime originalCreationTime = extractCreationDateTime(source, context);

            // Determine receipt status (ACPT or RJCT)
            String receiptStatus = determineReceiptStatus(context);

            // Build admi.007
            Admi007 admi007 = Admi007.builder()
                    .groupHeader(buildGroupHeader(context))
                    .relatedReference(buildRelatedReference(
                            originalMessageId, messageNameId, originalCreationTime))
                    .receiptDetails(buildReceiptDetails(context, receiptStatus))
                    .build();

            logStep("Successfully created admi.007 receipt acknowledgment with status: " + receiptStatus);
            return admi007;
        });
    }

    /**
     * Extracts message ID from source message or context.
     *
     * @param source source message
     * @param context converter context
     * @return message ID
     */
    private String extractMessageId(Object source, ConverterContext context) {
        // Try to get from context first
        String messageId = context.getAttribute("originalMessageId", String.class);
        if (messageId != null) {
            return messageId;
        }

        // Try to extract from source if it has a GroupHeader
        if (source instanceof com.fednow.iso20022.domain.pacs.Pacs008) {
            return ((com.fednow.iso20022.domain.pacs.Pacs008) source).getGroupHeader().getMessageId();
        } else if (source instanceof com.fednow.iso20022.domain.pacs.Pacs002) {
            return ((com.fednow.iso20022.domain.pacs.Pacs002) source).getGroupHeader().getMessageId();
        } else if (source instanceof com.fednow.iso20022.domain.pacs.Pacs004) {
            return ((com.fednow.iso20022.domain.pacs.Pacs004) source).getGroupHeader().getMessageId();
        } else if (source instanceof com.fednow.iso20022.domain.pain.Pain001) {
            return ((com.fednow.iso20022.domain.pain.Pain001) source).getGroupHeader().getMessageId();
        } else if (source instanceof com.fednow.iso20022.domain.camt.Camt054) {
            return ((com.fednow.iso20022.domain.camt.Camt054) source).getGroupHeader().getMessageId();
        }

        // Default
        return "UNKNOWN-MSG-ID";
    }

    /**
     * Extracts message name identification.
     *
     * @param source source message
     * @param context converter context
     * @return message name ID
     */
    private String extractMessageNameId(Object source, ConverterContext context) {
        // Try to get from context first
        String messageNameId = context.getAttribute("originalMessageNameId", String.class);
        if (messageNameId != null) {
            return messageNameId;
        }

        // Determine from source class
        if (source instanceof com.fednow.iso20022.domain.pacs.Pacs008) {
            return "pacs.008.001.11";
        } else if (source instanceof com.fednow.iso20022.domain.pacs.Pacs002) {
            return "pacs.002.001.13";
        } else if (source instanceof com.fednow.iso20022.domain.pacs.Pacs004) {
            return "pacs.004.001.12";
        } else if (source instanceof com.fednow.iso20022.domain.pacs.Pacs007) {
            return "pacs.007.001.12";
        } else if (source instanceof com.fednow.iso20022.domain.pain.Pain001) {
            return "pain.001.001.11";
        } else if (source instanceof com.fednow.iso20022.domain.pain.Pain002) {
            return "pain.002.001.13";
        } else if (source instanceof com.fednow.iso20022.domain.camt.Camt054) {
            return "camt.054.001.11";
        } else if (source instanceof com.fednow.iso20022.domain.camt.Camt056) {
            return "camt.056.001.11";
        }

        return "unknown";
    }

    /**
     * Extracts creation date/time from source message.
     *
     * @param source source message
     * @param context converter context
     * @return creation date/time
     */
    private ZonedDateTime extractCreationDateTime(Object source, ConverterContext context) {
        // Try to get from context first
        ZonedDateTime creationTime = context.getAttribute("originalCreationDateTime", ZonedDateTime.class);
        if (creationTime != null) {
            return creationTime;
        }

        // Try to extract from source
        if (source instanceof com.fednow.iso20022.domain.pacs.Pacs008) {
            return ((com.fednow.iso20022.domain.pacs.Pacs008) source).getGroupHeader().getCreationDateTime();
        } else if (source instanceof com.fednow.iso20022.domain.pacs.Pacs002) {
            return ((com.fednow.iso20022.domain.pacs.Pacs002) source).getGroupHeader().getCreationDateTime();
        } else if (source instanceof com.fednow.iso20022.domain.pacs.Pacs004) {
            return ((com.fednow.iso20022.domain.pacs.Pacs004) source).getGroupHeader().getCreationDateTime();
        } else if (source instanceof com.fednow.iso20022.domain.pain.Pain001) {
            return ((com.fednow.iso20022.domain.pain.Pain001) source).getGroupHeader().getCreationDateTime();
        } else if (source instanceof com.fednow.iso20022.domain.camt.Camt054) {
            return ((com.fednow.iso20022.domain.camt.Camt054) source).getGroupHeader().getCreationDateTime();
        }

        // Default to current time
        return context.getCurrentTimestamp();
    }

    /**
     * Determines receipt status based on context validation.
     *
     * @param context converter context
     * @return ACPT or RJCT
     */
    private String determineReceiptStatus(ConverterContext context) {
        // Check if explicitly set in context
        String explicitStatus = context.getAttribute("receiptStatus", String.class);
        if (explicitStatus != null) {
            return explicitStatus;
        }

        // Check validation results
        if (context.getValidationResults() != null) {
            // If there are critical errors at technical level, reject
            if (context.hasCriticalErrors()) {
                return "RJCT";
            }
        }

        // Check for specific rejection reasons in context
        Boolean authenticationFailed = context.getAttribute("authenticationFailed", Boolean.class);
        if (Boolean.TRUE.equals(authenticationFailed)) {
            return "RJCT";
        }

        Boolean schemaValidationFailed = context.getAttribute("schemaValidationFailed", Boolean.class);
        if (Boolean.TRUE.equals(schemaValidationFailed)) {
            return "RJCT";
        }

        Boolean duplicateMessage = context.getAttribute("duplicateMessage", Boolean.class);
        if (Boolean.TRUE.equals(duplicateMessage)) {
            return "RJCT";
        }

        // Default to accepted
        return "ACPT";
    }

    /**
     * Builds group header for admi.007.
     *
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds related reference to original message.
     *
     * @param messageId original message ID
     * @param messageNameId message name identification
     * @param creationDateTime original creation time
     * @return related reference
     */
    private Admi007.RelatedReference buildRelatedReference(
            String messageId,
            String messageNameId,
            ZonedDateTime creationDateTime) {

        return Admi007.RelatedReference.builder()
                .messageId(messageId)
                .messageNameIdentification(messageNameId)
                .creationDateTime(creationDateTime)
                .build();
    }

    /**
     * Builds receipt details.
     *
     * @param context converter context
     * @param status receipt status (ACPT or RJCT)
     * @return receipt details
     */
    private Admi007.ReceiptDetails buildReceiptDetails(
            ConverterContext context,
            String status) {

        Admi007.ReceiptDetails.ReceiptDetailsBuilder builder = Admi007.ReceiptDetails.builder()
                .receiptTimestamp(context.getCurrentTimestamp())
                .status(status);

        if ("ACPT".equals(status)) {
            // Accepted - add success information
            builder.additionalInformation(List.of(
                    "Message received and structure validated successfully",
                    "Processing will begin"));
        } else {
            // Rejected - add reason
            builder.statusReason(buildRejectionReason(context))
                    .additionalInformation(List.of(
                            "Message rejected - please review and resubmit"));
        }

        return builder.build();
    }

    /**
     * Builds rejection reason based on context.
     *
     * @param context converter context
     * @return status reason
     */
    private Admi007.StatusReason buildRejectionReason(ConverterContext context) {
        // Check for specific rejection reasons
        Boolean authenticationFailed = context.getAttribute("authenticationFailed", Boolean.class);
        if (Boolean.TRUE.equals(authenticationFailed)) {
            return Admi007.StatusReason.builder()
                    .reasonCode("AUTHF")
                    .additionalReasonInformation(List.of(
                            "Authentication failed - invalid credentials"))
                    .build();
        }

        Boolean schemaValidationFailed = context.getAttribute("schemaValidationFailed", Boolean.class);
        String schemaError = context.getAttribute("schemaValidationError", String.class);
        if (Boolean.TRUE.equals(schemaValidationFailed)) {
            return Admi007.StatusReason.builder()
                    .reasonCode("SCHF")
                    .additionalReasonInformation(List.of(
                            "Schema validation failed",
                            schemaError != null ? schemaError : "Invalid message structure"))
                    .build();
        }

        Boolean duplicateMessage = context.getAttribute("duplicateMessage", Boolean.class);
        String originalReceiptTime = context.getAttribute("originalReceiptTime", String.class);
        if (Boolean.TRUE.equals(duplicateMessage)) {
            return Admi007.StatusReason.builder()
                    .reasonCode("DUPL")
                    .additionalReasonInformation(List.of(
                            "Duplicate message",
                            originalReceiptTime != null
                                    ? "Already received at " + originalReceiptTime
                                    : "Message already processed"))
                    .build();
        }

        // Generic rejection
        String rejectionReason = context.getAttribute("rejectionReason", String.class);
        return Admi007.StatusReason.builder()
                .reasonCode("MS03")
                .additionalReasonInformation(List.of(
                        rejectionReason != null ? rejectionReason : "Not specified"))
                .build();
    }

    /**
     * Helper method to create successful receipt acknowledgment.
     *
     * @param source source message
     * @param context converter context
     * @return Mono<Admi007>
     */
    public Mono<Admi007> createSuccessfulReceipt(Object source, ConverterContext context) {
        context.setAttribute("receiptStatus", "ACPT");
        return convert(source, context);
    }

    /**
     * Helper method to create schema validation failure acknowledgment.
     *
     * @param source source message
     * @param context converter context
     * @param validationError validation error message
     * @return Mono<Admi007>
     */
    public Mono<Admi007> createSchemaValidationFailure(
            Object source,
            ConverterContext context,
            String validationError) {
        context.setAttribute("receiptStatus", "RJCT");
        context.setAttribute("schemaValidationFailed", true);
        context.setAttribute("schemaValidationError", validationError);
        return convert(source, context);
    }

    /**
     * Helper method to create authentication failure acknowledgment.
     *
     * @param source source message
     * @param context converter context
     * @return Mono<Admi007>
     */
    public Mono<Admi007> createAuthenticationFailure(Object source, ConverterContext context) {
        context.setAttribute("receiptStatus", "RJCT");
        context.setAttribute("authenticationFailed", true);
        return convert(source, context);
    }

    /**
     * Helper method to create duplicate message acknowledgment.
     *
     * @param source source message
     * @param context converter context
     * @param originalReceiptTime when the duplicate was first received
     * @return Mono<Admi007>
     */
    public Mono<Admi007> createDuplicateMessage(
            Object source,
            ConverterContext context,
            String originalReceiptTime) {
        context.setAttribute("receiptStatus", "RJCT");
        context.setAttribute("duplicateMessage", true);
        context.setAttribute("originalReceiptTime", originalReceiptTime);
        return convert(source, context);
    }
}
