package com.fednow.iso20022.domain.camt;

import com.fednow.iso20022.domain.common.*;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Camt052 - Bank To Customer Account Report
 *
 * ISO 20022 message providing account activity report to customers.
 * Used for intraday and end-of-day account reporting.
 *
 * Purpose:
 * - Provide detailed account transaction history
 * - Report current account balance
 * - Support reconciliation and cash management
 * - Enable real-time cash visibility
 *
 * Report Types:
 * - Intraday: Real-time transaction updates during business day
 * - Daily: End-of-day summary and detail
 * - Custom: Adhoc reports for specific time periods
 *
 * Key Difference from camt.053:
 * - camt.052: Account REPORT (can be intraday, interim)
 * - camt.053: Account STATEMENT (official, typically end-of-day/month)
 * - camt.054: Account NOTIFICATION (real-time individual transaction alerts)
 *
 * Use Cases:
 * - Treasury cash positioning
 * - Real-time balance monitoring
 * - Intraday liquidity management
 * - Payment reconciliation
 * - Fraud detection and monitoring
 */
@Value
@Builder
public class Camt052 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Account reports.
     */
    List<AccountReport> report;

    /**
     * Account report.
     */
    @Value
    @Builder
    public static class AccountReport {

        /**
         * Report identification.
         */
        String reportId;

        /**
         * Report creation date/time.
         */
        ZonedDateTime creationDateTime;

        /**
         * From date/time (reporting period start).
         */
        ZonedDateTime fromDateTime;

        /**
         * To date/time (reporting period end).
         */
        ZonedDateTime toDateTime;

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
         * - ITBD: Interim booked balance (intraday)
         * - OPAV: Opening available balance
         * - CLAV: Closing available balance
         * - ITAV: Interim available balance
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
         * Status (BOOK, PDNG, INFO).
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
         * Entry details (transaction details).
         */
        List<TransactionDetails> entryDetails;
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
         * Sub-family code (e.g., ESCT for SEPA credit transfer).
         */
        String subFamilyCode;

        /**
         * Proprietary code.
         */
        String proprietaryCode;
    }

    /**
     * Transaction details.
     */
    @Value
    @Builder
    public static class TransactionDetails {

        /**
         * References (instruction ID, end-to-end ID, etc.).
         */
        TransactionReferences references;

        /**
         * Amount details.
         */
        Amount amount;

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
         * Return information (if this is a return).
         */
        ReturnInformation returnInformation;
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
    }
}
