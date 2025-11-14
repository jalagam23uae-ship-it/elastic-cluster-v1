package com.fednow.iso20022.domain.camt;

import com.fednow.iso20022.domain.common.*;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Camt053 - Bank To Customer Statement
 *
 * ISO 20022 message providing official account statement to customers.
 * Used for periodic (daily, weekly, monthly) account statements.
 *
 * Purpose:
 * - Provide official account statement for reconciliation
 * - Document all debits and credits for a period
 * - Legal record of account activity
 * - Support regulatory reporting and compliance
 *
 * Statement Types:
 * - Daily: End-of-day statement
 * - Weekly: Week-end statement
 * - Monthly: Month-end statement
 * - Quarterly: Quarter-end statement
 * - Annual: Year-end statement
 *
 * Key Difference from camt.052:
 * - camt.053: Official STATEMENT (legal record, typically end-of-period)
 * - camt.052: Account REPORT (can be intraday, interim, informational)
 * - camt.054: Account NOTIFICATION (real-time individual alerts)
 *
 * Legal Status:
 * - Official bank statement
 * - Auditable and legally binding
 * - Used for tax reporting
 * - Basis for dispute resolution
 *
 * Use Cases:
 * - Month-end reconciliation
 * - Financial statement preparation
 * - Audit and compliance
 * - Tax reporting
 * - Legal proceedings
 */
@Value
@Builder
public class Camt053 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Account statements.
     */
    List<AccountStatement> statement;

    /**
     * Account statement.
     */
    @Value
    @Builder
    public static class AccountStatement {

        /**
         * Statement identification.
         */
        String statementId;

        /**
         * Statement number (sequential).
         */
        String statementNumber;

        /**
         * Statement creation date/time.
         */
        ZonedDateTime creationDateTime;

        /**
         * From date/time (statement period start).
         */
        ZonedDateTime fromDateTime;

        /**
         * To date/time (statement period end).
         */
        ZonedDateTime toDateTime;

        /**
         * Frequency code (DAIL, WEEK, MNTH, QURT, YEAR).
         */
        String frequency;

        /**
         * Copy duplicate indicator.
         * - CODU: Copy duplicate
         * - DUPL: Duplicate
         */
        String copyDuplicateIndicator;

        /**
         * Account identification.
         */
        AccountIdentification account;

        /**
         * Account owner.
         */
        PartyIdentification accountOwner;

        /**
         * Account servicer (bank).
         */
        AgentIdentification accountServicer;

        /**
         * Balances.
         */
        List<Balance> balance;

        /**
         * Transaction summary.
         */
        TransactionsSummary transactionsSummary;

        /**
         * Transaction entries.
         */
        List<TransactionEntry> entry;
    }

    /**
     * Balance information.
     */
    @Value
    @Builder
    public static class Balance {

        /**
         * Balance type.
         * - OPBD: Opening booked balance
         * - CLBD: Closing booked balance
         * - OPAV: Opening available balance
         * - CLAV: Closing available balance
         * - FWAV: Forward available balance
         * - PRCD: Previously closed booked balance
         */
        String type;

        /**
         * Amount.
         */
        Amount amount;

        /**
         * Credit/Debit indicator.
         * - CRDT: Credit (positive)
         * - DBIT: Debit (negative)
         */
        String creditDebitIndicator;

        /**
         * Date.
         */
        String date;

        /**
         * Date/Time.
         */
        ZonedDateTime dateTime;
    }

    /**
     * Transactions summary.
     */
    @Value
    @Builder
    public static class TransactionsSummary {

        /**
         * Total number of entries.
         */
        Integer totalEntries;

        /**
         * Total number of credit entries.
         */
        Integer totalCreditEntries;

        /**
         * Total credit amount.
         */
        Amount totalCreditAmount;

        /**
         * Total number of debit entries.
         */
        Integer totalDebitEntries;

        /**
         * Total debit amount.
         */
        Amount totalDebitAmount;
    }

    /**
     * Transaction entry.
     */
    @Value
    @Builder
    public static class TransactionEntry {

        /**
         * Entry reference.
         */
        String entryReference;

        /**
         * Amount.
         */
        Amount amount;

        /**
         * Credit/Debit indicator.
         */
        String creditDebitIndicator;

        /**
         * Reversal indicator.
         */
        Boolean reversalIndicator;

        /**
         * Status (BOOK, PDNG).
         */
        String status;

        /**
         * Booking date.
         */
        String bookingDate;

        /**
         * Value date.
         */
        String valueDate;

        /**
         * Account servicer reference.
         */
        String accountServicerReference;

        /**
         * Bank transaction code.
         */
        BankTransactionCode bankTransactionCode;

        /**
         * Commission, charge, or other fees.
         */
        List<Charge> charges;

        /**
         * Entry details (transaction details).
         */
        List<TransactionDetails> entryDetails;

        /**
         * Additional entry information.
         */
        String additionalEntryInformation;
    }

    /**
     * Bank transaction code.
     */
    @Value
    @Builder
    public static class BankTransactionCode {

        /**
         * Domain code (e.g., PMNT for payments).
         */
        String domainCode;

        /**
         * Family code (e.g., RCDT for received credit transfer).
         */
        String familyCode;

        /**
         * Sub-family code.
         */
        String subFamilyCode;

        /**
         * Proprietary code.
         */
        String proprietaryCode;
    }

    /**
     * Charge information.
     */
    @Value
    @Builder
    public static class Charge {

        /**
         * Charge type (e.g., COMM for commission).
         */
        String type;

        /**
         * Amount.
         */
        Amount amount;

        /**
         * Credit/Debit indicator.
         */
        String creditDebitIndicator;
    }

    /**
     * Transaction details.
     */
    @Value
    @Builder
    public static class TransactionDetails {

        /**
         * References.
         */
        TransactionReferences references;

        /**
         * Amount details.
         */
        Amount amount;

        /**
         * Credit/Debit indicator.
         */
        String creditDebitIndicator;

        /**
         * Amount details with breakdown.
         */
        AmountDetails amountDetails;

        /**
         * Related parties.
         */
        RelatedParties relatedParties;

        /**
         * Related agents.
         */
        RelatedAgents relatedAgents;

        /**
         * Purpose.
         */
        String purpose;

        /**
         * Remittance information.
         */
        RemittanceInformation remittanceInformation;

        /**
         * Return information (if return/reject).
         */
        ReturnInformation returnInformation;

        /**
         * Related dates.
         */
        RelatedDates relatedDates;
    }

    /**
     * Transaction references.
     */
    @Value
    @Builder
    public static class TransactionReferences {
        String messageId;
        String accountServicerReference;
        String paymentInformationId;
        String instructionId;
        String endToEndId;
        String transactionId;
        String uetr;
        String mandateId;
        String chequeNumber;
        String clearingSystemReference;
        String proprietaryReference;
    }

    /**
     * Amount details.
     */
    @Value
    @Builder
    public static class AmountDetails {
        Amount instructedAmount;
        Amount transactionAmount;
        Amount counterValueAmount;
        Amount announcedPostingAmount;
        List<Amount> proprietaryAmount;
    }

    /**
     * Related parties.
     */
    @Value
    @Builder
    public static class RelatedParties {
        PartyIdentification debtor;
        AccountIdentification debtorAccount;
        PartyIdentification ultimateDebtor;
        PartyIdentification creditor;
        AccountIdentification creditorAccount;
        PartyIdentification ultimateCreditor;
    }

    /**
     * Related agents.
     */
    @Value
    @Builder
    public static class RelatedAgents {
        AgentIdentification debtorAgent;
        AgentIdentification creditorAgent;
        AgentIdentification intermediaryAgent1;
        AgentIdentification intermediaryAgent2;
        AgentIdentification instructingAgent;
        AgentIdentification instructedAgent;
    }

    /**
     * Return information.
     */
    @Value
    @Builder
    public static class ReturnInformation {
        String returnReasonCode;
        List<String> additionalReturnReasonInformation;
        PartyIdentification returnOriginator;
        Amount returnedAmount;
    }

    /**
     * Related dates.
     */
    @Value
    @Builder
    public static class RelatedDates {
        String acceptanceDateTime;
        String interbankSettlementDate;
        String requestedExecutionDate;
        String requestedCollectionDate;
    }
}
