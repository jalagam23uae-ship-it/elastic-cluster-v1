package com.fednow.iso20022.domain.pain;

import com.fednow.iso20022.domain.common.*;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Pain008 - Customer Direct Debit Initiation
 *
 * ISO 20022 message for initiating direct debit payments.
 * Used when a creditor (merchant, biller) wants to pull funds from
 * a customer's (debtor's) account.
 *
 * Direct Debit Flow:
 * 1. Customer authorizes creditor to debit their account (mandate)
 * 2. Creditor sends pain.008 to their bank
 * 3. Bank converts to pacs.003 (FI to FI Direct Debit)
 * 4. FedNow routes to debtor's bank
 * 5. Debtor's bank debits customer account
 *
 * Common Use Cases:
 * - Recurring bill payments (utilities, subscriptions)
 * - One-time authorized debits
 * - Insurance premium collections
 * - Loan repayments
 *
 * Key Difference from Credit Transfer (pain.001):
 * - pain.001: Debtor pushes funds to creditor
 * - pain.008: Creditor pulls funds from debtor (with authorization)
 */
@Value
@Builder
public class Pain008 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Payment information containing direct debit details.
     */
    List<PaymentInformation> paymentInformation;

    /**
     * Payment information.
     */
    @Value
    @Builder
    public static class PaymentInformation {

        /**
         * Payment information ID.
         */
        String paymentInformationId;

        /**
         * Payment method (always DD for direct debit).
         */
        String paymentMethod;

        /**
         * Batch booking indicator.
         */
        Boolean batchBooking;

        /**
         * Number of transactions.
         */
        Integer numberOfTransactions;

        /**
         * Control sum.
         */
        Amount controlSum;

        /**
         * Payment type information.
         */
        PaymentTypeInformation paymentTypeInformation;

        /**
         * Requested collection date.
         */
        String requestedCollectionDate;

        /**
         * Creditor (the party collecting the payment).
         */
        PartyIdentification creditor;

        /**
         * Creditor account.
         */
        AccountIdentification creditorAccount;

        /**
         * Creditor agent (creditor's bank).
         */
        AgentIdentification creditorAgent;

        /**
         * Direct debit transaction information.
         */
        List<DirectDebitTransactionInformation> directDebitTransactionInformation;
    }

    /**
     * Payment type information.
     */
    @Value
    @Builder
    public static class PaymentTypeInformation {
        String instructionPriority;
        String serviceLevel;
        String localInstrument;
        String sequenceType; // FRST, RCUR, FNAL, OOFF
        String categoryPurpose;
    }

    /**
     * Direct debit transaction information.
     */
    @Value
    @Builder
    public static class DirectDebitTransactionInformation {

        /**
         * Payment identification.
         */
        PaymentIdentification paymentIdentification;

        /**
         * Instructed amount.
         */
        Amount instructedAmount;

        /**
         * Direct debit transaction details.
         */
        DirectDebitTransaction directDebitTransaction;

        /**
         * Ultimate creditor.
         */
        PartyIdentification ultimateCreditor;

        /**
         * Debtor agent (debtor's bank).
         */
        AgentIdentification debtorAgent;

        /**
         * Debtor (the party being debited).
         */
        PartyIdentification debtor;

        /**
         * Debtor account.
         */
        AccountIdentification debtorAccount;

        /**
         * Ultimate debtor.
         */
        PartyIdentification ultimateDebtor;

        /**
         * Purpose.
         */
        Purpose purpose;

        /**
         * Remittance information.
         */
        RemittanceInformation remittanceInformation;
    }

    /**
     * Direct debit transaction details.
     */
    @Value
    @Builder
    public static class DirectDebitTransaction {

        /**
         * Mandate related information.
         */
        MandateRelatedInformation mandateRelatedInformation;

        /**
         * Creditor scheme identification.
         */
        PartyIdentification creditorSchemeIdentification;
    }

    /**
     * Mandate related information.
     */
    @Value
    @Builder
    public static class MandateRelatedInformation {

        /**
         * Mandate identification (unique reference).
         */
        String mandateId;

        /**
         * Date of signature.
         */
        String dateOfSignature;

        /**
         * Amendment indicator.
         */
        Boolean amendmentIndicator;

        /**
         * Original mandate identification (if amended).
         */
        String originalMandateId;

        /**
         * Original creditor scheme ID (if amended).
         */
        PartyIdentification originalCreditorSchemeId;
    }

    /**
     * Purpose.
     */
    @Value
    @Builder
    public static class Purpose {
        String code;
        String proprietary;
    }
}
