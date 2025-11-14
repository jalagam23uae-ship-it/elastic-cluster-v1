package com.fednow.iso20022.domain.pacs;

import com.fednow.iso20022.domain.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * pacs.002.001.12 - FI to FI Payment Status Report
 *
 * This message is sent by a financial institution to provide the status of a previously
 * received payment instruction (pacs.008). It's the business-level response indicating
 * acceptance, rejection, or pending status.
 *
 * Message Flow:
 * Bank/FedNow → Bank: pacs.002 (this message)
 * Then Bank → Customer: pain.002 (converted)
 *
 * This is a CRITICAL message for payment status tracking.
 *
 * Status Codes:
 * - ACCP: Accepted (payment will settle)
 * - RJCT: Rejected (payment will not settle)
 * - PDNG: Pending (requires manual review or additional processing)
 * - PART: Partially accepted (some transactions in batch rejected)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pacs002 {

    /**
     * Group Header - Message-level information.
     */
    @NotNull(message = "Group header is required")
    @Valid
    private GroupHeader groupHeader;

    /**
     * Original Group Information and Status - Status of the entire original message.
     * Optional - only present if reporting on group level.
     */
    @Valid
    private OriginalGroupInformationAndStatus originalGroupInformationAndStatus;

    /**
     * Transaction Information and Status - Status of individual transactions.
     * At least one is required.
     */
    @NotEmpty(message = "At least one transaction status is required")
    @Valid
    private List<TransactionInformationAndStatus> transactionInformationAndStatus;

    /**
     * Original Group Information and Status - Group-level status information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OriginalGroupInformationAndStatus {

        /**
         * Original Message Identification - Message ID of the original payment (pacs.008).
         * Max 35 characters.
         */
        @NotNull(message = "Original message ID is required")
        @Size(max = 35, message = "Original message ID must not exceed 35 characters")
        private String originalMessageId;

        /**
         * Original Message Name Identification - Type of original message.
         * For FedNow: "pacs.008.001.11"
         * Max 35 characters.
         */
        @NotNull(message = "Original message name ID is required")
        @Size(max = 35, message = "Original message name ID must not exceed 35 characters")
        private String originalMessageNameIdentification;

        /**
         * Original Creation Date Time - Timestamp of original message.
         */
        private ZonedDateTime originalCreationDateTime;

        /**
         * Group Status - Overall status of the message.
         *
         * Status Codes:
         * - ACCP: All transactions accepted
         * - RJCT: All transactions rejected
         * - PART: Some accepted, some rejected
         * - PDNG: All transactions pending
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Group status must not exceed 4 characters")
        private String groupStatus;

        /**
         * Status Reason Information - Reasons for the group status.
         */
        private List<StatusReasonInformation> statusReasonInformation;

        /**
         * Number of Transactions Per Status - Count of transactions in each status.
         */
        private List<NumberOfTransactionsPerStatus> numberOfTransactionsPerStatus;
    }

    /**
     * Transaction Information and Status - Status of a single transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionInformationAndStatus {

        /**
         * Status Identification - Unique ID for this status report entry.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Status ID must not exceed 35 characters")
        private String statusId;

        /**
         * Original Group Information - Reference to original message group.
         */
        private OriginalGroupInformation originalGroupInformation;

        /**
         * Original Instruction Identification - Original instruction ID from pacs.008.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Original instruction ID must not exceed 35 characters")
        private String originalInstructionId;

        /**
         * Original End-to-End Identification - Original E2E ID from pacs.008.
         * MANDATORY for FedNow tracking.
         * Max 35 characters.
         */
        @NotNull(message = "Original end-to-end ID is required")
        @Size(max = 35, message = "Original end-to-end ID must not exceed 35 characters")
        private String originalEndToEndId;

        /**
         * Original Transaction Identification - Original transaction ID from pacs.008.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Original transaction ID must not exceed 35 characters")
        private String originalTransactionId;

        /**
         * Original UETR - Original Unique End-to-End Transaction Reference.
         * UUID from pacs.008.
         */
        private java.util.UUID originalUetr;

        /**
         * Transaction Status - Status of this specific transaction.
         *
         * CRITICAL STATUS CODES:
         * - ACCP: Accepted for settlement (✅ Success)
         * - ACSC: Settlement completed (✅ Settled)
         * - ACSP: Accepted, settlement in process
         * - ACTC: Accepted, technical validation successful
         * - ACWC: Accepted, with change
         * - RJCT: Rejected (❌ Failed)
         * - PDNG: Pending (⏳ Manual review required)
         * - PART: Partially accepted
         * - RCVD: Received (initial acknowledgement)
         *
         * Max 4 characters.
         */
        @NotNull(message = "Transaction status is required")
        @Size(max = 4, message = "Transaction status must not exceed 4 characters")
        private String transactionStatus;

        /**
         * Status Reason Information - Reasons for the status (especially for RJCT or PDNG).
         */
        private List<StatusReasonInformation> statusReasonInformation;

        /**
         * Charges Information - Charges applied to the transaction.
         */
        private List<ChargesInformation> chargesInformation;

        /**
         * Acceptance Date Time - When the transaction was accepted.
         * Present for ACCP and ACSC statuses.
         */
        private ZonedDateTime acceptanceDateTimeacceptanceDateTimeclearingSystemReference;

        /**
         * Clearing System Reference - Reference assigned by FedNow clearing system.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Clearing system reference must not exceed 35 characters")
        private String clearingSystemReference;

        /**
         * Account Servicing Reference - Reference assigned by account servicer.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Account servicing reference must not exceed 35 characters")
        private String accountServicingReference;

        /**
         * Original Transaction Reference - Full details of original transaction.
         * Used to provide context about what was being processed.
         */
        private OriginalTransactionReference originalTransactionReference;

        /**
         * Supplementary Data - Additional proprietary information.
         */
        private List<SupplementaryData> supplementaryData;
    }

    /**
     * Status Reason Information - Detailed reason for a status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusReasonInformation {

        /**
         * Originator - Party that originated this status.
         */
        private PartyIdentification originator;

        /**
         * Reason - The reason code and/or proprietary code.
         */
        private StatusReason reason;

        /**
         * Additional Information - Human-readable explanation.
         * Max 105 characters per line.
         */
        private List<String> additionalInformation;

        /**
         * Common rejection reason codes for FedNow.
         */
        public static class RejectionReasons {
            // Account-related rejections
            public static final String AC01 = "AC01"; // Incorrect account number
            public static final String AC04 = "AC04"; // Closed account
            public static final String AC06 = "AC06"; // Blocked account

            // Amount-related rejections
            public static final String AM04 = "AM04"; // Insufficient funds
            public static final String AM09 = "AM09"; // Wrong amount / Amount exceeds limits

            // Agent/Bank-related rejections
            public static final String RC01 = "RC01"; // Bank identifier incorrect
            public static final String AG01 = "AG01"; // Transaction forbidden (OFAC, sanctions)

            // Format/Technical rejections
            public static final String FF01 = "FF01"; // Invalid file format
            public static final String DS01 = "DS01"; // Incorrect data structure

            // Party/Reference rejections
            public static final String RR01 = "RR01"; // Missing debtor account/identification
            public static final String RR03 = "RR03"; // Missing creditor name/address
            public static final String RR04 = "RR04"; // Regulatory reason

            // Other rejections
            public static final String MS03 = "MS03"; // Not specified reason
            public static final String DUPL = "DUPL"; // Duplicate payment
            public static final String FRAD = "FRAD"; // Fraudulent payment
        }
    }

    /**
     * Original Group Information - Reference to original message.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OriginalGroupInformation {

        /**
         * Original Message Identification - ID of original message.
         * Max 35 characters.
         */
        @NotNull(message = "Original message ID is required")
        @Size(max = 35, message = "Original message ID must not exceed 35 characters")
        private String originalMessageId;

        /**
         * Original Message Name Identification - Type of original message.
         * Max 35 characters.
         */
        @NotNull(message = "Original message name ID is required")
        @Size(max = 35, message = "Original message name ID must not exceed 35 characters")
        private String originalMessageNameIdentification;

        /**
         * Original Creation Date Time - Timestamp of original.
         */
        private ZonedDateTime originalCreationDateTime;
    }

    /**
     * Number of Transactions Per Status - Count breakdown by status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NumberOfTransactionsPerStatus {

        /**
         * Detailed Number of Transactions - Count for a specific status.
         */
        @NotNull(message = "Detailed number of transactions is required")
        private Integer detailedNumberOfTransactions;

        /**
         * Detailed Status - The status being counted.
         * ACCP, RJCT, PDNG, etc.
         * Max 4 characters.
         */
        @NotNull(message = "Detailed status is required")
        @Size(max = 4, message = "Detailed status must not exceed 4 characters")
        private String detailedStatus;

        /**
         * Detailed Control Sum - Sum of amounts for this status.
         */
        private java.math.BigDecimal detailedControlSum;
    }

    /**
     * Charges Information - Information about charges applied.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargesInformation {

        /**
         * Amount - Charge amount.
         */
        @NotNull(message = "Charge amount is required")
        @Valid
        private Amount amount;

        /**
         * Agent - Agent applying the charge.
         */
        @Valid
        private AgentIdentification agent;

        /**
         * Type - Type of charge.
         */
        private ChargeType type;

        /**
         * Additional Information - Extra details about the charge.
         * Max 140 characters.
         */
        @Size(max = 140, message = "Additional information must not exceed 140 characters")
        private String additionalInformation;
    }

    /**
     * Charge Type - Classification of charge.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargeType {

        /**
         * Code - Standardized charge code.
         * Max 4 characters.
         */
        @Size(max = 4, message = "Code must not exceed 4 characters")
        private String code;

        /**
         * Proprietary - Proprietary charge type.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Proprietary must not exceed 35 characters")
        private String proprietary;
    }

    /**
     * Original Transaction Reference - Complete details of original transaction.
     * Used to provide full context about what was being paid.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OriginalTransactionReference {

        /**
         * Interbank Settlement Amount - Original settlement amount.
         */
        private Amount interbankSettlementAmount;

        /**
         * Interbank Settlement Date - Original settlement date.
         */
        private java.time.LocalDate interbankSettlementDate;

        /**
         * Payment Type Information - Original payment type.
         */
        private PaymentTypeInformation paymentTypeInformation;

        /**
         * Payment Method - Original payment method (TRF, CHK, etc.).
         * Max 3 characters.
         */
        @Size(max = 3, message = "Payment method must not exceed 3 characters")
        private String paymentMethod;

        /**
         * Debtor - Original debtor.
         */
        private PartyIdentification debtor;

        /**
         * Debtor Account - Original debtor account.
         */
        private AccountIdentification debtorAccount;

        /**
         * Debtor Agent - Original debtor agent.
         */
        private AgentIdentification debtorAgent;

        /**
         * Creditor - Original creditor.
         */
        private PartyIdentification creditor;

        /**
         * Creditor Account - Original creditor account.
         */
        private AccountIdentification creditorAccount;

        /**
         * Creditor Agent - Original creditor agent.
         */
        private AgentIdentification creditorAgent;

        /**
         * Remittance Information - Original remittance info.
         */
        private RemittanceInformation remittanceInformation;
    }

    /**
     * Payment Type Information - Payment classification.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentTypeInformation {

        /**
         * Instruction Priority - Priority level.
         * Max 4 characters.
         */
        @Size(max = 4, message = "Instruction priority must not exceed 4 characters")
        private String instructionPriority;

        /**
         * Clearing Channel - Clearing channel used.
         * Max 4 characters.
         */
        @Size(max = 4, message = "Clearing channel must not exceed 4 characters")
        private String clearingChannel;

        /**
         * Service Level - Service level.
         * Max 4 characters.
         */
        @Size(max = 4, message = "Service level must not exceed 4 characters")
        private String serviceLevel;

        /**
         * Local Instrument - Local instrument code.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Local instrument must not exceed 35 characters")
        private String localInstrument;

        /**
         * Category Purpose - Payment category.
         * Max 4 characters.
         */
        @Size(max = 4, message = "Category purpose must not exceed 4 characters")
        private String categoryPurpose;
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
         * Envelope - Proprietary XML content.
         */
        private String envelope;
    }
}
