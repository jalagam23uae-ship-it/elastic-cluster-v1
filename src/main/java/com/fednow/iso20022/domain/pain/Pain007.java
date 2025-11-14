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

import java.util.List;

/**
 * pain.007.001.11 - Customer Payment Reversal
 *
 * This message is sent by a bank to a customer to notify them of a payment reversal.
 * It's the customer-facing version of pacs.004 (Payment Return).
 *
 * Message Flow:
 * Bank → Customer: pain.007 (this message)
 * (Converted from pacs.004 received from another bank)
 *
 * A "return" at the interbank level becomes a "reversal" for the customer.
 *
 * Common Scenarios:
 * - Recipient account was incorrect or closed
 * - Payment was rejected by recipient's bank
 * - Fraudulent payment detected
 * - Customer requested cancellation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pain007 {

    /**
     * Group Header - Message-level information.
     */
    @NotNull(message = "Group header is required")
    @Valid
    private GroupHeader groupHeader;

    /**
     * Original Group Information - Reference to original pain.001.
     */
    @Valid
    private OriginalGroupInformation originalGroupInformation;

    /**
     * Original Payment Information and Reversal - Reversal details by payment block.
     */
    @NotEmpty(message = "At least one original payment information is required")
    @Valid
    private List<OriginalPaymentInformationAndReversal> originalPaymentInformationAndReversal;

    /**
     * Original Group Information - Reference to original customer payment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OriginalGroupInformation {

        /**
         * Original Message Identification - ID of original pain.001.
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
        private java.time.ZonedDateTime originalCreationDateTime;

        /**
         * Reversal Reason Information - Overall reason for reversal.
         */
        private List<ReversalReasonInformation> reversalReasonInformation;
    }

    /**
     * Original Payment Information and Reversal - Reversal by payment block.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OriginalPaymentInformationAndReversal {

        /**
         * Reversal Payment Information ID - Unique ID for this reversal block.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Reversal payment information ID must not exceed 35 characters")
        private String reversalPaymentInformationId;

        /**
         * Original Payment Information ID - ID from original pain.001.
         * Max 35 characters.
         */
        @NotNull(message = "Original payment information ID is required")
        @Size(max = 35, message = "Original payment information ID must not exceed 35 characters")
        private String originalPaymentInformationId;

        /**
         * Reversal Reason Information - Reason for this payment block reversal.
         */
        private List<ReversalReasonInformation> reversalReasonInformation;

        /**
         * Transaction Information - Individual transaction reversals.
         */
        @NotEmpty(message = "At least one transaction is required")
        @Valid
        private List<PaymentTransactionInformation> transactionInformation;
    }

    /**
     * Payment Transaction Information - A single reversed transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentTransactionInformation {

        /**
         * Reversal Identification - Unique ID for this reversal.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Reversal ID must not exceed 35 characters")
        private String reversalId;

        /**
         * Original Instruction Identification - From original pain.001.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Original instruction ID must not exceed 35 characters")
        private String originalInstructionId;

        /**
         * Original End-to-End Identification - From original pain.001.
         * CRITICAL for customer tracking.
         * Max 35 characters.
         */
        @NotNull(message = "Original end-to-end ID is required")
        @Size(max = 35, message = "Original end-to-end ID must not exceed 35 characters")
        private String originalEndToEndId;

        /**
         * Original Transaction Identification - Bank-assigned transaction ID.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Original transaction ID must not exceed 35 characters")
        private String originalTransactionId;

        /**
         * Reversed Amount - Amount being returned to customer.
         */
        @NotNull(message = "Reversed amount is required")
        @Valid
        private Amount reversedAmount;

        /**
         * Reversal Reason Information - Why this transaction is being reversed.
         */
        @NotEmpty(message = "At least one reversal reason is required")
        @Valid
        private List<ReversalReasonInformation> reversalReasonInformation;

        /**
         * Original Transaction Reference - Details of original transaction.
         */
        private OriginalTransactionReference originalTransactionReference;

        /**
         * Charges Information - Any charges applied to the reversal.
         */
        private List<ChargesInformation> chargesInformation;
    }

    /**
     * Reversal Reason Information - Customer-friendly reversal reason.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReversalReasonInformation {

        /**
         * Originator - Party that originated the reversal.
         */
        private PartyIdentification originator;

        /**
         * Reason Code - Standardized reversal reason.
         *
         * Common Reversal Reason Codes (customer-friendly):
         * - AC01: Incorrect account number provided
         * - AC04: Account closed - funds returned
         * - AC06: Recipient account blocked
         * - AM04: Insufficient funds at recipient
         * - CUST: Payment returned at recipient's request
         * - FRAD: Fraudulent payment detected - funds returned
         * - DUPL: Duplicate payment - funds returned
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Reason code must not exceed 4 characters")
        private String reasonCode;

        /**
         * Additional Information - Customer-friendly explanation.
         * Should help customer understand what happened.
         * Max 105 characters per line.
         */
        private List<String> additionalInformation;

        /**
         * Customer-friendly translations for reversal reasons.
         */
        public static class CustomerFriendlyReversalMessages {

            public static ReversalReasonInformation translateReturnCode(String code) {
                return switch (code) {
                    case "AC01" -> ReversalReasonInformation.builder()
                            .reasonCode("AC01")
                            .additionalInformation(List.of(
                                    "Payment returned: Incorrect account number provided",
                                    "Funds have been returned to your account",
                                    "Please verify the correct account number with the recipient"
                            ))
                            .build();

                    case "AC04" -> ReversalReasonInformation.builder()
                            .reasonCode("AC04")
                            .additionalInformation(List.of(
                                    "Payment returned: Recipient's account has been closed",
                                    "Funds have been returned to your account",
                                    "Please contact the recipient for an updated account number"
                            ))
                            .build();

                    case "AC06" -> ReversalReasonInformation.builder()
                            .reasonCode("AC06")
                            .additionalInformation(List.of(
                                    "Payment returned: Recipient's account is blocked",
                                    "Funds have been returned to your account",
                                    "Please contact the recipient or their bank"
                            ))
                            .build();

                    case "AM04" -> ReversalReasonInformation.builder()
                            .reasonCode("AM04")
                            .additionalInformation(List.of(
                                    "Payment returned: Insufficient funds at recipient",
                                    "Funds have been returned to your account"
                            ))
                            .build();

                    case "CUST" -> ReversalReasonInformation.builder()
                            .reasonCode("CUST")
                            .additionalInformation(List.of(
                                    "Payment returned at recipient's request",
                                    "Funds have been returned to your account"
                            ))
                            .build();

                    case "FRAD" -> ReversalReasonInformation.builder()
                            .reasonCode("FRAD")
                            .additionalInformation(List.of(
                                    "Payment returned: Suspected fraudulent transaction",
                                    "Funds have been returned to your account for your protection"
                            ))
                            .build();

                    case "DUPL" -> ReversalReasonInformation.builder()
                            .reasonCode("DUPL")
                            .additionalInformation(List.of(
                                    "Payment returned: Duplicate payment detected",
                                    "Funds have been returned to your account"
                            ))
                            .build();

                    default -> ReversalReasonInformation.builder()
                            .reasonCode(code)
                            .additionalInformation(List.of(
                                    "Payment has been returned",
                                    "Funds have been returned to your account",
                                    "Please contact your bank for details (Code: " + code + ")"
                            ))
                            .build();
                };
            }
        }
    }

    /**
     * Original Transaction Reference - Details of original payment.
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
         * Creditor - Intended recipient.
         */
        private PartyIdentification creditor;

        /**
         * Creditor Account - Intended recipient account.
         */
        private AccountIdentification creditorAccount;

        /**
         * Creditor Agent - Intended recipient bank.
         */
        private AgentIdentification creditorAgent;

        /**
         * Remittance Information - Original payment details.
         */
        private RemittanceInformation remittanceInformation;
    }

    /**
     * Charges Information - Charges related to reversal.
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
}
