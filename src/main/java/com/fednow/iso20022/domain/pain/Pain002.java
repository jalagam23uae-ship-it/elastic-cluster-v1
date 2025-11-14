package com.fednow.iso20022.domain.pain;

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
 * pain.002.001.12 - Customer Payment Status Report
 *
 * This message is sent by a bank to a customer to provide the status of
 * a previously submitted payment instruction (pain.001). It's the customer-facing
 * version of pacs.002 with user-friendly messages.
 *
 * Message Flow:
 * Bank → Customer: pain.002 (this message)
 * (Converted from pacs.002 received from FedNow)
 *
 * Key Features:
 * - Translates technical codes to customer-friendly messages
 * - Preserves End-to-End ID for customer tracking
 * - Provides actionable information for rejected payments
 *
 * Status Codes (same as pacs.002):
 * - ACCP: Payment accepted and will be settled
 * - RJCT: Payment rejected and will not be processed
 * - PDNG: Payment pending, requires additional action
 * - PART: Some payments accepted, some rejected
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pain002 {

    /**
     * Group Header - Message-level information.
     */
    @NotNull(message = "Group header is required")
    @Valid
    private GroupHeader groupHeader;

    /**
     * Original Group Information and Status - Status of the original pain.001.
     * Optional - only if reporting at group level.
     */
    @Valid
    private OriginalGroupInformationAndStatus originalGroupInformationAndStatus;

    /**
     * Original Payment Information and Status - Status of payment information blocks.
     * At least one is required.
     */
    @NotEmpty(message = "At least one original payment information status is required")
    @Valid
    private List<OriginalPaymentInformationAndStatus> originalPaymentInformationAndStatus;

    /**
     * Original Group Information and Status - Group-level status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OriginalGroupInformationAndStatus {

        /**
         * Original Message Identification - Message ID from original pain.001.
         * Max 35 characters.
         */
        @NotNull(message = "Original message ID is required")
        @Size(max = 35, message = "Original message ID must not exceed 35 characters")
        private String originalMessageId;

        /**
         * Original Message Name Identification - Type of original message.
         * Typically: "pain.001.001.11"
         * Max 35 characters.
         */
        @NotNull(message = "Original message name ID is required")
        @Size(max = 35, message = "Original message name ID must not exceed 35 characters")
        private String originalMessageNameIdentification;

        /**
         * Original Creation Date Time - Timestamp of original pain.001.
         */
        private ZonedDateTime originalCreationDateTime;

        /**
         * Group Status - Overall status.
         *
         * - ACCP: All payments accepted
         * - RJCT: All payments rejected
         * - PART: Some accepted, some rejected
         * - PDNG: All payments pending
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Group status must not exceed 4 characters")
        private String groupStatus;

        /**
         * Status Reason Information - Reasons for group status.
         */
        private List<StatusReasonInformation> statusReasonInformation;

        /**
         * Number of Transactions Per Status - Count by status.
         */
        private List<NumberOfTransactionsPerStatus> numberOfTransactionsPerStatus;
    }

    /**
     * Original Payment Information and Status - Status of a payment information block.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OriginalPaymentInformationAndStatus {

        /**
         * Original Payment Information ID - ID from original pain.001.
         * Max 35 characters.
         */
        @NotNull(message = "Original payment information ID is required")
        @Size(max = 35, message = "Original payment information ID must not exceed 35 characters")
        private String originalPaymentInformationId;

        /**
         * Payment Information Status - Status of this payment block.
         *
         * - ACCP: All transactions in block accepted
         * - RJCT: All transactions in block rejected
         * - PART: Some accepted, some rejected
         * - PDNG: All transactions pending
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Payment information status must not exceed 4 characters")
        private String paymentInformationStatus;

        /**
         * Status Reason Information - Reasons for block status.
         */
        private List<StatusReasonInformation> statusReasonInformation;

        /**
         * Number of Transactions Per Status - Transaction count by status.
         */
        private List<NumberOfTransactionsPerStatus> numberOfTransactionsPerStatus;

        /**
         * Transaction Information and Status - Status of individual transactions.
         */
        private List<TransactionInformationAndStatus> transactionInformationAndStatus;
    }

    /**
     * Transaction Information and Status - Status of a single payment transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionInformationAndStatus {

        /**
         * Status Identification - Unique ID for this status entry.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Status ID must not exceed 35 characters")
        private String statusId;

        /**
         * Original Instruction Identification - Original instruction ID from pain.001.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Original instruction ID must not exceed 35 characters")
        private String originalInstructionId;

        /**
         * Original End-to-End Identification - E2E ID from pain.001.
         * CRITICAL for customer tracking - must be preserved.
         * Max 35 characters.
         */
        @NotNull(message = "Original end-to-end ID is required")
        @Size(max = 35, message = "Original end-to-end ID must not exceed 35 characters")
        private String originalEndToEndId;

        /**
         * Original Transaction Identification - Transaction ID assigned by bank.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Original transaction ID must not exceed 35 characters")
        private String originalTransactionId;

        /**
         * Transaction Status - Status of this transaction.
         *
         * CUSTOMER-FACING STATUS CODES:
         * - ACCP: ✅ "Payment accepted and will be processed"
         * - ACSC: ✅ "Payment successfully completed and funds transferred"
         * - RJCT: ❌ "Payment rejected - see reason below"
         * - PDNG: ⏳ "Payment pending - additional action may be required"
         * - PART: ⚠️ "Payment partially processed"
         *
         * Max 4 characters.
         */
        @NotNull(message = "Transaction status is required")
        @Size(max = 4, message = "Transaction status must not exceed 4 characters")
        private String transactionStatus;

        /**
         * Status Reason Information - Customer-friendly reasons.
         * This is where technical codes are translated to user-friendly messages.
         */
        private List<StatusReasonInformation> statusReasonInformation;

        /**
         * Charges Information - Charges applied (if any).
         */
        private List<ChargesInformation> chargesInformation;

        /**
         * Acceptance Date Time - When accepted (for ACCP/ACSC).
         */
        private ZonedDateTime acceptanceDateTime;

        /**
         * Account Servicer Reference - Bank's internal reference.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Account servicer reference must not exceed 35 characters")
        private String accountServicerReference;

        /**
         * Clearing System Reference - FedNow system reference.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Clearing system reference must not exceed 35 characters")
        private String clearingSystemReference;

        /**
         * Original Transaction Reference - Details of original transaction.
         */
        private OriginalTransactionReference originalTransactionReference;
    }

    /**
     * Status Reason Information - Customer-friendly reason for status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusReasonInformation {

        /**
         * Originator - Who reported this status (typically the bank).
         */
        private PartyIdentification originator;

        /**
         * Reason - Status reason with customer-friendly message.
         */
        private StatusReason reason;

        /**
         * Additional Information - User-friendly explanation.
         * This should help customer understand what happened and what to do next.
         * Max 105 characters per line.
         */
        private List<String> additionalInformation;

        /**
         * Customer-friendly reason code translations.
         * These convert technical ISO 20022 codes to messages customers can understand.
         */
        public static class CustomerFriendlyMessages {

            public static StatusReasonInformation translateRejectionCode(String code) {
                return switch (code) {
                    case "AC01" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("AC01", "Invalid account number"))
                            .additionalInformation(List.of(
                                    "The account number you provided is incorrect",
                                    "Please verify the account number with the recipient and try again"
                            ))
                            .build();

                    case "AC04" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("AC04", "Account closed"))
                            .additionalInformation(List.of(
                                    "The recipient's account has been closed",
                                    "Please contact the recipient for an updated account number"
                            ))
                            .build();

                    case "AC06" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("AC06", "Account blocked"))
                            .additionalInformation(List.of(
                                    "The recipient's account is blocked and cannot receive payments",
                                    "Please contact the recipient or their bank for assistance"
                            ))
                            .build();

                    case "AM04" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("AM04", "Insufficient funds"))
                            .additionalInformation(List.of(
                                    "Your account does not have sufficient funds for this payment",
                                    "Please add funds to your account and try again"
                            ))
                            .build();

                    case "AM09" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("AM09", "Amount exceeds limit"))
                            .additionalInformation(List.of(
                                    "The payment amount exceeds your allowed transaction limit",
                                    "Please contact your bank to increase your limit or split the payment"
                            ))
                            .build();

                    case "AG01" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("AG01", "Transaction not permitted"))
                            .additionalInformation(List.of(
                                    "This transaction is not permitted due to regulatory or compliance requirements",
                                    "Please contact your bank for more information"
                            ))
                            .build();

                    case "FF01" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("FF01", "Invalid payment format"))
                            .additionalInformation(List.of(
                                    "The payment information provided is not in the correct format",
                                    "Please verify all payment details and resubmit"
                            ))
                            .build();

                    case "RC01" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("RC01", "Invalid bank information"))
                            .additionalInformation(List.of(
                                    "The recipient's bank information (routing number) is incorrect",
                                    "Please verify the bank details with the recipient"
                            ))
                            .build();

                    case "RR01" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("RR01", "Missing account information"))
                            .additionalInformation(List.of(
                                    "Required account information is missing",
                                    "Please provide complete account details and try again"
                            ))
                            .build();

                    case "RR03" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("RR03", "Missing recipient information"))
                            .additionalInformation(List.of(
                                    "Required recipient name or address information is missing",
                                    "Please provide complete recipient details and try again"
                            ))
                            .build();

                    case "DUPL" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("DUPL", "Duplicate payment"))
                            .additionalInformation(List.of(
                                    "This payment appears to be a duplicate of a recent transaction",
                                    "If this is intentional, please wait a few minutes and try again"
                            ))
                            .build();

                    case "FRAD" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("FRAD", "Suspected fraudulent payment"))
                            .additionalInformation(List.of(
                                    "This payment has been flagged for security review",
                                    "Please contact your bank to verify this transaction"
                            ))
                            .build();

                    case "MS03" -> StatusReasonInformation.builder()
                            .reason(StatusReason.of("MS03", "Payment requires review"))
                            .additionalInformation(List.of(
                                    "Your payment is being reviewed and will be processed shortly",
                                    "You will receive an update once the review is complete"
                            ))
                            .build();

                    default -> StatusReasonInformation.builder()
                            .reason(StatusReason.of(code, "Payment processing issue"))
                            .additionalInformation(List.of(
                                    "Your payment could not be processed at this time",
                                    "Please contact your bank for more details (Code: " + code + ")"
                            ))
                            .build();
                };
            }

            public static StatusReasonInformation acceptanceMessage() {
                return StatusReasonInformation.builder()
                        .additionalInformation(List.of(
                                "Payment successfully completed and funds transferred",
                                "The recipient should receive the funds shortly"
                        ))
                        .build();
            }

            public static StatusReasonInformation pendingMessage(String reason) {
                return StatusReasonInformation.builder()
                        .additionalInformation(List.of(
                                "Your payment is pending: " + reason,
                                "You will be notified once processing is complete"
                        ))
                        .build();
            }
        }
    }

    /**
     * Number of Transactions Per Status - Count of transactions by status.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NumberOfTransactionsPerStatus {

        /**
         * Detailed Number of Transactions - Count.
         */
        @NotNull(message = "Number of transactions is required")
        private Integer detailedNumberOfTransactions;

        /**
         * Detailed Status - Status being counted.
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
     * Charges Information - Charges applied to transaction.
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
         * Agent - Agent applying charge.
         */
        @Valid
        private AgentIdentification agent;

        /**
         * Type - Type of charge.
         */
        private ChargeType type;

        /**
         * Additional Information - Explanation of charge.
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
         * Proprietary - Bank-specific charge type.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Proprietary must not exceed 35 characters")
        private String proprietary;
    }

    /**
     * Original Transaction Reference - Details of original transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OriginalTransactionReference {

        /**
         * Amount - Original payment amount.
         */
        private Amount amount;

        /**
         * Requested Execution Date - Original requested date.
         */
        private java.time.LocalDate requestedExecutionDate;

        /**
         * Creditor - Recipient information.
         */
        private PartyIdentification creditor;

        /**
         * Creditor Account - Recipient account.
         */
        private AccountIdentification creditorAccount;

        /**
         * Remittance Information - Payment details.
         */
        private RemittanceInformation remittanceInformation;
    }
}
