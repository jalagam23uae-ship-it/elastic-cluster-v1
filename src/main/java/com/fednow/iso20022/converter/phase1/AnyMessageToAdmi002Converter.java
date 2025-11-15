package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.admi.Admi002;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * AnyMessageToAdmi002Converter - Generic System Error Handler
 *
 * Converts ANY ISO 20022 message to admi.002 (System Event Notification) for generic errors.
 *
 * This is a CRITICAL converter for handling unexpected system errors that occur during
 * message processing, regardless of the message type.
 *
 * Usage Scenarios:
 * 1. System Failures: Database down, service unavailable
 * 2. Network Failures: Connection lost, timeout
 * 3. Configuration Errors: Missing configuration, invalid setup
 * 4. Resource Exhaustion: Out of memory, disk full
 * 5. Unexpected Exceptions: NullPointerException, etc.
 *
 * Event Codes:
 * - SYSF: System Failure (database, service failures)
 * - NETF: Network Failure (connection, timeout)
 * - CONF: Configuration Failure (missing config, invalid setup)
 * - RESF: Resource Failure (memory, disk, CPU)
 * - UNKN: Unknown Error (unexpected exceptions)
 *
 * FedNow Requirements:
 * - Must respond within 1 second for critical events
 * - All system events must be logged
 * - Severity levels must be properly set
 * - Actionable information must be provided
 *
 * KEY DISTINCTION:
 * - admi.002 = Technical/system issues BEFORE business validation
 * - pacs.002 = Business acceptance/rejection AFTER validation
 * - admi.007 = Receipt acknowledgment (message received but not yet processed)
 */
@Slf4j
@Component
public class AnyMessageToAdmi002Converter extends AbstractMessageConverter<Object, Admi002> {

    public AnyMessageToAdmi002Converter() {
        super(Object.class, Admi002.class, "AnyMessageToAdmi002Converter");
    }

    @Override
    protected Mono<Admi002> doConvert(Object source, ConverterContext context) {
        logStep("Starting ANY → admi.002 conversion (generic system error)");

        // Generate message ID for admi.002
        enrichContextWithIds(context, "SYSERR", false, false);

        return Mono.fromCallable(() -> {
            // Get error details from context
            String errorCode = context.getAttribute("errorCode", String.class);
            String errorMessage = context.getAttribute("errorMessage", String.class);
            String errorComponent = context.getAttribute("errorComponent", String.class);
            String severity = context.getAttribute("errorSeverity", String.class);
            Throwable exception = context.getAttribute("exception", Throwable.class);

            // Default values
            if (errorCode == null) errorCode = "SYSF";
            if (errorMessage == null) errorMessage = "System error occurred";
            if (errorComponent == null) errorComponent = "System";
            if (severity == null) severity = "FATAL";

            // Build error reason
            Admi002.EventReason reason = buildEventReason(
                    errorCode, errorMessage, errorComponent, severity, exception);

            // Build admi.002
            Admi002 admi002 = Admi002.builder()
                    .messageId(context.getGeneratedMessageId())
                    .creationDateTime(context.getCurrentTimestamp())
                    .relatedReference(buildRelatedReference(source, context))
                    .reason(reason)
                    .originalBusinessInstruction(buildOriginalBusinessInstruction(source, context))
                    .build();

            logStep("Successfully created admi.002 system error notification with code: " + errorCode);
            return admi002;
        });
    }

    /**
     * Builds event reason for the error.
     *
     * @param errorCode error code
     * @param errorMessage error message
     * @param component affected component
     * @param severity severity level
     * @param exception exception if available
     * @return event reason
     */
    private Admi002.EventReason buildEventReason(
            String errorCode,
            String errorMessage,
            String component,
            String severity,
            Throwable exception) {

        List<String> additionalInfo = List.of(
                errorMessage,
                exception != null ? "Exception: " + exception.getClass().getSimpleName() : "No exception details",
                exception != null && exception.getMessage() != null
                        ? "Details: " + exception.getMessage()
                        : "No additional details available"
        );

        String recommendedAction = determineRecommendedAction(errorCode, exception);

        return Admi002.EventReason.builder()
                .code(errorCode)
                .severity(severity)
                .affectedComponent(component)
                .additionalInformation(additionalInfo)
                .recommendedAction(recommendedAction)
                .build();
    }

    /**
     * Determines recommended action based on error code and exception.
     *
     * @param errorCode error code
     * @param exception exception if available
     * @return recommended action
     */
    private String determineRecommendedAction(String errorCode, Throwable exception) {
        switch (errorCode) {
            case "SYSF":
                return "Contact system administrator - critical service failure";
            case "NETF":
                return "Check network connectivity and retry";
            case "CONF":
                return "Verify system configuration and restart service";
            case "RESF":
                return "Check system resources (memory, disk, CPU) and scale if needed";
            case "DBNF":
                return "Verify database connectivity and restart database service";
            default:
                if (exception != null && exception instanceof java.sql.SQLException) {
                    return "Check database connectivity and retry";
                } else if (exception != null && exception instanceof java.io.IOException) {
                    return "Check file system and network connectivity";
                } else if (exception != null && exception instanceof java.util.concurrent.TimeoutException) {
                    return "Retry operation or increase timeout threshold";
                }
                return "Contact support with error details for assistance";
        }
    }

    /**
     * Builds related reference from source message.
     *
     * @param source source message
     * @param context converter context
     * @return related reference
     */
    private Admi002.RelatedReference buildRelatedReference(Object source, ConverterContext context) {
        // Try to extract message details from source
        String messageId = extractMessageId(source);
        String messageType = extractMessageType(source);

        return Admi002.RelatedReference.builder()
                .messageId(messageId)
                .messageNameIdentification(messageType)
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds original business instruction from source message.
     *
     * @param source source message
     * @param context converter context
     * @return original business instruction
     */
    private Admi002.OriginalBusinessInstruction buildOriginalBusinessInstruction(
            Object source, ConverterContext context) {

        String messageId = extractMessageId(source);
        String messageType = extractMessageType(source);

        if (messageId == null || messageType == null) {
            return null; // No business instruction available
        }

        return Admi002.OriginalBusinessInstruction.builder()
                .messageId(messageId)
                .messageNameIdentification(messageType)
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Extracts message ID from source object using reflection.
     *
     * @param source source object
     * @return message ID or null
     */
    private String extractMessageId(Object source) {
        if (source == null) return null;

        try {
            // Try getGroupHeader().getMessageId()
            var groupHeaderMethod = source.getClass().getMethod("getGroupHeader");
            var groupHeader = groupHeaderMethod.invoke(source);
            if (groupHeader != null) {
                var messageIdMethod = groupHeader.getClass().getMethod("getMessageId");
                return (String) messageIdMethod.invoke(groupHeader);
            }
        } catch (Exception e) {
            // Ignore - source may not have group header
        }

        try {
            // Try getMessageId() directly
            var messageIdMethod = source.getClass().getMethod("getMessageId");
            return (String) messageIdMethod.invoke(source);
        } catch (Exception e) {
            // Ignore - source may not have messageId
        }

        return "UNKNOWN";
    }

    /**
     * Extracts message type from source object class name.
     *
     * @param source source object
     * @return message type or "unknown"
     */
    private String extractMessageType(Object source) {
        if (source == null) return "unknown";

        String className = source.getClass().getSimpleName();

        // Convert class name to ISO 20022 message type
        // E.g., "Pacs008" → "pacs.008.001.11"
        if (className.matches("^[A-Z][a-z]+\\d{3}$")) {
            String prefix = className.substring(0, 4).toLowerCase();
            String suffix = className.substring(4);
            return prefix + "." + suffix + ".001.11";
        }

        return className.toLowerCase();
    }

    // ==================== Helper Methods for Common Errors ====================

    /**
     * Creates admi.002 for system failure.
     */
    public Mono<Admi002> createSystemFailure(
            Object source,
            ConverterContext context,
            String component,
            String details,
            Throwable exception) {
        context.setAttribute("errorCode", "SYSF");
        context.setAttribute("errorMessage", "System failure in " + component + ": " + details);
        context.setAttribute("errorComponent", component);
        context.setAttribute("errorSeverity", "FATAL");
        context.setAttribute("exception", exception);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for network failure.
     */
    public Mono<Admi002> createNetworkFailure(
            Object source,
            ConverterContext context,
            String details,
            Throwable exception) {
        context.setAttribute("errorCode", "NETF");
        context.setAttribute("errorMessage", "Network failure: " + details);
        context.setAttribute("errorComponent", "Network");
        context.setAttribute("errorSeverity", "FATAL");
        context.setAttribute("exception", exception);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for database failure.
     */
    public Mono<Admi002> createDatabaseFailure(
            Object source,
            ConverterContext context,
            String details,
            Throwable exception) {
        context.setAttribute("errorCode", "DBNF");
        context.setAttribute("errorMessage", "Database failure: " + details);
        context.setAttribute("errorComponent", "Database");
        context.setAttribute("errorSeverity", "FATAL");
        context.setAttribute("exception", exception);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for configuration error.
     */
    public Mono<Admi002> createConfigurationError(
            Object source,
            ConverterContext context,
            String details) {
        context.setAttribute("errorCode", "CONF");
        context.setAttribute("errorMessage", "Configuration error: " + details);
        context.setAttribute("errorComponent", "Configuration");
        context.setAttribute("errorSeverity", "FATAL");
        return convert(source, context);
    }

    /**
     * Creates admi.002 for resource exhaustion.
     */
    public Mono<Admi002> createResourceExhaustion(
            Object source,
            ConverterContext context,
            String resourceType,
            String details) {
        context.setAttribute("errorCode", "RESF");
        context.setAttribute("errorMessage", resourceType + " exhausted: " + details);
        context.setAttribute("errorComponent", "Resource Management");
        context.setAttribute("errorSeverity", "FATAL");
        return convert(source, context);
    }

    /**
     * Creates admi.002 for unknown error.
     */
    public Mono<Admi002> createUnknownError(
            Object source,
            ConverterContext context,
            Throwable exception) {
        context.setAttribute("errorCode", "UNKN");
        context.setAttribute("errorMessage", "Unexpected error occurred");
        context.setAttribute("errorComponent", "System");
        context.setAttribute("errorSeverity", "FATAL");
        context.setAttribute("exception", exception);
        return convert(source, context);
    }
}
