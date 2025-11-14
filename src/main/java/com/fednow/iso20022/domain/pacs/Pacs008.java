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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * pacs.008.001.11 - FI to FI Customer Credit Transfer
 *
 * This message is sent between financial institutions to execute a credit transfer
 * through the FedNow system. It's the core message for interbank payments.
 *
 * Message Flow:
 * Bank → FedNow: pacs.008 (this message)
 * FedNow → Bank: pacs.002 (status report)
 *
 * This is the MOST IMPORTANT message in the FedNow system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pacs008 {

    /**
     * Group Header - Message-level information for interbank transfer.
     */
    @NotNull(message = "Group header is required")
    @Valid
    private GroupHeader groupHeader;

    /**
     * Credit Transfer Transaction Information - Individual transactions.
     * A pacs.008 can contain multiple transactions.
     */
    @NotEmpty(message = "At least one credit transfer transaction is required")
    @Valid
    private List<CreditTransferTransactionInformation> creditTransferTransactionInformation;

    /**
     * Credit Transfer Transaction Information - A single interbank transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditTransferTransactionInformation {

        /**
         * Payment Identification - Unique identifiers for tracking.
         * MANDATORY: Must include UETR, End-to-End ID, Transaction ID.
         */
        @NotNull(message = "Payment identification is required")
        @Valid
        private PaymentIdentification paymentIdentification;

        /**
         * Payment Type Information - Type and priority of payment.
         */
        private PaymentTypeInformation paymentTypeInformation;

        /**
         * Interbank Settlement Amount - Amount to be settled between banks.
         * For USD payments, this is the same as Instructed Amount.
         */
        @NotNull(message = "Interbank settlement amount is required")
        @Valid
        private Amount interbankSettlementAmount;

        /**
         * Interbank Settlement Date - Date of settlement.
         * For FedNow: Always current date (T+0).
         */
        @NotNull(message = "Interbank settlement date is required")
        private LocalDate interbankSettlementDate;

        /**
         * Settlement Priority - Priority for settlement.
         *
         * Common codes:
         * - HIGH: High priority
         * - NORM: Normal priority (FedNow default)
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Settlement priority must not exceed 4 characters")
        private String settlementPriority;

        /**
         * Settlement Time Indication - When settlement should occur.
         */
        private SettlementTimeIndication settlementTimeIndication;

        /**
         * Instructed Amount - Original amount instructed (from customer).
         */
        @Valid
        private Amount instructedAmount;

        /**
         * Exchange Rate - If currency conversion involved (not typically used for USD FedNow).
         */
        private BigDecimal exchangeRate;

        /**
         * Charge Bearer - Who bears transaction charges.
         *
         * SHAR: Shared (FedNow default)
         * DEBT: Borne by debtor
         * CRED: Borne by creditor
         * SLEV: Service level
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Charge bearer must not exceed 4 characters")
        @Builder.Default
        private String chargeBearer = "SHAR";

        /**
         * Charges Information - Details about charges (if any).
         */
        private List<ChargesInformation> chargesInformation;

        /**
         * Previous Instructing Agent - Previous agent in the chain (if applicable).
         */
        private AgentIdentification previousInstructingAgent;

        /**
         * Instructing Agent - Current instructing agent (sending bank).
         */
        private AgentIdentification instructingAgent;

        /**
         * Instructed Agent - Agent being instructed (receiving bank).
         */
        private AgentIdentification instructedAgent;

        /**
         * Intermediary Agent 1 - First intermediary bank (if applicable).
         */
        private AgentIdentification intermediaryAgent1;

        /**
         * Intermediary Agent 2 - Second intermediary bank (if applicable).
         */
        private AgentIdentification intermediaryAgent2;

        /**
         * Debtor - Party that owes the amount.
         */
        @NotNull(message = "Debtor is required")
        @Valid
        private PartyIdentification debtor;

        /**
         * Debtor Account - Account to be debited.
         */
        @NotNull(message = "Debtor account is required")
        @Valid
        private AccountIdentification debtorAccount;

        /**
         * Debtor Agent - Debtor's financial institution.
         */
        @NotNull(message = "Debtor agent is required")
        @Valid
        private AgentIdentification debtorAgent;

        /**
         * Debtor Agent Account - Account of the debtor agent (if applicable).
         */
        private AccountIdentification debtorAgentAccount;

        /**
         * Previous Instructing Agent Account - Account of previous agent (if applicable).
         */
        private AccountIdentification previousInstructingAgentAccount;

        /**
         * Instructing Agent Account - Account of instructing agent (if applicable).
         */
        private AccountIdentification instructingAgentAccount;

        /**
         * Instructed Agent Account - Account of instructed agent (if applicable).
         */
        private AccountIdentification instructedAgentAccount;

        /**
         * Intermediary Agent 1 Account - Account of first intermediary (if applicable).
         */
        private AccountIdentification intermediaryAgent1Account;

        /**
         * Intermediary Agent 2 Account - Account of second intermediary (if applicable).
         */
        private AccountIdentification intermediaryAgent2Account;

        /**
         * Creditor Agent - Creditor's financial institution (beneficiary bank).
         */
        @NotNull(message = "Creditor agent is required")
        @Valid
        private AgentIdentification creditorAgent;

        /**
         * Creditor Agent Account - Account of creditor agent (if applicable).
         */
        private AccountIdentification creditorAgentAccount;

        /**
         * Creditor - Beneficiary of the payment.
         */
        @NotNull(message = "Creditor is required")
        @Valid
        private PartyIdentification creditor;

        /**
         * Creditor Account - Account to be credited.
         */
        @NotNull(message = "Creditor account is required")
        @Valid
        private AccountIdentification creditorAccount;

        /**
         * Ultimate Debtor - Ultimate party that owes (if different from debtor).
         */
        private PartyIdentification ultimateDebtor;

        /**
         * Ultimate Creditor - Ultimate beneficiary (if different from creditor).
         */
        private PartyIdentification ultimateCreditor;

        /**
         * Purpose - Purpose of the payment.
         */
        private Purpose purpose;

        /**
         * Remittance Information - Payment details/invoice information.
         */
        private RemittanceInformation remittanceInformation;

        /**
         * Related Remittance Information - Additional remittance details.
         */
        private List<RemittanceLocation> relatedRemittanceInformation;

        /**
         * Regulatory Reporting - Compliance and regulatory information.
         */
        private List<RegulatoryReporting> regulatoryReporting;

        /**
         * Supplementary Data - Additional proprietary data.
         */
        private List<SupplementaryData> supplementaryData;
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
         * Instruction Priority - Priority level.
         * HIGH, NORM, URGP
         * Max 4 characters.
         */
        @Size(max = 4, message = "Instruction priority must not exceed 4 characters")
        private String instructionPriority;

        /**
         * Clearing Channel - Channel for clearing.
         * RTGS: Real-time gross settlement (FedNow uses this)
         * Max 4 characters.
         */
        @Size(max = 4, message = "Clearing channel must not exceed 4 characters")
        private String clearingChannel;

        /**
         * Service Level - Level of service.
         * SDVA: Same day value (FedNow)
         * Max 4 characters.
         */
        @Size(max = 4, message = "Service level must not exceed 4 characters")
        private String serviceLevel;

        /**
         * Local Instrument - Local clearing system code.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Local instrument must not exceed 35 characters")
        private String localInstrument;

        /**
         * Category Purpose - High-level payment category.
         * CASH, CORT, INTC, SUPP, SALA, etc.
         * Max 4 characters.
         */
        @Size(max = 4, message = "Category purpose must not exceed 4 characters")
        private String categoryPurpose;
    }

    /**
     * Settlement Time Indication - Timing information for settlement.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettlementTimeIndication {

        /**
         * Debit Date Time - Exact time for debiting.
         */
        private ZonedDateTime debitDateTime;

        /**
         * Credit Date Time - Exact time for crediting.
         */
        private ZonedDateTime creditDateTime;
    }

    /**
     * Charges Information - Details about transaction charges.
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
         * Agent - Agent that applies the charge.
         */
        @Valid
        private AgentIdentification agent;

        /**
         * Type - Type of charge.
         */
        private ChargeType type;
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
        @Size(max = 4, message = "Charge type code must not exceed 4 characters")
        private String code;

        /**
         * Proprietary - Proprietary charge type.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Proprietary charge type must not exceed 35 characters")
        private String proprietary;
    }

    /**
     * Purpose - Specific purpose of payment.
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
         * Proprietary - Proprietary purpose.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Proprietary purpose must not exceed 35 characters")
        private String proprietary;
    }

    /**
     * Remittance Location - Location of remittance information document.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RemittanceLocation {

        /**
         * Remote Location Identification - URL or URI of document.
         * Max 2048 characters.
         */
        @Size(max = 2048, message = "Remote location must not exceed 2048 characters")
        private String remoteLocationIdentification;

        /**
         * Method - How to access the document.
         * FAXI, EDIC, URID, EMAL, POST, SMSM
         * Max 4 characters.
         */
        @Size(max = 4, message = "Method must not exceed 4 characters")
        private String method;
    }

    /**
     * Regulatory Reporting - Compliance information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulatoryReporting {

        /**
         * Debit Credit Reporting Indicator - DEBT or CRED.
         */
        @Size(max = 4, message = "Indicator must not exceed 4 characters")
        private String debitCreditReportingIndicator;

        /**
         * Authority - Regulatory authority.
         */
        private RegulatoryAuthority authority;

        /**
         * Details - Regulatory details.
         */
        private List<StructuredRegulatoryReporting> details;
    }

    /**
     * Regulatory Authority - Authority requiring reporting.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegulatoryAuthority {

        /**
         * Name - Authority name.
         * Max 140 characters.
         */
        @Size(max = 140, message = "Name must not exceed 140 characters")
        private String name;

        /**
         * Country - Authority country.
         * ISO 3166-1 alpha-2 (2 characters).
         */
        @Size(min = 2, max = 2, message = "Country must be exactly 2 characters")
        private String country;
    }

    /**
     * Structured Regulatory Reporting - Detailed regulatory info.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StructuredRegulatoryReporting {

        /**
         * Type - Reporting type.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Type must not exceed 35 characters")
        private String type;

        /**
         * Information - Regulatory information values.
         */
        private List<String> information;

        /**
         * Amount - Related amount.
         */
        private Amount amount;

        /**
         * Country - Related country.
         * ISO 3166-1 alpha-2 (2 characters).
         */
        @Size(min = 2, max = 2, message = "Country must be exactly 2 characters")
        private String country;
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
         * Placement - Location in message.
         * Max 350 characters.
         */
        @Size(max = 350, message = "Placement must not exceed 350 characters")
        private String placement;

        /**
         * Envelope - Contains proprietary data (any XML content).
         */
        private String envelope;
    }
}
