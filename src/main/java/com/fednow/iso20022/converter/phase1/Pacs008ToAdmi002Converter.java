package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.admi.Admi002;
import com.fednow.iso20022.domain.pacs.Pacs008;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Pacs008ToAdmi002Converter - System Event Notification Converter
 *
 * Converts pacs.008 (or attempts to process it) to admi.002 (System Event Notification).
 *
 * This converter is used for TECHNICAL/SYSTEM-LEVEL events that occur BEFORE business validation:
 * - AUTHF: Authentication Failure
 * - ENCF: Encryption Failure
 * - SCHF: Schema Validation Failure
 * - SYSF: System Failure
 * - NETF: Network Failure
 * - CAPC: Capacity Critical
 * - CAPH: Capacity High
 * - QHGH: Queue High
 *
 * KEY DISTINCTION:
 * - admi.002: Technical issues BEFORE business validation (this converter)
 * - pacs.002: Business acceptance/rejection AFTER validation (Pacs008ToPacs002Converter)
 *
 * Response Time: <1 second for critical events
 *
 * Usage:
 * - Call this converter when technical issues prevent normal processing
 * - Event code determines severity and recommended action
 */
@Slf4j
@Component
public class Pacs008ToAdmi002Converter extends AbstractMessageConverter<Pacs008, Admi002> {

    public Pacs008ToAdmi002Converter() {
        super(Pacs008.class, Admi002.class, "Pacs008ToAdmi002Converter");
    }

    @Override
    protected Mono<Admi002> doConvert(Pacs008 source, ConverterContext context) {
        logStep("Starting pacs.008 → admi.002 conversion for system event");

        // Generate message ID for admi.002
        enrichContextWithIds(context, "EVT", false, false);

        return Mono.fromCallable(() -> {
            // Determine event reason from context
            Admi002.EventReason eventReason = determineEventReason(context);
            logStep("Generated system event: " + eventReason.getCode());

            // Build admi.002
            Admi002 admi002 = Admi002.builder()
                    .messageId(context.getGeneratedMessageId())
                    .creationDateTime(context.getCurrentTimestamp())
                    .relatedReference(buildRelatedReference(source, context))
                    .reason(eventReason)
                    .originalBusinessInstruction(buildOriginalBusinessInstruction(source, context))
                    .build();

            logStep("Successfully created admi.002 with event code: " + eventReason.getCode());
            return admi002;
        });
    }

    /**
     * Determines the event reason based on context attributes.
     *
     * @param context converter context
     * @return event reason
     */
    private Admi002.EventReason determineEventReason(ConverterContext context) {
        // Check for event type in context attributes
        String eventType = context.getAttribute("eventType", String.class);
        String eventDetails = context.getAttribute("eventDetails", String.class);

        if (eventType == null) {
            // Default to system failure if no type specified
            return Admi002.CommonEvents.systemFailure("Unknown", "No event type specified");
        }

        return switch (eventType) {
            case "AUTHF" -> Admi002.CommonEvents.authenticationFailure(
                    eventDetails != null ? eventDetails : "Authentication failed");

            case "ENCF" -> Admi002.CommonEvents.encryptionFailure(
                    eventDetails != null ? eventDetails : "Encryption/decryption failed");

            case "SCHF" -> Admi002.CommonEvents.schemaValidationFailure(
                    eventDetails != null ? eventDetails : "XML schema validation failed");

            case "SYSF" -> {
                String component = context.getAttribute("component", String.class);
                yield Admi002.CommonEvents.systemFailure(
                        component != null ? component : "System",
                        eventDetails != null ? eventDetails : "System failure");
            }

            case "NETF" -> Admi002.CommonEvents.networkFailure(
                    eventDetails != null ? eventDetails : "Network failure");

            case "CAPC" -> {
                Integer percentUsed = context.getAttribute("percentUsed", Integer.class);
                yield Admi002.CommonEvents.capacityCritical(
                        percentUsed != null ? percentUsed : 95);
            }

            case "CAPH" -> {
                Integer percentUsed = context.getAttribute("percentUsed", Integer.class);
                yield Admi002.CommonEvents.capacityHigh(
                        percentUsed != null ? percentUsed : 80);
            }

            case "QHGH" -> {
                String queueName = context.getAttribute("queueName", String.class);
                Integer percentFull = context.getAttribute("percentFull", Integer.class);
                yield Admi002.CommonEvents.queueHigh(
                        queueName != null ? queueName : "MessageQueue",
                        percentFull != null ? percentFull : 80);
            }

            case "DEGR" -> {
                String component = context.getAttribute("component", String.class);
                yield Admi002.CommonEvents.degradedPerformance(
                        component != null ? component : "System",
                        eventDetails != null ? eventDetails : "Performance degraded");
            }

            case "MAINT" -> {
                java.time.ZonedDateTime startTime = context.getAttribute("startTime",
                        java.time.ZonedDateTime.class);
                java.time.ZonedDateTime endTime = context.getAttribute("endTime",
                        java.time.ZonedDateTime.class);
                yield Admi002.CommonEvents.scheduledMaintenance(
                        startTime != null ? startTime : context.getCurrentTimestamp(),
                        endTime != null ? endTime : context.getCurrentTimestamp().plusHours(2));
            }

            case "RSTR" -> {
                String component = context.getAttribute("component", String.class);
                yield Admi002.CommonEvents.serviceRestored(
                        component != null ? component : "System");
            }

            case "NRML" -> Admi002.CommonEvents.normalOperations();

            default -> Admi002.EventReason.builder()
                    .code(eventType)
                    .severity("ERROR")
                    .additionalInformation(java.util.List.of(
                            eventDetails != null ? eventDetails : "Unknown error"))
                    .build();
        };
    }

    /**
     * Builds related reference to the original pacs.008.
     *
     * @param source original pacs.008
     * @param context converter context
     * @return related reference
     */
    private Admi002.RelatedReference buildRelatedReference(Pacs008 source,
                                                            ConverterContext context) {
        // Try to get first transaction for reference
        Pacs008.CreditTransferTransactionInformation firstTxn = null;
        if (source.getCreditTransferTransactionInformation() != null &&
                !source.getCreditTransferTransactionInformation().isEmpty()) {
            firstTxn = source.getCreditTransferTransactionInformation().get(0);
        }

        return Admi002.RelatedReference.builder()
                .messageId(source.getGroupHeader() != null
                        ? source.getGroupHeader().getMessageId()
                        : null)
                .messageNameIdentification("pacs.008.001.11")
                .creationDateTime(source.getGroupHeader() != null
                        ? source.getGroupHeader().getCreationDateTime()
                        : null)
                .transactionId(firstTxn != null && firstTxn.getPaymentIdentification() != null
                        ? firstTxn.getPaymentIdentification().getTransactionId()
                        : null)
                .uetr(firstTxn != null && firstTxn.getPaymentIdentification() != null
                        ? firstTxn.getPaymentIdentification().getUetr()
                        : null)
                .build();
    }

    /**
     * Builds original business instruction reference.
     *
     * @param source original pacs.008
     * @param context converter context
     * @return original business instruction
     */
    private Admi002.OriginalBusinessInstruction buildOriginalBusinessInstruction(
            Pacs008 source, ConverterContext context) {

        return Admi002.OriginalBusinessInstruction.builder()
                .messageId(source.getGroupHeader() != null
                        ? source.getGroupHeader().getMessageId()
                        : null)
                .messageNameIdentification("pacs.008.001.11")
                .creationDateTime(source.getGroupHeader() != null
                        ? source.getGroupHeader().getCreationDateTime()
                        : null)
                .instructingParty(source.getGroupHeader() != null
                        ? source.getGroupHeader().getInitiatingParty()
                        : null)
                .build();
    }

    /**
     * Helper method to create admi.002 for authentication failure.
     *
     * @param source pacs.008 that failed authentication
     * @param context converter context
     * @param details failure details
     * @return Mono<Admi002>
     */
    public Mono<Admi002> createAuthenticationFailureEvent(Pacs008 source,
                                                           ConverterContext context,
                                                           String details) {
        context.setAttribute("eventType", "AUTHF");
        context.setAttribute("eventDetails", details);
        return convert(source, context);
    }

    /**
     * Helper method to create admi.002 for encryption failure.
     *
     * @param source pacs.008 that failed encryption check
     * @param context converter context
     * @param details failure details
     * @return Mono<Admi002>
     */
    public Mono<Admi002> createEncryptionFailureEvent(Pacs008 source,
                                                       ConverterContext context,
                                                       String details) {
        context.setAttribute("eventType", "ENCF");
        context.setAttribute("eventDetails", details);
        return convert(source, context);
    }

    /**
     * Helper method to create admi.002 for schema validation failure.
     *
     * @param source pacs.008 that failed schema validation
     * @param context converter context
     * @param details validation failure details
     * @return Mono<Admi002>
     */
    public Mono<Admi002> createSchemaValidationFailureEvent(Pacs008 source,
                                                             ConverterContext context,
                                                             String details) {
        context.setAttribute("eventType", "SCHF");
        context.setAttribute("eventDetails", details);
        return convert(source, context);
    }

    /**
     * Helper method to create admi.002 for system failure.
     *
     * @param source pacs.008 being processed when failure occurred
     * @param context converter context
     * @param component component that failed
     * @param details failure details
     * @return Mono<Admi002>
     */
    public Mono<Admi002> createSystemFailureEvent(Pacs008 source,
                                                   ConverterContext context,
                                                   String component,
                                                   String details) {
        context.setAttribute("eventType", "SYSF");
        context.setAttribute("component", component);
        context.setAttribute("eventDetails", details);
        return convert(source, context);
    }

    /**
     * Helper method to create admi.002 for capacity warning.
     *
     * @param source pacs.008 being processed
     * @param context converter context
     * @param percentUsed percentage of capacity used
     * @return Mono<Admi002>
     */
    public Mono<Admi002> createCapacityWarningEvent(Pacs008 source,
                                                     ConverterContext context,
                                                     int percentUsed) {
        String eventType = percentUsed >= 95 ? "CAPC" : "CAPH";
        context.setAttribute("eventType", eventType);
        context.setAttribute("percentUsed", percentUsed);
        return convert(source, context);
    }
}
