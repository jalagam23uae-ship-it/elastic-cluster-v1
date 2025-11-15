package com.fednow.iso20022.domain.camt;

import com.fednow.iso20022.domain.common.*;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Camt029 - Resolution Of Investigation
 *
 * ISO 20022 message providing the outcome of a payment investigation.
 * Used when a bank completes investigation of a payment issue and needs to
 * inform the requesting party of the results.
 *
 * Purpose:
 * - Communicate investigation results
 * - Provide resolution details
 * - Confirm corrective actions taken
 * - Close investigation case
 *
 * Investigation Triggers:
 * - Missing payment (camt.028 query)
 * - Payment discrepancy
 * - Customer complaint
 * - Regulatory inquiry
 * - Fraud investigation
 *
 * Resolution Outcomes:
 * - Payment found and processed
 * - Payment cancelled/returned
 * - Investigation ongoing (partial result)
 * - No resolution possible
 *
 * Message Flow:
 * Customer/Bank → Investigation request → Bank investigates → camt.029 (results) → Requestor
 */
@Value
@Builder
public class Camt029 {

    /**
     * Group header containing message identification and creation timestamp.
     */
    GroupHeader groupHeader;

    /**
     * Case assignment (investigation case details).
     */
    CaseAssignment caseAssignment;

    /**
     * Resolution information.
     */
    ResolutionOfInvestigation resolutionOfInvestigation;

    /**
     * Case assignment.
     */
    @Value
    @Builder
    public static class CaseAssignment {

        /**
         * Investigation case ID.
         */
        String caseId;

        /**
         * Party who created the investigation case.
         */
        PartyIdentification creator;

        /**
         * Creation date/time of the investigation.
         */
        ZonedDateTime creationDateTime;
    }

    /**
     * Resolution of investigation.
     */
    @Value
    @Builder
    public static class ResolutionOfInvestigation {

        /**
         * Investigation status.
         * - RSLV: Resolved
         * - PNDG: Pending (ongoing)
         * - CNCL: Cancelled
         * - NRES: No resolution possible
         */
        String investigationStatus;

        /**
         * Investigated case details.
         */
        InvestigatedCase investigatedCase;

        /**
         * Resolution details.
         */
        List<InvestigationResult> investigationResult;

        /**
         * Charges applied (if any).
         */
        List<Charge> charges;
    }

    /**
     * Investigated case.
     */
    @Value
    @Builder
    public static class InvestigatedCase {

        /**
         * Original case ID being investigated.
         */
        String originalCaseId;

        /**
         * Original message ID being investigated.
         */
        String originalMessageId;

        /**
         * Original message name identification.
         */
        String originalMessageNameIdentification;

        /**
         * Original creation date/time.
         */
        ZonedDateTime originalCreationDateTime;

        /**
         * Original instruction ID.
         */
        String originalInstructionId;

        /**
         * Original end-to-end ID.
         */
        String originalEndToEndId;

        /**
         * Original transaction ID.
         */
        String originalTransactionId;

        /**
         * Original UETR.
         */
        String originalUetr;
    }

    /**
     * Investigation result.
     */
    @Value
    @Builder
    public static class InvestigationResult {

        /**
         * Investigation execution confirmation.
         */
        InvestigationExecutionConfirmation investigationExecutionConfirmation;

        /**
         * Corrective action taken.
         */
        CorrectiveAction correctiveAction;

        /**
         * Rejection reason (if not resolved).
         */
        RejectionReason rejectionReason;
    }

    /**
     * Investigation execution confirmation.
     */
    @Value
    @Builder
    public static class InvestigationExecutionConfirmation {

        /**
         * Confirmation code.
         * - CNCL: Payment cancelled
         * - MODI: Payment modified
         * - ACPT: Payment accepted/processed
         * - PDNG: Still pending
         * - RJCT: Payment rejected
         */
        String confirmationCode;

        /**
         * Original payment information (if found).
         */
        OriginalPaymentInformation originalPaymentInformation;

        /**
         * Modified payment information (if modified).
         */
        ModifiedPaymentInformation modifiedPaymentInformation;

        /**
         * Cancellation details (if cancelled).
         */
        CancellationDetails cancellationDetails;
    }

    /**
     * Original payment information.
     */
    @Value
    @Builder
    public static class OriginalPaymentInformation {

        /**
         * Original amount.
         */
        Amount originalAmount;

        /**
         * Original settlement date.
         */
        String originalSettlementDate;

        /**
         * Original debtor.
         */
        PartyIdentification originalDebtor;

        /**
         * Original debtor account.
         */
        AccountIdentification originalDebtorAccount;

        /**
         * Original creditor.
         */
        PartyIdentification originalCreditor;

        /**
         * Original creditor account.
         */
        AccountIdentification originalCreditorAccount;

        /**
         * Payment status.
         * - ACCP: Accepted
         * - RJCT: Rejected
         * - PDNG: Pending
         */
        String paymentStatus;

        /**
         * Status reason.
         */
        String statusReasonCode;
    }

    /**
     * Modified payment information.
     */
    @Value
    @Builder
    public static class ModifiedPaymentInformation {

        /**
         * New amount (if modified).
         */
        Amount newAmount;

        /**
         * New settlement date (if modified).
         */
        String newSettlementDate;

        /**
         * Modification reason.
         */
        String modificationReason;
    }

    /**
     * Cancellation details.
     */
    @Value
    @Builder
    public static class CancellationDetails {

        /**
         * Cancellation reason code.
         */
        String cancellationReasonCode;

        /**
         * Additional cancellation information.
         */
        List<String> additionalCancellationInformation;

        /**
         * Cancellation timestamp.
         */
        ZonedDateTime cancellationDateTime;
    }

    /**
     * Corrective action taken.
     */
    @Value
    @Builder
    public static class CorrectiveAction {

        /**
         * Action code.
         * - RETR: Payment returned
         * - CORR: Correction applied
         * - REPR: Payment reprocessed
         * - CRDT: Credit applied
         */
        String actionCode;

        /**
         * Action description.
         */
        String actionDescription;

        /**
         * Additional action information.
         */
        List<String> additionalActionInformation;
    }

    /**
     * Rejection reason (if investigation couldn't resolve).
     */
    @Value
    @Builder
    public static class RejectionReason {

        /**
         * Reason code.
         * - NFND: Payment not found
         * - NPAY: No payment made
         * - TIMO: Timeout (investigation took too long)
         * - CUST: Customer withdrew request
         */
        String reasonCode;

        /**
         * Additional reason information.
         */
        List<String> additionalReasonInformation;
    }

    /**
     * Charge information.
     */
    @Value
    @Builder
    public static class Charge {

        /**
         * Charge type (e.g., investigation fee).
         */
        String chargeType;

        /**
         * Charge amount.
         */
        Amount chargeAmount;

        /**
         * Charge bearer (who pays).
         */
        String chargeBearer;
    }
}
