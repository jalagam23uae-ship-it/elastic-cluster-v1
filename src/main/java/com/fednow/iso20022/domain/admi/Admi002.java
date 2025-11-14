package com.fednow.iso20022.domain.admi;

import com.fednow.iso20022.domain.common.PartyIdentification;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * admi.002.001.01 - System Event Notification
 *
 * This message is sent to notify about system-level events, operational issues,
 * or technical problems. It's used for events that occur BEFORE business validation.
 *
 * Message Flow:
 * System → Participant: admi.002 (this message)
 *
 * Use Cases:
 * - Authentication failures (AUTHF)
 * - Encryption failures (ENCF)
 * - Schema validation failures (SCHF)
 * - System failures (SYSF)
 * - Network failures (NETF)
 * - Capacity warnings (CAPC, CAPH)
 * - Queue warnings (QHGH)
 * - Maintenance notifications (MAINT)
 * - Service restoration (RSTR)
 *
 * Response Time: <1 second for critical events
 *
 * KEY DISTINCTION:
 * - admi.002 = Technical issues BEFORE business validation
 * - pacs.002 = Business acceptance/rejection AFTER validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Admi002 {

    /**
     * Message Identification - Unique ID for this system event notification.
     * Max 35 characters.
     */
    @NotBlank(message = "Message ID is required")
    @Size(max = 35, message = "Message ID must not exceed 35 characters")
    private String messageId;

    /**
     * Creation Date Time - When this event notification was created.
     */
    @NotNull(message = "Creation date time is required")
    private ZonedDateTime creationDateTime;

    /**
     * Related Reference - Reference to the message/transaction that triggered this event.
     */
    @Valid
    private RelatedReference relatedReference;

    /**
     * Reason - The system event reason/code.
     */
    @NotNull(message = "Reason is required")
    @Valid
    private EventReason reason;

    /**
     * Original Business Instruction - Optional reference to original business message.
     */
    private OriginalBusinessInstruction originalBusinessInstruction;

    /**
     * Supplementary Data - Additional proprietary information.
     */
    private List<SupplementaryData> supplementaryData;

    /**
     * Related Reference - Reference to related message or transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedReference {

        /**
         * Message Identification - ID of the related message.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Message ID must not exceed 35 characters")
        private String messageId;

        /**
         * Message Name Identification - Type of related message.
         * Examples: "pacs.008.001.11", "pain.001.001.11"
         * Max 35 characters.
         */
        @Size(max = 35, message = "Message name ID must not exceed 35 characters")
        private String messageNameIdentification;

        /**
         * Creation Date Time - Original creation timestamp.
         */
        private ZonedDateTime creationDateTime;

        /**
         * Transaction Identification - Transaction ID if applicable.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Transaction ID must not exceed 35 characters")
        private String transactionId;

        /**
         * UETR - Unique End-to-End Transaction Reference if applicable.
         */
        private java.util.UUID uetr;
    }

    /**
     * Event Reason - The reason for this system event.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventReason {

        /**
         * Code - Standardized event code.
         *
         * CRITICAL EVENT CODES (FATAL):
         * - AUTHF: Authentication Failure (certificate expired, signature failed)
         * - ENCF: Encryption Failure (TLS failed, decryption error)
         * - SCHF: Schema Validation Failure (invalid XML structure)
         * - SYSF: System Failure (database down, critical service failure)
         * - NETF: Network Failure (connection lost, timeout)
         *
         * WARNING EVENT CODES:
         * - CAPC: Capacity Critical (system >95% capacity)
         * - CAPH: Capacity High (system >80% capacity)
         * - QHGH: Queue High (message queue >80% full)
         * - DEGR: Degraded Performance (service degraded)
         *
         * INFORMATIONAL EVENT CODES:
         * - MAINT: Scheduled Maintenance
         * - RSTR: Service Restored
         * - NRML: Normal Operations
         * - INFO: General Information
         *
         * Max 10 characters.
         */
        @NotBlank(message = "Event code is required")
        @Size(max = 10, message = "Event code must not exceed 10 characters")
        private String code;

        /**
         * Proprietary - Proprietary event code (if not using standard code).
         * Max 35 characters.
         */
        @Size(max = 35, message = "Proprietary code must not exceed 35 characters")
        private String proprietary;

        /**
         * Additional Information - Human-readable description of the event.
         * This should provide actionable information to help resolve the issue.
         * Max 105 characters per line.
         */
        private List<String> additionalInformation;

        /**
         * Severity - Severity level of the event.
         *
         * Levels:
         * - FATAL: Critical failure, immediate action required
         * - ERROR: Error condition, action required
         * - WARNING: Warning condition, attention recommended
         * - INFO: Informational only
         *
         * Max 10 characters.
         */
        @Size(max = 10, message = "Severity must not exceed 10 characters")
        private String severity;

        /**
         * Affected Component - System component affected by this event.
         * Examples: "Authentication", "Database", "Network", "Queue"
         * Max 35 characters.
         */
        @Size(max = 35, message = "Affected component must not exceed 35 characters")
        private String affectedComponent;

        /**
         * Recommended Action - Suggested action to resolve the issue.
         * Max 140 characters.
         */
        @Size(max = 140, message = "Recommended action must not exceed 140 characters")
        private String recommendedAction;
    }

    /**
     * Original Business Instruction - Details about the original message that triggered the event.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OriginalBusinessInstruction {

        /**
         * Message Identification - ID of the original message.
         * Max 35 characters.
         */
        @NotNull(message = "Message ID is required")
        @Size(max = 35, message = "Message ID must not exceed 35 characters")
        private String messageId;

        /**
         * Message Name Identification - Type of original message.
         * Max 35 characters.
         */
        @NotNull(message = "Message name ID is required")
        @Size(max = 35, message = "Message name ID must not exceed 35 characters")
        private String messageNameIdentification;

        /**
         * Creation Date Time - Original creation timestamp.
         */
        private ZonedDateTime creationDateTime;

        /**
         * Instructing Party - Party that sent the original message.
         */
        private PartyIdentification instructingParty;

        /**
         * Instructed Party - Party that received the original message.
         */
        private PartyIdentification instructedParty;
    }

    /**
     * Supplementary Data - Additional proprietary data.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplementaryData {

        /**
         * Placement - Where in message this data applies.
         * Max 350 characters.
         */
        @Size(max = 350, message = "Placement must not exceed 350 characters")
        private String placement;

        /**
         * Envelope - Proprietary XML or JSON content with additional details.
         */
        private String envelope;
    }

    /**
     * Predefined system event reasons for common scenarios.
     */
    public static class CommonEvents {

        // FATAL Events
        public static EventReason authenticationFailure(String details) {
            return EventReason.builder()
                    .code("AUTHF")
                    .severity("FATAL")
                    .affectedComponent("Authentication")
                    .additionalInformation(List.of(
                            "Authentication failure: " + details,
                            "Certificate may be expired or signature verification failed"
                    ))
                    .recommendedAction("Verify certificates and retry with valid credentials")
                    .build();
        }

        public static EventReason encryptionFailure(String details) {
            return EventReason.builder()
                    .code("ENCF")
                    .severity("FATAL")
                    .affectedComponent("Encryption")
                    .additionalInformation(List.of(
                            "Encryption/Decryption failure: " + details,
                            "TLS handshake may have failed or message decryption error"
                    ))
                    .recommendedAction("Check TLS configuration and encryption keys")
                    .build();
        }

        public static EventReason schemaValidationFailure(String details) {
            return EventReason.builder()
                    .code("SCHF")
                    .severity("FATAL")
                    .affectedComponent("Schema Validation")
                    .additionalInformation(List.of(
                            "XML schema validation failed: " + details,
                            "Message does not conform to ISO 20022 schema"
                    ))
                    .recommendedAction("Validate message against ISO 20022 XSD schema and correct errors")
                    .build();
        }

        public static EventReason systemFailure(String component, String details) {
            return EventReason.builder()
                    .code("SYSF")
                    .severity("FATAL")
                    .affectedComponent(component)
                    .additionalInformation(List.of(
                            "System failure in " + component + ": " + details,
                            "Critical service unavailable"
                    ))
                    .recommendedAction("Contact system administrator immediately")
                    .build();
        }

        public static EventReason networkFailure(String details) {
            return EventReason.builder()
                    .code("NETF")
                    .severity("FATAL")
                    .affectedComponent("Network")
                    .additionalInformation(List.of(
                            "Network failure: " + details,
                            "Connection lost or timeout occurred"
                    ))
                    .recommendedAction("Check network connectivity and retry")
                    .build();
        }

        // WARNING Events
        public static EventReason capacityCritical(int percentUsed) {
            return EventReason.builder()
                    .code("CAPC")
                    .severity("WARNING")
                    .affectedComponent("System Capacity")
                    .additionalInformation(List.of(
                            "System capacity critical: " + percentUsed + "% utilized",
                            "Performance degradation may occur"
                    ))
                    .recommendedAction("Reduce load or scale up resources")
                    .build();
        }

        public static EventReason capacityHigh(int percentUsed) {
            return EventReason.builder()
                    .code("CAPH")
                    .severity("WARNING")
                    .affectedComponent("System Capacity")
                    .additionalInformation(List.of(
                            "System capacity high: " + percentUsed + "% utilized",
                            "Monitor for further increase"
                    ))
                    .recommendedAction("Monitor system load and prepare for scaling")
                    .build();
        }

        public static EventReason queueHigh(String queueName, int percentFull) {
            return EventReason.builder()
                    .code("QHGH")
                    .severity("WARNING")
                    .affectedComponent("Message Queue")
                    .additionalInformation(List.of(
                            "Queue '" + queueName + "' is " + percentFull + "% full",
                            "Message processing may be delayed"
                    ))
                    .recommendedAction("Increase queue processing capacity or investigate slow consumers")
                    .build();
        }

        public static EventReason degradedPerformance(String component, String details) {
            return EventReason.builder()
                    .code("DEGR")
                    .severity("WARNING")
                    .affectedComponent(component)
                    .additionalInformation(List.of(
                            "Degraded performance in " + component + ": " + details,
                            "Service operating below normal levels"
                    ))
                    .recommendedAction("Monitor performance metrics and investigate bottlenecks")
                    .build();
        }

        // INFO Events
        public static EventReason scheduledMaintenance(ZonedDateTime startTime, ZonedDateTime endTime) {
            return EventReason.builder()
                    .code("MAINT")
                    .severity("INFO")
                    .affectedComponent("System")
                    .additionalInformation(List.of(
                            "Scheduled maintenance window",
                            "Start: " + startTime,
                            "End: " + endTime
                    ))
                    .recommendedAction("Plan accordingly for maintenance window")
                    .build();
        }

        public static EventReason serviceRestored(String component) {
            return EventReason.builder()
                    .code("RSTR")
                    .severity("INFO")
                    .affectedComponent(component)
                    .additionalInformation(List.of(
                            "Service restored: " + component,
                            "Normal operations resumed"
                    ))
                    .recommendedAction("No action required")
                    .build();
        }

        public static EventReason normalOperations() {
            return EventReason.builder()
                    .code("NRML")
                    .severity("INFO")
                    .affectedComponent("System")
                    .additionalInformation(List.of("All systems operational"))
                    .recommendedAction("No action required")
                    .build();
        }
    }
}
