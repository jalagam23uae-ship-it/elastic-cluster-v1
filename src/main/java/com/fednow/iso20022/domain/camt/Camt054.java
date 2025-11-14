package com.fednow.iso20022.domain.camt;

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
 * camt.054.001.10 - Bank to Customer Debit Credit Notification
 *
 * This message is sent by a bank to a customer to notify them of individual
 * debit or credit entries to their account. It's a real-time notification
 * of account activity.
 *
 * Message Flow:
 * Bank → Customer: camt.054 (this message)
 * (Generated from pacs.008 or other payment messages)
 *
 * Use Cases:
 * - Notify customer of incoming payment (credit notification)
 * - Notify customer of outgoing payment (debit notification)
 * - Real-time account activity alerts
 * - Transaction confirmations
 *
 * Difference from camt.052/053:
 * - camt.054: Individual transaction notifications (real-time)
 * - camt.052: Intraday account report (aggregated)
 * - camt.053: End-of-day account statement (aggregated)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Camt054 {

    /**
     * Group Header - Message-level information.
     */
    @NotNull(message = "Group header is required")
    @Valid
    private GroupHeader groupHeader;

    /**
     * Notification - One or more account notifications.
     * Typically one notification per account.
     */
    @NotEmpty(message = "At least one notification is required")
    @Valid
    private List<AccountNotification> notification;

    /**
     * Account Notification - Notification for a specific account.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountNotification {

        /**
         * Identification - Unique ID for this notification.
         * Max 35 characters.
         */
        @NotNull(message = "Notification ID is required")
        @Size(max = 35, message = "Notification ID must not exceed 35 characters")
        private String identification;

        /**
         * Creation Date Time - When this notification was created.
         */
        @NotNull(message = "Creation date time is required")
        private ZonedDateTime creationDateTime;

        /**
         * Account - The account this notification is for.
         */
        @NotNull(message = "Account is required")
        @Valid
        private CashAccount account;

        /**
         * Related Account - Related account (if applicable).
         */
        private CashAccount relatedAccount;

        /**
         * Entry - Individual debit/credit entries.
         */
        @NotEmpty(message = "At least one entry is required")
        @Valid
        private List<ReportEntry> entry;

        /**
         * Additional Notification Information - Extra details.
         * Max 500 characters.
         */
        @Size(max = 500, message = "Additional information must not exceed 500 characters")
        private String additionalNotificationInformation;
    }

    /**
     * Cash Account - Account information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashAccount {

        /**
         * Identification - Account identifier.
         */
        @NotNull(message = "Account identification is required")
        @Valid
        private AccountIdentification identification;

        /**
         * Type - Type of account (CACC, SVGS, etc.).
         * Max 4 characters.
         */
        @Size(max = 4, message = "Account type must not exceed 4 characters")
        private String type;

        /**
         * Currency - Account currency (USD for FedNow).
         * ISO 4217 alphabetic code (3 characters).
         */
        @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
        private String currency;

        /**
         * Name - Account name.
         * Max 70 characters.
         */
        @Size(max = 70, message = "Account name must not exceed 70 characters")
        private String name;

        /**
         * Owner - Account owner.
         */
        private PartyIdentification owner;

        /**
         * Servicer - Bank servicing the account.
         */
        private AgentIdentification servicer;
    }

    /**
     * Report Entry - A single debit or credit entry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportEntry {

        /**
         * Entry Reference - Unique reference for this entry.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Entry reference must not exceed 35 characters")
        private String entryReference;

        /**
         * Amount - Entry amount.
         */
        @NotNull(message = "Entry amount is required")
        @Valid
        private Amount amount;

        /**
         * Credit Debit Indicator - Whether this is a credit or debit.
         *
         * CRDT: Credit (funds received, balance increases)
         * DBIT: Debit (funds sent, balance decreases)
         *
         * Max 4 characters.
         */
        @NotNull(message = "Credit/debit indicator is required")
        @Size(max = 4, message = "Credit/debit indicator must not exceed 4 characters")
        private String creditDebitIndicator;

        /**
         * Reversal Indicator - Whether this entry is a reversal.
         * true: This is a reversal of a previous entry
         * false: Normal entry
         */
        private Boolean reversalIndicator;

        /**
         * Status - Booking status of the entry.
         *
         * BOOK: Booked (final)
         * PDNG: Pending
         * INFO: Information only
         *
         * Max 4 characters.
         */
        @NotNull(message = "Status is required")
        @Size(max = 4, message = "Status must not exceed 4 characters")
        private String status;

        /**
         * Booking Date - Date when entry was booked.
         */
        private DateAndDateTime bookingDate;

        /**
         * Value Date - Date when funds are available.
         */
        private DateAndDateTime valueDate;

        /**
         * Account Servicer Reference - Bank's internal reference.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Account servicer reference must not exceed 35 characters")
        private String accountServicerReference;

        /**
         * Bank Transaction Code - Categorizes the transaction type.
         */
        private BankTransactionCode bankTransactionCode;

        /**
         * Charges - Charges applied to this entry.
         */
        private Charges charges;

        /**
         * Technical Input Channel - How transaction was received.
         *
         * Common values:
         * - FIIN: File input
         * - ONLN: Online
         * - TELA: Telephone
         * - MOBL: Mobile
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Technical input channel must not exceed 4 characters")
        private String technicalInputChannel;

        /**
         * Entry Details - Detailed information about this entry.
         */
        private List<EntryDetails> entryDetails;

        /**
         * Additional Entry Information - Free-form additional details.
         * Max 500 characters.
         */
        @Size(max = 500, message = "Additional entry information must not exceed 500 characters")
        private String additionalEntryInformation;
    }

    /**
     * Date and Date Time - Flexible date/datetime representation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateAndDateTime {

        /**
         * Date - Just a date (no time).
         */
        private java.time.LocalDate date;

        /**
         * Date Time - Full timestamp.
         */
        private ZonedDateTime dateTime;
    }

    /**
     * Bank Transaction Code - Categorizes the transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BankTransactionCode {

        /**
         * Domain - High-level category.
         *
         * Common domains:
         * - PMNT: Payments
         * - LDAS: Loans and deposits
         * - CMDT: Commodities
         * - SECU: Securities
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Domain must not exceed 4 characters")
        private String domain;

        /**
         * Family - Sub-category within domain.
         *
         * For PMNT domain:
         * - RCDT: Received credit transfer
         * - ICDT: Issued credit transfer
         * - RDDT: Received direct debit
         * - IDDT: Issued direct debit
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Family must not exceed 4 characters")
        private String family;

        /**
         * Sub Family - Further categorization.
         *
         * For RCDT family:
         * - RRTN: Return
         * - ESCT: SEPA credit transfer
         * - etc.
         *
         * Max 4 characters.
         */
        @Size(max = 4, message = "Sub family must not exceed 4 characters")
        private String subFamily;

        /**
         * Proprietary - Bank-specific code.
         */
        private ProprietaryBankTransactionCode proprietary;

        /**
         * Common bank transaction codes for FedNow.
         */
        public static class FedNowCodes {
            /**
             * Received credit transfer (incoming payment).
             */
            public static BankTransactionCode receivedCreditTransfer() {
                return BankTransactionCode.builder()
                        .domain("PMNT")
                        .family("RCDT")
                        .build();
            }

            /**
             * Issued credit transfer (outgoing payment).
             */
            public static BankTransactionCode issuedCreditTransfer() {
                return BankTransactionCode.builder()
                        .domain("PMNT")
                        .family("ICDT")
                        .build();
            }

            /**
             * Return of credit transfer.
             */
            public static BankTransactionCode returnedCreditTransfer() {
                return BankTransactionCode.builder()
                        .domain("PMNT")
                        .family("RCDT")
                        .subFamily("RRTN")
                        .build();
            }
        }
    }

    /**
     * Proprietary Bank Transaction Code - Bank-specific transaction code.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProprietaryBankTransactionCode {

        /**
         * Code - The proprietary code.
         * Max 35 characters.
         */
        @NotNull(message = "Proprietary code is required")
        @Size(max = 35, message = "Proprietary code must not exceed 35 characters")
        private String code;

        /**
         * Issuer - Who issued this code (typically the bank).
         * Max 35 characters.
         */
        @Size(max = 35, message = "Issuer must not exceed 35 characters")
        private String issuer;
    }

    /**
     * Charges - Charges applied to entry.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Charges {

        /**
         * Total Charges and Tax Amount - Total of all charges.
         */
        private Amount totalChargesAndTaxAmount;

        /**
         * Record - Individual charge records.
         */
        private List<ChargeRecord> record;
    }

    /**
     * Charge Record - A single charge.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChargeRecord {

        /**
         * Amount - Charge amount.
         */
        @NotNull(message = "Charge amount is required")
        @Valid
        private Amount amount;

        /**
         * Credit Debit Indicator - Whether charge is debit or credit.
         * Typically DBIT (charge reduces balance).
         * Max 4 characters.
         */
        @Size(max = 4, message = "Credit/debit indicator must not exceed 4 characters")
        private String creditDebitIndicator;

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
     * Entry Details - Detailed transaction information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntryDetails {

        /**
         * Batch - Batch information (if part of a batch).
         */
        private BatchInformation batch;

        /**
         * Transaction Details - Details of the underlying transaction.
         */
        private List<TransactionDetails> transactionDetails;
    }

    /**
     * Batch Information - Information about a batch of transactions.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchInformation {

        /**
         * Message Identification - ID of the batch message.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Message ID must not exceed 35 characters")
        private String messageIdentification;

        /**
         * Number of Transactions - Count of transactions in batch.
         */
        private Integer numberOfTransactions;

        /**
         * Total Amount - Total amount of all transactions in batch.
         */
        private Amount totalAmount;
    }

    /**
     * Transaction Details - Details of underlying payment transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDetails {

        /**
         * References - Transaction references (E2E ID, UETR, etc.).
         */
        private TransactionReferences references;

        /**
         * Amount Details - Amount information.
         */
        private AmountDetails amountDetails;

        /**
         * Related Parties - Parties involved in the transaction.
         */
        private RelatedParties relatedParties;

        /**
         * Related Agents - Banks involved in the transaction.
         */
        private RelatedAgents relatedAgents;

        /**
         * Purpose - Purpose of the transaction.
         */
        private Purpose purpose;

        /**
         * Remittance Information - Payment details/invoice info.
         */
        private RemittanceInformation remittanceInformation;

        /**
         * Return Information - If this is a return, why it was returned.
         */
        private ReturnInformation returnInformation;

        /**
         * Additional Transaction Information - Free-form details.
         * Max 500 characters.
         */
        @Size(max = 500, message = "Additional information must not exceed 500 characters")
        private String additionalTransactionInformation;
    }

    /**
     * Transaction References - IDs for tracking.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionReferences {

        /**
         * Message Identification - ID of the original payment message.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Message ID must not exceed 35 characters")
        private String messageIdentification;

        /**
         * Account Servicer Reference - Bank's internal reference.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Account servicer reference must not exceed 35 characters")
        private String accountServicerReference;

        /**
         * Payment Information Identification - Payment info block ID.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Payment information ID must not exceed 35 characters")
        private String paymentInformationIdentification;

        /**
         * Instruction Identification - Instruction ID.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Instruction ID must not exceed 35 characters")
        private String instructionIdentification;

        /**
         * End-to-End Identification - E2E ID from original payment.
         * CRITICAL for tracking.
         * Max 35 characters.
         */
        @Size(max = 35, message = "End-to-end ID must not exceed 35 characters")
        private String endToEndIdentification;

        /**
         * Transaction Identification - Transaction ID.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Transaction ID must not exceed 35 characters")
        private String transactionIdentification;

        /**
         * UETR - Unique End-to-End Transaction Reference.
         */
        private java.util.UUID uetr;

        /**
         * Clearing System Reference - FedNow clearing reference.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Clearing system reference must not exceed 35 characters")
        private String clearingSystemReference;
    }

    /**
     * Amount Details - Amount information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AmountDetails {

        /**
         * Instructed Amount - Original instructed amount.
         */
        private Amount instructedAmount;

        /**
         * Transaction Amount - Actual transaction amount.
         */
        private Amount transactionAmount;

        /**
         * Interbank Settlement Amount - Settlement amount between banks.
         */
        private Amount interbankSettlementAmount;
    }

    /**
     * Related Parties - Parties involved in transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedParties {

        /**
         * Debtor - Party that sent the funds.
         */
        private PartyIdentification debtor;

        /**
         * Debtor Account - Debtor's account.
         */
        private AccountIdentification debtorAccount;

        /**
         * Ultimate Debtor - Ultimate debtor (if different).
         */
        private PartyIdentification ultimateDebtor;

        /**
         * Creditor - Party that received the funds.
         */
        private PartyIdentification creditor;

        /**
         * Creditor Account - Creditor's account.
         */
        private AccountIdentification creditorAccount;

        /**
         * Ultimate Creditor - Ultimate creditor (if different).
         */
        private PartyIdentification ultimateCreditor;
    }

    /**
     * Related Agents - Banks involved in transaction.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedAgents {

        /**
         * Debtor Agent - Debtor's bank.
         */
        private AgentIdentification debtorAgent;

        /**
         * Creditor Agent - Creditor's bank.
         */
        private AgentIdentification creditorAgent;

        /**
         * Intermediary Agent - Intermediary bank (if applicable).
         */
        private AgentIdentification intermediaryAgent;
    }

    /**
     * Purpose - Purpose of transaction.
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
     * Return Information - Information about a returned payment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnInformation {

        /**
         * Original Bank Transaction Code - Original transaction type.
         */
        private BankTransactionCode originalBankTransactionCode;

        /**
         * Reason - Reason for return.
         */
        private ReturnReason reason;

        /**
         * Additional Information - Extra details about return.
         * Max 105 characters per line.
         */
        private List<String> additionalInformation;
    }

    /**
     * Return Reason - Reason for payment return.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnReason {

        /**
         * Code - Standardized return reason code.
         * Max 4 characters.
         */
        @Size(max = 4, message = "Return reason code must not exceed 4 characters")
        private String code;

        /**
         * Proprietary - Proprietary return reason.
         * Max 35 characters.
         */
        @Size(max = 35, message = "Proprietary return reason must not exceed 35 characters")
        private String proprietary;
    }
}
