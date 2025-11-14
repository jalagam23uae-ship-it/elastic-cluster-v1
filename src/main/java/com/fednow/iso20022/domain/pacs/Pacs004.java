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

import java.util.List;

/**
 * pacs.004.001.11 - Payment Return
 *
 * This message is sent to return a previously received payment.
 * Returns can occur for various reasons: incorrect account, closed account,
 * fraud, customer request, etc.
 *
 * Message Flow:
 * Bank → Bank: pacs.004 (return notification)
 * Then Bank → Customer: pain.007 (converted to customer reversal)
 *
 * Common Return Reasons:
 * - AC01: Incorrect account number
 * - AC04: Closed account
 * - AC06: Blocked account
 * - CUST: Customer requested return
 * - FRAD: Fraudulent payment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pacs004 {

    /**
     * Group Header - Message-level information.
     */
    @NotNull(message = "Group header is required")
    @Valid
    private GroupHeader groupHeader;

    /**
     * Transaction Information - Individual return transactions.
     */
    @NotEmpty(message = "At least one transaction is required")
    @Valid
    private List<PaymentReturnTransactionInformation> transactionInformation;

    /**
     * Payment Return Transaction Information - A single return.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentReturnTransactionInformation {

        /**
         * Return Identification - Unique ID for this return.
         * Max 35 characters.
         */
        @NotNull(message = "Return ID is required")
        @Size(max = 35, message = "Return ID must not exceed 35 characters")
        private String returnId;

        /**
         * Original Group Information - Reference to original payment.
         */
        private OriginalGroupInformation originalGroupInformation;

        /**
         * Original Instruction Identification - From original pacs.008.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Original instruction ID must not exceed 35 characters")
        private String originalInstructionId;

        /**
         * Original End-to-End Identification - From original pacs.008.
         * Max 35 characters.
         */
        @NotNull(message = "Original end-to-end ID is required")
        @Size(max = 35, message = "Original end-to-end ID must not exceed 35 characters")
        private String originalEndToEndId;

        /**
         * Original Transaction Identification - From original pacs.008.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Original transaction ID must not exceed 35 characters")
        private String originalTransactionId;

        /**
         * Original UETR - From original pacs.008.
         */
        private java.util.UUID originalUetr;

        /**
         * Original Interbank Settlement Amount - Amount being returned.
         */
        @NotNull(message = "Return amount is required")
        @Valid
        private Amount originalInterbankSettlementAmount;

        /**
         * Original Interbank Settlement Date - Original settlement date.
         */
        private java.time.LocalDate originalInterbankSettlementDate;

        /**
         * Returned Interbank Settlement Amount - Actual returned amount.
         * Usually same as original, but may differ if charges applied.
         */
        @NotNull(message = "Returned amount is required")
        @Valid
        private Amount returnedInterbankSettlementAmount;

        /**
         * Interbank Settlement Date - When return will settle.
         */
        private java.time.LocalDate interbankSettlementDate;

        /**
         * Return Reason Information - Why payment is being returned.
         */
        @NotEmpty(message = "At least one return reason is required")
        @Valid
        private List<ReturnReasonInformation> returnReasonInformation;

        /**
         * Charges Information - Any charges related to the return.
         */
        private List<ChargesInformation> chargesInformation;

        /**
         * Instructing Agent - Agent instructing the return.
         */
        private AgentIdentification instructingAgent;

        /**
         * Instructed Agent - Agent receiving the return instruction.
         */
        private AgentIdentification instructedAgent;

        /**
         * Original Transaction Reference - Details of original transaction.
         */
        private OriginalTransactionReference originalTransactionReference;
    }

    /**
     * Return Reason Information - Reason for return.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnReasonInformation {

        /**
         * Originator - Party that originated the return.
         */
        private PartyIdentification originator;

        /**
         * Reason - Return reason code.
         *
         * Common Return Reason Codes:
         * - AC01: Incorrect account number
         * - AC04: Closed account
         * - AC06: Blocked account
         * - AM04: Insufficient funds
         * - CUST: Requested by customer
         * - DUPL: Duplicate payment
         * - FRAD: Fraudulent originated entry
         * - TECH: Technical problem
         *
         * Max 4 characters.
         */
        @NotNull(message = "Return reason code is required")
        @Size(max = 4, message = "Return reason code must not exceed 4 characters")
        private String reasonCode;

        /**
         * Additional Information - Explanation of return.
         * Max 105 characters per line.
         */
        private List<String> additionalInformation;
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
         * Original Message Identification - ID of original pacs.008.
         * Max 35 characters.
         */
        @NotNull(message = "Original message ID is required")
        @Size(max = 35, message = "Original message ID must not exceed 35 characters")
        private String originalMessageId;

        /**
         * Original Message Name Identification - Type of original message.
         * Typically: "pacs.008.001.11"
         * Max 35 characters.
         */
        @NotNull(message = "Original message name ID is required")
        @Size(max = 35, message = "Original message name ID must not exceed 35 characters")
        private String originalMessageNameIdentification;

        /**
         * Original Creation Date Time - Timestamp of original message.
         */
        private java.time.ZonedDateTime originalCreationDateTime;
    }

    /**
     * Charges Information - Charges related to return.
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
    }

    /**
     * Original Transaction Reference - Full details of original transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OriginalTransactionReference {

        /**
         * Interbank Settlement Amount - Original amount.
         */
        private Amount interbankSettlementAmount;

        /**
         * Interbank Settlement Date - Original settlement date.
         */
        private java.time.LocalDate interbankSettlementDate;

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
}
