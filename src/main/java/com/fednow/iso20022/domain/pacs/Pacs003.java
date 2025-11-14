package com.fednow.iso20022.domain.pacs;

import com.fednow.iso20022.domain.common.*;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Pacs003 - FI To FI Customer Direct Debit
 *
 * ISO 20022 message for interbank direct debit transfers.
 * Used when a creditor's bank sends a direct debit request to the debtor's bank
 * to collect funds from the debtor's account.
 *
 * Direct Debit Flow:
 * 1. Creditor initiates collection (pain.008)
 * 2. Creditor's bank converts to pacs.003
 * 3. FedNow routes to debtor's bank
 * 4. Debtor's bank debits account and responds with pacs.002
 *
 * Key Difference from pacs.008 (Credit Transfer):
 * - pacs.008: Debtor pushes funds (debit their account, credit beneficiary)
 * - pacs.003: Creditor pulls funds (with pre-authorization/mandate)
 *
 * Common Use Cases:
 * - Recurring bill payments
 * - Insurance premium collections
 * - Loan repayments
 * - Subscription services
 */
@Value
@Builder
public class Pacs003 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Direct debit transaction information.
     */
    List<DirectDebitTransactionInformation> directDebitTransactionInformation;

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
         * Interbank settlement amount.
         */
        Amount interbankSettlementAmount;

        /**
         * Interbank settlement date.
         */
        String interbankSettlementDate;

        /**
         * Instructed amount (amount to be collected).
         */
        Amount instructedAmount;

        /**
         * Charge bearer.
         */
        String chargeBearerCode;

        /**
         * Direct debit transaction.
         */
        DirectDebitTransaction directDebitTransaction;

        /**
         * Creditor agent (collecting bank).
         */
        AgentIdentification creditorAgent;

        /**
         * Creditor (party collecting payment).
         */
        PartyIdentification creditor;

        /**
         * Creditor account.
         */
        AccountIdentification creditorAccount;

        /**
         * Ultimate creditor.
         */
        PartyIdentification ultimateCreditor;

        /**
         * Instructing agent.
         */
        AgentIdentification instructingAgent;

        /**
         * Instructed agent.
         */
        AgentIdentification instructedAgent;

        /**
         * Debtor agent (debtor's bank).
         */
        AgentIdentification debtorAgent;

        /**
         * Debtor (party being debited).
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

        /**
         * Settlement information.
         */
        SettlementInformation settlementInformation;

        /**
         * Payment type information.
         */
        PaymentTypeInformation paymentTypeInformation;
    }

    /**
     * Direct debit transaction.
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
         * Mandate identification.
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
         * Original mandate ID (if amended).
         */
        String originalMandateId;

        /**
         * Original creditor scheme ID.
         */
        PartyIdentification originalCreditorSchemeId;
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
        String clearingChannel;
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
