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
 * pain.001.001.11 - Customer Credit Transfer Initiation
 *
 * This message is sent by a customer (debtor) to their bank to initiate one or more
 * credit transfers. The bank then converts this to pacs.008 for FedNow processing.
 *
 * Message Flow:
 * Customer → Bank: pain.001 (this message)
 * Bank → FedNow: pacs.008 (converted)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pain001 {

    /**
     * Group Header - Message-level information.
     */
    @NotNull(message = "Group header is required")
    @Valid
    private GroupHeader groupHeader;

    /**
     * Payment Information - Contains one or more payment instructions.
     * A pain.001 can contain multiple payment information blocks.
     */
    @NotEmpty(message = "At least one payment information block is required")
    @Valid
    private List<PaymentInformation> paymentInformation;

    /**
     * Payment Information - A single payment instruction block within pain.001.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInformation {

        /**
         * Payment Information ID - Unique identifier for this payment instruction.
         * Max 35 characters.
         */
        @NotNull(message = "Payment information ID is required")
        @Size(max = 35, message = "Payment information ID must not exceed 35 characters")
        private String paymentInformationId;

        /**
         * Payment Method - Method of payment.
         * TRF: Transfer (used for credit transfers)
         */
        @NotNull(message = "Payment method is required")
        @Size(max = 3, message = "Payment method must not exceed 3 characters")
        @Builder.Default
        private String paymentMethod = "TRF";

        /**
         * Batch Booking - Indicates if transactions should be booked individually or as batch.
         * true: Book as single entry (batch)
         * false: Book each transaction individually
         */
        private Boolean batchBooking;

        /**
         * Number of Transactions - Total number of transactions in this payment block.
         */
        private Integer numberOfTransactions;

        /**
         * Control Sum - Total of all individual amounts in this payment block.
         */
        private java.math.BigDecimal controlSum;

        /**
         * Payment Type Information - Additional information about the payment type.
         */
        private PaymentTypeInformation paymentTypeInformation;

        /**
         * Requested Execution Date - Date when the customer wants the payment executed.
         * For FedNow: Must be current date (T+0).
         */
        @NotNull(message = "Requested execution date is required")
        private java.time.LocalDate requestedExecutionDate;

        /**
         * Debtor - The party initiating the payment (customer).
         */
        @NotNull(message = "Debtor is required")
        @Valid
        private PartyIdentification debtor;

        /**
         * Debtor Account - The account to be debited.
         */
        @NotNull(message = "Debtor account is required")
        @Valid
        private AccountIdentification debtorAccount;

        /**
         * Debtor Agent - The debtor's bank.
         */
        @NotNull(message = "Debtor agent is required")
        @Valid
        private AgentIdentification debtorAgent;

        /**
         * Ultimate Debtor - The ultimate party that owes the amount (if different from debtor).
         */
        private PartyIdentification ultimateDebtor;

        /**
         * Charge Bearer - Specifies which party bears the charges.
         *
         * Common codes:
         * - SHAR: Shared between debtor and creditor (FedNow default)
         * - DEBT: Borne by debtor
         * - CRED: Borne by creditor
         * - SLEV: Service level agreement
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Charge bearer must not exceed 4 characters")
        @Builder.Default
        private String chargeBearer = "SHAR";

        /**
         * Credit Transfer Transaction Information - Individual transactions within this payment.
         */
        @NotEmpty(message = "At least one credit transfer transaction is required")
        @Valid
        private List<CreditTransferTransactionInformation> creditTransferTransactionInformation;
    }

    /**
     * Credit Transfer Transaction Information - A single credit transfer transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditTransferTransactionInformation {

        /**
         * Payment Identification - Identifiers for this transaction.
         */
        @NotNull(message = "Payment identification is required")
        @Valid
        private PaymentIdentification paymentIdentification;

        /**
         * Payment Type Information - Type and category of payment.
         */
        private PaymentTypeInformation paymentTypeInformation;

        /**
         * Amount - The instructed amount to be transferred.
         */
        @NotNull(message = "Amount is required")
        @Valid
        private Amount amount;

        /**
         * Charge Bearer - Overrides payment-level charge bearer if specified.
         */
        @Size(max = 4, message = "Charge bearer must not exceed 4 characters")
        private String chargeBearer;

        /**
         * Ultimate Debtor - The ultimate party that owes the amount.
         */
        private PartyIdentification ultimateDebtor;

        /**
         * Creditor Agent - The creditor's bank (beneficiary bank).
         */
        @NotNull(message = "Creditor agent is required")
        @Valid
        private AgentIdentification creditorAgent;

        /**
         * Creditor - The beneficiary of the payment.
         */
        @NotNull(message = "Creditor is required")
        @Valid
        private PartyIdentification creditor;

        /**
         * Creditor Account - The account to be credited.
         */
        @NotNull(message = "Creditor account is required")
        @Valid
        private AccountIdentification creditorAccount;

        /**
         * Ultimate Creditor - The ultimate party to be paid (if different from creditor).
         */
        private PartyIdentification ultimateCreditor;

        /**
         * Purpose - Purpose of the payment.
         */
        private Purpose purpose;

        /**
         * Remittance Information - Information about the payment purpose/invoice.
         */
        private RemittanceInformation remittanceInformation;

        /**
         * Regulatory Reporting - Regulatory/compliance information.
         */
        private List<RegulatoryReporting> regulatoryReporting;
    }

    /**
     * Payment Type Information - Classification of the payment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentTypeInformation {

        /**
         * Instruction Priority - Priority level of the payment.
         *
         * Common codes:
         * - HIGH: High priority
         * - NORM: Normal priority (default for FedNow)
         * - URGP: Urgent payment
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Instruction priority must not exceed 4 characters")
        private String instructionPriority;

        /**
         * Service Level - Level of service for the payment.
         *
         * For FedNow: "SDVA" (Same Day Value)
         * Max 4 characters.
         */
        @Size(max = 4, message = "Service level must not exceed 4 characters")
        private String serviceLevel;

        /**
         * Local Instrument - Local clearing instrument or system.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Local instrument must not exceed 35 characters")
        private String localInstrument;

        /**
         * Category Purpose - High-level category of the payment purpose.
         *
         * Common codes:
         * - CASH: Cash management transfer
         * - CORT: Trade settlement
         * - INTC: Intra-company payment
         * - SUPP: Supplier payment
         * - SALA: Salary payment
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Category purpose must not exceed 4 characters")
        private String categoryPurpose;
    }

    /**
     * Purpose - Specific purpose of the payment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Purpose {

        /**
         * Code - Standardized purpose code.
         * Max 4 characters.
         */
        @Size(max = 4, message = "Purpose code must not exceed 4 characters")
        private String code;

        /**
         * Proprietary - Proprietary purpose code.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Proprietary purpose must not exceed 35 characters")
        private String proprietary;
    }

    /**
     * Regulatory Reporting - Regulatory and compliance information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulatoryReporting {

        /**
         * Debit Credit Reporting Indicator - Indicates if reporting is for debit or credit.
         * CRED or DEBT
         */
        @Size(max = 4, message = "Indicator must not exceed 4 characters")
        private String debitCreditReportingIndicator;

        /**
         * Authority - Regulatory authority requiring the report.
         */
        private RegulatoryAuthority authority;

        /**
         * Details - Specific regulatory reporting details.
         */
        private List<StructuredRegulatoryReporting> details;
    }

    /**
     * Regulatory Authority - Authority requiring regulatory reporting.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulatoryAuthority {

        /**
         * Name - Name of the regulatory authority.
         * Max 140 characters.
         */
        @Size(max = 140, message = "Name must not exceed 140 characters")
        private String name;

        /**
         * Country - Country of the regulatory authority.
         * ISO 3166-1 alpha-2 (2 characters).
         */
        @Size(min = 2, max = 2, message = "Country must be exactly 2 characters")
        private String country;
    }

    /**
     * Structured Regulatory Reporting - Detailed regulatory information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StructuredRegulatoryReporting {

        /**
         * Type - Type of regulatory reporting.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Type must not exceed 35 characters")
        private String type;

        /**
         * Information - Regulatory information details.
         * Max 35 characters per entry.
         */
        private List<String> information;

        /**
         * Amount - Amount related to the regulatory reporting.
         */
        private Amount amount;

        /**
         * Country - Country code related to the reporting.
         * ISO 3166-1 alpha-2 (2 characters).
         */
        @Size(min = 2, max = 2, message = "Country must be exactly 2 characters")
        private String country;
    }
}
