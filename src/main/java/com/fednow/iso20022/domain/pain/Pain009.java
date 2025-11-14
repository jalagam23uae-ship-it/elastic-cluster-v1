package com.fednow.iso20022.domain.pain;

import com.fednow.iso20022.domain.common.*;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Pain009 - Mandate Initiation Request
 *
 * ISO 20022 message for creating or amending direct debit mandates.
 * Used when a debtor authorizes a creditor to collect payments from their account.
 *
 * Mandate Flow:
 * 1. Debtor signs authorization for creditor to debit account
 * 2. Creditor submits pain.009 to establish mandate
 * 3. Bank validates and stores mandate
 * 4. Future direct debits reference this mandate
 *
 * Mandate Types:
 * - One-off (OOFF): Single payment authorization
 * - Recurring (RCUR): Multiple payments authorization
 * - First (FRST): First payment in a recurring series
 * - Final (FNAL): Last payment in a recurring series
 *
 * Use Cases:
 * - Subscription service authorization
 * - Recurring bill payment setup
 * - Insurance premium auto-pay
 * - Loan repayment authorization
 */
@Value
@Builder
public class Pain009 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Mandate information.
     */
    List<MandateInformation> mandate;

    /**
     * Mandate information.
     */
    @Value
    @Builder
    public static class MandateInformation {

        /**
         * Mandate request identification.
         */
        String mandateRequestId;

        /**
         * Mandate identification.
         */
        String mandateId;

        /**
         * Mandate request type (new mandate, amendment, cancellation).
         * - MNDT: New mandate
         * - AMND: Amendment
         * - CANC: Cancellation
         */
        String mandateRequestType;

        /**
         * Creditor scheme identification.
         */
        PartyIdentification creditorSchemeIdentification;

        /**
         * Mandate details.
         */
        MandateDetails mandateDetails;
    }

    /**
     * Mandate details.
     */
    @Value
    @Builder
    public static class MandateDetails {

        /**
         * Mandate identification.
         */
        String mandateId;

        /**
         * Date of signature.
         */
        String dateOfSignature;

        /**
         * Sequence type.
         * - FRST: First collection
         * - RCUR: Recurring collection
         * - FNAL: Final collection
         * - OOFF: One-off collection
         */
        String sequenceType;

        /**
         * Frequency (for recurring mandates).
         */
        Frequency frequency;

        /**
         * First collection date.
         */
        String firstCollectionDate;

        /**
         * Final collection date.
         */
        String finalCollectionDate;

        /**
         * Maximum amount (optional limit).
         */
        Amount maximumAmount;

        /**
         * Creditor.
         */
        PartyIdentification creditor;

        /**
         * Creditor account.
         */
        AccountIdentification creditorAccount;

        /**
         * Creditor agent.
         */
        AgentIdentification creditorAgent;

        /**
         * Debtor.
         */
        PartyIdentification debtor;

        /**
         * Debtor account.
         */
        AccountIdentification debtorAccount;

        /**
         * Debtor agent.
         */
        AgentIdentification debtorAgent;

        /**
         * Electronic signature (if applicable).
         */
        String electronicSignature;

        /**
         * Amendment details (if this is an amendment).
         */
        AmendmentDetails amendmentDetails;
    }

    /**
     * Frequency (for recurring mandates).
     */
    @Value
    @Builder
    public static class Frequency {
        /**
         * Frequency code.
         * - DAIL: Daily
         * - WEEK: Weekly
         * - MNTH: Monthly
         * - QUTR: Quarterly
         * - YEAR: Annually
         */
        String code;

        /**
         * Point in time (e.g., day of month: 15).
         */
        String pointInTime;
    }

    /**
     * Amendment details.
     */
    @Value
    @Builder
    public static class AmendmentDetails {

        /**
         * Original mandate identification.
         */
        String originalMandateId;

        /**
         * Original creditor scheme ID.
         */
        PartyIdentification originalCreditorSchemeId;

        /**
         * Amendment reason.
         */
        AmendmentReason amendmentReason;
    }

    /**
     * Amendment reason.
     */
    @Value
    @Builder
    public static class AmendmentReason {
        /**
         * Reason code.
         * - MD01: Mandate details changed
         * - MD02: Amount limit changed
         * - MD06: Debtor account changed
         * - MD07: Creditor account changed
         */
        String code;

        /**
         * Additional information.
         */
        String additionalInformation;
    }
}
