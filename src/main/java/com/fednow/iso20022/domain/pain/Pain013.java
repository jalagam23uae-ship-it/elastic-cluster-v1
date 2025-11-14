package com.fednow.iso20022.domain.pain;

import com.fednow.iso20022.domain.common.*;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Pain013 - Creditor Payment Activation Request
 *
 * ISO 20022 message for requesting activation or deactivation of a standing order
 * or recurring payment instruction.
 *
 * Purpose:
 * - Activate previously suspended payment instructions
 * - Deactivate (suspend) active payment instructions
 * - Modify activation status without cancelling the instruction
 *
 * Common Scenarios:
 * - Temporarily pause recurring payments (vacation, cash flow management)
 * - Reactivate paused subscriptions
 * - Seasonal payment adjustments
 * - Credit line management
 *
 * Difference from pain.009:
 * - pain.009: Creates/amends/cancels mandates permanently
 * - pain.013: Temporarily activates/deactivates existing instructions
 */
@Value
@Builder
public class Pain013 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Creditor payment activation request.
     */
    List<CreditorPaymentActivationRequest> creditorPaymentActivationRequest;

    /**
     * Creditor payment activation request.
     */
    @Value
    @Builder
    public static class CreditorPaymentActivationRequest {

        /**
         * Payment information identification.
         */
        String paymentInformationId;

        /**
         * Activation request identification.
         */
        String activationRequestId;

        /**
         * Requested action.
         * - ACTV: Activate
         * - CANC: Deactivate (suspend)
         */
        String requestedAction;

        /**
         * Account identification being activated/deactivated.
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
         * Original payment instruction reference.
         */
        OriginalPaymentInstruction originalPaymentInstruction;

        /**
         * Activation details.
         */
        ActivationDetails activationDetails;
    }

    /**
     * Original payment instruction reference.
     */
    @Value
    @Builder
    public static class OriginalPaymentInstruction {

        /**
         * Original message ID.
         */
        String originalMessageId;

        /**
         * Original message name ID.
         */
        String originalMessageNameId;

        /**
         * Original payment information ID.
         */
        String originalPaymentInformationId;

        /**
         * Original instruction ID.
         */
        String originalInstructionId;

        /**
         * Original end-to-end ID.
         */
        String originalEndToEndId;
    }

    /**
     * Activation details.
     */
    @Value
    @Builder
    public static class ActivationDetails {

        /**
         * Effective date for activation/deactivation.
         */
        String effectiveDate;

        /**
         * Reactivation date (if temporarily suspending).
         */
        String reactivationDate;

        /**
         * Reason for activation/deactivation.
         */
        ActivationReason reason;

        /**
         * Additional information.
         */
        List<String> additionalInformation;
    }

    /**
     * Activation reason.
     */
    @Value
    @Builder
    public static class ActivationReason {
        /**
         * Reason code.
         * - CUST: Customer request
         * - CASH: Cash flow management
         * - SEAS: Seasonal adjustment
         * - SUSP: Suspicious activity (fraud prevention)
         * - TECH: Technical issue
         */
        String code;

        /**
         * Additional reason details.
         */
        String additionalInformation;
    }
}
