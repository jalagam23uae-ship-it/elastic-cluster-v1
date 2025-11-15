package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.admi.Admi002;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * SchemaFailureToAdmi002Converter - Schema and Format Validation Error Handler
 *
 * Converts schema validation failures to admi.002 (System Event Notification).
 *
 * This is a CRITICAL converter for handling XML/JSON schema validation failures,
 * format errors, and structural issues in ISO 20022 messages.
 *
 * Validation Failure Types:
 * 1. Schema Validation (SCHF):
 *    - XML schema (XSD) validation failures
 *    - Missing required fields
 *    - Invalid data types
 *    - Constraint violations (max length, pattern, etc.)
 *    - Namespace errors
 *
 * 2. Format Validation (FMTF):
 *    - Invalid date/time formats
 *    - Invalid currency codes
 *    - Invalid BIC codes
 *    - Invalid IBAN/account numbers
 *    - Invalid amount formats
 *
 * 3. Structural Validation (STRF):
 *    - Invalid XML structure
 *    - Malformed JSON
 *    - Missing required elements
 *    - Invalid element ordering
 *    - Incorrect message type
 *
 * 4. Character Encoding (ENCF):
 *    - Invalid UTF-8 encoding
 *    - Unsupported character sets
 *    - Special character issues
 *
 * FedNow Schema Requirements:
 * - All messages must conform to ISO 20022 XSD schemas
 * - Must use specific ISO 20022 versions (e.g., pacs.008.001.11)
 * - Must respond within 1 second for schema failures
 * - Provide detailed validation error information
 * - Include line/column numbers when available
 *
 * Common Schema Errors:
 * - Missing mandatory fields (group header, message ID, etc.)
 * - Field length violations (messageId > 35 chars)
 * - Pattern violations (BIC code format)
 * - Enumeration violations (invalid currency code)
 * - Cardinality violations (min/max occurrences)
 *
 * Response Flow:
 * 1. Receive malformed message
 * 2. Schema validation fails
 * 3. Generate admi.002 with detailed error info
 * 4. Send admi.002 back to sender
 * 5. Log validation failure for monitoring
 */
@Slf4j
@Component
public class SchemaFailureToAdmi002Converter extends AbstractMessageConverter<Object, Admi002> {

    public SchemaFailureToAdmi002Converter() {
        super(Object.class, Admi002.class, "SchemaFailureToAdmi002Converter");
    }

    @Override
    protected Mono<Admi002> doConvert(Object source, ConverterContext context) {
        logStep("Starting SCHEMA_FAILURE → admi.002 conversion (validation error)");

        // Generate message ID for admi.002
        enrichContextWithIds(context, "SCHMERR", false, false);

        return Mono.fromCallable(() -> {
            // Get validation error details from context
            String validationType = context.getAttribute("validationType", String.class);
            String validationMessage = context.getAttribute("validationMessage", String.class);
            String fieldName = context.getAttribute("fieldName", String.class);
            String fieldValue = context.getAttribute("fieldValue", String.class);
            Integer lineNumber = context.getAttribute("lineNumber", Integer.class);
            Integer columnNumber = context.getAttribute("columnNumber", Integer.class);
            List<String> validationErrors = context.getAttribute("validationErrors", List.class);

            // Default values
            if (validationType == null) validationType = "SCHF";
            if (validationMessage == null) validationMessage = "Schema validation failed";

            // Build event reason
            Admi002.EventReason reason = buildValidationEventReason(
                    validationType,
                    validationMessage,
                    fieldName,
                    fieldValue,
                    lineNumber,
                    columnNumber,
                    validationErrors);

            // Build admi.002
            Admi002 admi002 = Admi002.builder()
                    .messageId(context.getGeneratedMessageId())
                    .creationDateTime(context.getCurrentTimestamp())
                    .relatedReference(buildRelatedReference(source, context))
                    .reason(reason)
                    .originalBusinessInstruction(buildOriginalBusinessInstruction(source, context))
                    .build();

            logStep("Successfully created admi.002 schema validation error: " + validationType);
            return admi002;
        });
    }

    /**
     * Builds validation event reason.
     *
     * @param validationType validation type code
     * @param validationMessage validation error message
     * @param fieldName field that failed validation
     * @param fieldValue value that failed
     * @param lineNumber line number in XML (if available)
     * @param columnNumber column number in XML (if available)
     * @param validationErrors list of all validation errors
     * @return event reason
     */
    private Admi002.EventReason buildValidationEventReason(
            String validationType,
            String validationMessage,
            String fieldName,
            String fieldValue,
            Integer lineNumber,
            Integer columnNumber,
            List<String> validationErrors) {

        List<String> additionalInfo = new ArrayList<>();
        additionalInfo.add("Schema validation failed: " + validationMessage);

        if (fieldName != null) {
            additionalInfo.add("Field: " + fieldName);
        }
        if (fieldValue != null) {
            // Truncate long values
            String truncatedValue = fieldValue.length() > 50
                    ? fieldValue.substring(0, 50) + "..."
                    : fieldValue;
            additionalInfo.add("Value: " + truncatedValue);
        }
        if (lineNumber != null || columnNumber != null) {
            additionalInfo.add(String.format("Location: Line %d, Column %d",
                    lineNumber != null ? lineNumber : 0,
                    columnNumber != null ? columnNumber : 0));
        }

        // Add additional validation errors (up to 3)
        if (validationErrors != null && !validationErrors.isEmpty()) {
            int errorCount = Math.min(validationErrors.size(), 3);
            for (int i = 0; i < errorCount; i++) {
                additionalInfo.add("Error " + (i + 1) + ": " + validationErrors.get(i));
            }
            if (validationErrors.size() > 3) {
                additionalInfo.add("... and " + (validationErrors.size() - 3) + " more errors");
            }
        }

        String recommendedAction = determineValidationRecommendedAction(validationType, fieldName);

        return Admi002.EventReason.builder()
                .code(validationType)
                .severity("FATAL")
                .affectedComponent("Schema Validation")
                .additionalInformation(additionalInfo)
                .recommendedAction(recommendedAction)
                .build();
    }

    /**
     * Determines recommended action for validation error.
     *
     * @param validationType validation type
     * @param fieldName field name
     * @return recommended action
     */
    private String determineValidationRecommendedAction(String validationType, String fieldName) {
        switch (validationType) {
            case "SCHF":
                if (fieldName != null) {
                    return "Correct field '" + fieldName + "' and retry with valid ISO 20022 message";
                }
                return "Validate message against ISO 20022 XSD schema and correct all errors";

            case "FMTF":
                if (fieldName != null) {
                    return "Fix format of field '" + fieldName + "' according to ISO 20022 standard";
                }
                return "Review and correct all format violations";

            case "STRF":
                return "Verify XML/JSON structure conforms to ISO 20022 specification";

            case "ENCF":
                return "Ensure message uses UTF-8 encoding and valid characters";

            case "MISS":
                if (fieldName != null) {
                    return "Add required field '" + fieldName + "' and retry";
                }
                return "Add all required fields and retry";

            case "LENG":
                if (fieldName != null) {
                    return "Reduce length of field '" + fieldName + "' to conform to maximum";
                }
                return "Reduce field lengths to conform to schema constraints";

            default:
                return "Review ISO 20022 schema documentation and correct all validation errors";
        }
    }

    /**
     * Builds related reference from source message.
     */
    private Admi002.RelatedReference buildRelatedReference(Object source, ConverterContext context) {
        String messageId = extractMessageId(source);
        String messageType = context.getAttribute("messageType", String.class);

        return Admi002.RelatedReference.builder()
                .messageId(messageId)
                .messageNameIdentification(messageType != null ? messageType : "unknown")
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds original business instruction.
     */
    private Admi002.OriginalBusinessInstruction buildOriginalBusinessInstruction(
            Object source, ConverterContext context) {

        String messageId = extractMessageId(source);
        String messageType = context.getAttribute("messageType", String.class);

        if (messageId == null || messageType == null) {
            return null;
        }

        return Admi002.OriginalBusinessInstruction.builder()
                .messageId(messageId)
                .messageNameIdentification(messageType)
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Extracts message ID from source (may fail for invalid messages).
     */
    private String extractMessageId(Object source) {
        if (source == null) return "INVALID_MESSAGE";

        // For raw XML/String input
        if (source instanceof String) {
            return extractMessageIdFromString((String) source);
        }

        try {
            var groupHeaderMethod = source.getClass().getMethod("getGroupHeader");
            var groupHeader = groupHeaderMethod.invoke(source);
            if (groupHeader != null) {
                var messageIdMethod = groupHeader.getClass().getMethod("getMessageId");
                return (String) messageIdMethod.invoke(groupHeader);
            }
        } catch (Exception e) {
            // Ignore - source may be malformed
        }

        return "INVALID_MESSAGE";
    }

    /**
     * Extracts message ID from XML string.
     */
    private String extractMessageIdFromString(String xmlContent) {
        try {
            // Simple regex to extract message ID from XML
            // Example: <MsgId>PMT-001</MsgId>
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "<MsgId>([^<]+)</MsgId>", java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher = pattern.matcher(xmlContent);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            // Ignore
        }
        return "UNPARSEABLE_MESSAGE";
    }

    // ==================== Helper Methods for Common Schema Errors ====================

    /**
     * Creates admi.002 for missing required field.
     */
    public Mono<Admi002> createMissingFieldError(
            Object source,
            ConverterContext context,
            String messageType,
            String fieldName) {
        context.setAttribute("validationType", "MISS");
        context.setAttribute("validationMessage", "Required field is missing");
        context.setAttribute("fieldName", fieldName);
        context.setAttribute("messageType", messageType);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for field length violation.
     */
    public Mono<Admi002> createFieldLengthError(
            Object source,
            ConverterContext context,
            String messageType,
            String fieldName,
            String fieldValue,
            int maxLength) {
        context.setAttribute("validationType", "LENG");
        context.setAttribute("validationMessage",
                "Field exceeds maximum length of " + maxLength + " characters");
        context.setAttribute("fieldName", fieldName);
        context.setAttribute("fieldValue", fieldValue);
        context.setAttribute("messageType", messageType);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for invalid format.
     */
    public Mono<Admi002> createFormatError(
            Object source,
            ConverterContext context,
            String messageType,
            String fieldName,
            String fieldValue,
            String expectedFormat) {
        context.setAttribute("validationType", "FMTF");
        context.setAttribute("validationMessage",
                "Invalid format - expected: " + expectedFormat);
        context.setAttribute("fieldName", fieldName);
        context.setAttribute("fieldValue", fieldValue);
        context.setAttribute("messageType", messageType);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for invalid XML structure.
     */
    public Mono<Admi002> createStructureError(
            Object source,
            ConverterContext context,
            String messageType,
            String errorMessage,
            Integer lineNumber,
            Integer columnNumber) {
        context.setAttribute("validationType", "STRF");
        context.setAttribute("validationMessage", errorMessage);
        context.setAttribute("messageType", messageType);
        context.setAttribute("lineNumber", lineNumber);
        context.setAttribute("columnNumber", columnNumber);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for multiple validation errors.
     */
    public Mono<Admi002> createMultipleValidationErrors(
            Object source,
            ConverterContext context,
            String messageType,
            List<String> validationErrors) {
        context.setAttribute("validationType", "SCHF");
        context.setAttribute("validationMessage",
                "Multiple schema validation errors found (" + validationErrors.size() + " errors)");
        context.setAttribute("validationErrors", validationErrors);
        context.setAttribute("messageType", messageType);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for invalid message type.
     */
    public Mono<Admi002> createInvalidMessageType(
            Object source,
            ConverterContext context,
            String receivedType,
            String expectedType) {
        context.setAttribute("validationType", "STRF");
        context.setAttribute("validationMessage",
                "Invalid message type - received: " + receivedType + ", expected: " + expectedType);
        context.setAttribute("messageType", receivedType);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for character encoding error.
     */
    public Mono<Admi002> createEncodingError(
            Object source,
            ConverterContext context,
            String messageType,
            String details) {
        context.setAttribute("validationType", "ENCF");
        context.setAttribute("validationMessage", "Character encoding error: " + details);
        context.setAttribute("messageType", messageType);
        return convert(source, context);
    }

    /**
     * Creates admi.002 for pattern violation (e.g., invalid BIC code).
     */
    public Mono<Admi002> createPatternViolation(
            Object source,
            ConverterContext context,
            String messageType,
            String fieldName,
            String fieldValue,
            String pattern) {
        context.setAttribute("validationType", "FMTF");
        context.setAttribute("validationMessage",
                "Value does not match required pattern: " + pattern);
        context.setAttribute("fieldName", fieldName);
        context.setAttribute("fieldValue", fieldValue);
        context.setAttribute("messageType", messageType);
        return convert(source, context);
    }
}
