package com.fednow.iso20022.converter.core;

import com.fednow.iso20022.domain.common.AgentIdentification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Context object providing enrichment data and configuration for message conversion.
 *
 * This class holds:
 * - Bank identification and configuration
 * - Generated IDs (message ID, UETR, transaction ID)
 * - Timestamps
 * - Custom attributes for converter-specific data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConverterContext {

    /**
     * Bank Information - The bank performing the conversion.
     */
    private BankContext bankContext;

    /**
     * Source Message ID - UUID of the source message in database.
     * Used for tracking conversions and linking to messages table.
     */
    private UUID sourceMessageId;

    /**
     * Target Message ID - UUID of the target message in database.
     * Used for tracking conversions and linking to messages table.
     */
    private UUID targetMessageId;

    /**
     * Generated Message ID - Unique ID for the target message.
     * Max 35 characters.
     */
    private String generatedMessageId;

    /**
     * Generated UETR - Unique End-to-End Transaction Reference.
     * Generated as UUID v4 for new payments.
     */
    private UUID generatedUetr;

    /**
     * Generated Transaction ID - Bank-assigned transaction ID.
     * Max 35 characters.
     */
    private String generatedTransactionId;

    /**
     * Current Timestamp - Timestamp for message creation.
     */
    @Builder.Default
    private ZonedDateTime currentTimestamp = ZonedDateTime.now();

    /**
     * Current Date - Current date for settlement date.
     */
    @Builder.Default
    private java.time.LocalDate currentDate = java.time.LocalDate.now();

    /**
     * Custom Attributes - Converter-specific data.
     * Use this to pass custom data between validation and conversion.
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    /**
     * Validation Results - Results from validation framework.
     */
    private ValidationResults validationResults;

    /**
     * Bank Context - Information about the bank.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankContext {

        /**
         * Bank Identification - Agent identification for the bank.
         */
        private AgentIdentification bankIdentification;

        /**
         * Bank BIC - SWIFT code.
         * Example: "BANKUS33XXX"
         */
        private String bankBic;

        /**
         * Bank Routing Number - US ABA routing number.
         * Example: "026009593"
         */
        private String bankRoutingNumber;

        /**
         * Bank Name - Full name of the bank.
         */
        private String bankName;

        /**
         * Is FedNow Participant - Whether bank is direct FedNow participant.
         */
        @Builder.Default
        private boolean fedNowParticipant = true;

        /**
         * FedNow Member ID - Bank's FedNow member identifier.
         */
        private String fedNowMemberId;

        /**
         * Bank Timezone - Bank's timezone for timestamps.
         * Default: UTC
         */
        @Builder.Default
        private String timezone = "UTC";
    }

    /**
     * Validation Results - Results from validation framework.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResults {

        /**
         * Is Valid - Overall validation result.
         */
        private boolean valid;

        /**
         * Has Warnings - Whether there are validation warnings.
         */
        private boolean hasWarnings;

        /**
         * Errors - Validation errors (if any).
         */
        private java.util.List<ValidationError> errors;

        /**
         * Warnings - Validation warnings (if any).
         */
        private java.util.List<ValidationError> warnings;

        /**
         * OFAC Check Result - Result of OFAC screening.
         */
        private OFACCheckResult ofacCheckResult;

        /**
         * Fraud Score - Fraud detection score (0-100).
         * 0 = No risk, 100 = High risk
         */
        private Integer fraudScore;

        /**
         * Fraud Flags - Specific fraud indicators detected.
         */
        private java.util.List<String> fraudFlags;
    }

    /**
     * Validation Error - A single validation error or warning.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {

        /**
         * Error Code - Standardized error code.
         * Examples: ACC001, OFAC001, FRD001
         */
        private String errorCode;

        /**
         * Error Category - Category of the error.
         * Examples: ACCOUNT, AMOUNT, OFAC, FRAUD, SCHEMA
         */
        private String category;

        /**
         * Field Path - Path to the field that failed validation.
         * Example: "paymentInformation[0].debtor.account"
         */
        private String fieldPath;

        /**
         * Error Message - Human-readable error description.
         */
        private String errorMessage;

        /**
         * Severity - Error severity level.
         */
        private ErrorSeverity severity;

        /**
         * ISO 20022 Reason Code - Corresponding ISO 20022 status reason code.
         * Examples: AC01, AM04, AG01
         */
        private String iso20022ReasonCode;
    }

    /**
     * Error Severity - Severity levels for validation errors.
     */
    public enum ErrorSeverity {
        /**
         * Critical - Must reject immediately.
         */
        CRITICAL,

        /**
         * Error - Should reject or require manual review.
         */
        ERROR,

        /**
         * Warning - Can proceed but flag for review.
         */
        WARNING,

        /**
         * Info - Informational only.
         */
        INFO
    }

    /**
     * OFAC Check Result - Result of OFAC screening.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OFACCheckResult {

        /**
         * Passed - Whether OFAC check passed.
         */
        private boolean passed;

        /**
         * Matches - List of OFAC matches (if any).
         */
        private java.util.List<String> matches;

        /**
         * Check Timestamp - When check was performed.
         */
        private ZonedDateTime checkTimestamp;

        /**
         * SDN List Version - Version of SDN list used.
         */
        private String sdnListVersion;
    }

    /**
     * Helper methods for attributes.
     */

    public void setAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, Class<T> type) {
        if (attributes == null) {
            return null;
        }
        Object value = attributes.get(key);
        if (value == null) {
            return null;
        }
        return (T) value;
    }

    public boolean hasAttribute(String key) {
        return attributes != null && attributes.containsKey(key);
    }

    /**
     * Convenience method to check if validation passed without critical errors.
     *
     * @return true if no critical validation errors
     */
    public boolean isValidForConversion() {
        if (validationResults == null) {
            return true; // No validation performed yet
        }
        return validationResults.valid || !hasCriticalErrors();
    }

    /**
     * Check if there are critical validation errors.
     *
     * @return true if critical errors exist
     */
    public boolean hasCriticalErrors() {
        if (validationResults == null || validationResults.errors == null) {
            return false;
        }
        return validationResults.errors.stream()
                .anyMatch(error -> error.severity == ErrorSeverity.CRITICAL);
    }

    /**
     * Check if OFAC screening passed.
     *
     * @return true if OFAC check passed
     */
    public boolean passedOFAC() {
        if (validationResults == null || validationResults.ofacCheckResult == null) {
            return true; // No check performed
        }
        return validationResults.ofacCheckResult.passed;
    }

    /**
     * Check if fraud score is above threshold.
     *
     * @param threshold fraud score threshold (0-100)
     * @return true if fraud score exceeds threshold
     */
    public boolean isFraudScoreAbove(int threshold) {
        if (validationResults == null || validationResults.fraudScore == null) {
            return false;
        }
        return validationResults.fraudScore > threshold;
    }

    /**
     * Creates a default context for testing/development.
     *
     * @return default converter context
     */
    public static ConverterContext createDefault() {
        return ConverterContext.builder()
                .bankContext(BankContext.builder()
                        .bankBic("BANKUS33XXX")
                        .bankRoutingNumber("026009593")
                        .bankName("Example Bank")
                        .fedNowParticipant(true)
                        .build())
                .currentTimestamp(ZonedDateTime.now())
                .currentDate(java.time.LocalDate.now())
                .validationResults(ValidationResults.builder()
                        .valid(true)
                        .hasWarnings(false)
                        .build())
                .build();
    }
}
