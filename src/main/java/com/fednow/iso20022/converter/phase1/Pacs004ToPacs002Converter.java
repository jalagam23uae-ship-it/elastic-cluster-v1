package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.common.StatusReason;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pacs.Pacs004;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Pacs004ToPacs002Converter - Return Acknowledgment
 *
 * Converts pacs.004 (Payment Return) to pacs.002 (Payment Status Report).
 *
 * This converter generates acknowledgment for payment returns sent by the creditor agent.
 * The debtor agent responds with pacs.002 to confirm the return was received and processed.
 *
 * Message Flow:
 * Creditor Agent → pacs.004 (return) → FedNow → Debtor Agent
 * Debtor Agent → pacs.002 (acknowledgment) → FedNow → Creditor Agent
 *
 * Status Codes:
 * - ACCP: Return accepted - funds will be returned to debtor account
 * - ACSC: Return accepted, settled, and credited to debtor
 * - RJCT: Return rejected (e.g., invalid return reason, timing issues)
 * - PDNG: Return pending (awaiting manual review)
 *
 * FedNow Requirements:
 * - Must respond to pacs.004 within 5 seconds
 * - UETR must be preserved for tracking
 * - Rejection reasons must be valid ISO 20022 codes
 * - Return must be validated against original payment
 *
 * Common Rejection Reasons for Returns:
 * - NOAS: No answer from beneficiary (debtor agent can't locate account)
 * - LEGL: Legal decision to reject return
 * - NOOR: No original transaction reference found
 * - FF01: Invalid file format or reference data
 */
@Slf4j
@Component
public class Pacs004ToPacs002Converter extends AbstractMessageConverter<Pacs004, Pacs002> {

    public Pacs004ToPacs002Converter() {
        super(Pacs004.class, Pacs002.class, "Pacs004ToPacs002Converter");
    }

    @Override
    protected Mono<Pacs002> doConvert(Pacs004 source, ConverterContext context) {
        logStep("Starting pacs.004 → pacs.002 conversion (return acknowledgment)");

        // Generate message ID for pacs.002
        enrichContextWithIds(context, "RTN-ACK", false, false);

        return Mono.fromCallable(() -> {
            // Determine return status based on validation
            String returnStatus = determineReturnStatus(context);
            logStep("Determined return status: " + returnStatus);

            // Build pacs.002
            Pacs002 pacs002 = Pacs002.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .transactionInformationAndStatus(
                            buildTransactionStatuses(source, context, returnStatus))
                    .build();

            logStep("Successfully created pacs.002 acknowledgment for return with status: " + returnStatus);
            return pacs002;
        });
    }

    /**
     * Determines the return acknowledgment status.
     *
     * @param context converter context
     * @return ACCP, ACSC, RJCT, or PDNG
     */
    private String determineReturnStatus(ConverterContext context) {
        // Check if return validation passed
        Boolean returnValid = context.getAttribute("returnValid", Boolean.class);
        if (returnValid != null && !returnValid) {
            return "RJCT";
        }

        // Check if return was immediately settled
        Boolean returnSettled = context.getAttribute("returnSettled", Boolean.class);
        if (returnSettled != null && returnSettled) {
            return "ACSC"; // Accepted, settled, and credited
        }

        // Check if return requires manual review
        Boolean requiresReview = context.getAttribute("requiresReview", Boolean.class);
        if (requiresReview != null && requiresReview) {
            return "PDNG"; // Pending manual review
        }

        // Default to accepted
        return "ACCP";
    }

    /**
     * Builds group header for pacs.002.
     *
     * @param source pacs.004
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(Pacs004 source, ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                // Reverse the agents: debtor agent responds to creditor agent's return
                .instructingAgent(source.getGroupHeader().getInstructedAgent())
                .instructedAgent(source.getGroupHeader().getInstructingAgent())
                .build();
    }

    /**
     * Builds transaction status entries for each return in pacs.004.
     *
     * @param source pacs.004
     * @param context converter context
     * @param returnStatus return acknowledgment status
     * @return list of transaction statuses
     */
    private List<Pacs002.TransactionInformationAndStatus> buildTransactionStatuses(
            Pacs004 source, ConverterContext context, String returnStatus) {

        List<Pacs002.TransactionInformationAndStatus> statuses = new ArrayList<>();

        for (Pacs004.PaymentReturnTransactionInformation txn :
                source.getTransactionInformation()) {

            Pacs002.TransactionInformationAndStatus status =
                    buildTransactionStatus(txn, context, returnStatus);
            statuses.add(status);
        }

        return statuses;
    }

    /**
     * Builds a single transaction status entry.
     *
     * @param txn return transaction from pacs.004
     * @param context converter context
     * @param returnStatus return acknowledgment status
     * @return transaction status
     */
    private Pacs002.TransactionInformationAndStatus buildTransactionStatus(
            Pacs004.PaymentReturnTransactionInformation txn,
            ConverterContext context,
            String returnStatus) {

        return Pacs002.TransactionInformationAndStatus.builder()
                .statusId(idGenerator.generateStatusId())
                // Original return message information
                .originalGroupInformation(Pacs002.OriginalGroupInformation.builder()
                        .originalMessageId(txn.getOriginalGroupInformation() != null
                                ? txn.getOriginalGroupInformation().getOriginalMessageId()
                                : null)
                        .originalMessageNameIdentification("pacs.004.001.11")
                        .originalCreationDateTime(txn.getOriginalGroupInformation() != null
                                ? txn.getOriginalGroupInformation().getOriginalCreationDateTime()
                                : context.getCurrentTimestamp())
                        .build())
                // Preserve return IDs for tracking
                .originalInstructionId(txn.getReturnId())
                .originalEndToEndId(txn.getOriginalEndToEndId())
                .originalTransactionId(txn.getOriginalTransactionId())
                .originalUetr(txn.getOriginalUetr())
                // Return acknowledgment status
                .transactionStatus(returnStatus)
                // Status reasons (for rejections or pending)
                .statusReasonInformation(buildStatusReasons(context, returnStatus, txn))
                // Acceptance datetime (only for ACCP/ACSC)
                .acceptanceDateTime(
                        "ACCP".equals(returnStatus) || "ACSC".equals(returnStatus)
                                ? context.getCurrentTimestamp()
                                : null)
                // Clearing system reference (FedNow reference)
                .clearingSystemReference(
                        "ACCP".equals(returnStatus) || "ACSC".equals(returnStatus)
                                ? idGenerator.generateClearingSystemReference()
                                : null)
                // Account servicer reference (bank's internal reference)
                .accountServicingReference(idGenerator.generateAccountServicerReference())
                // Original transaction reference (from the return)
                .originalTransactionReference(buildOriginalTransactionReference(txn))
                .build();
    }

    /**
     * Builds status reason information.
     *
     * @param context converter context
     * @param returnStatus return acknowledgment status
     * @param txn return transaction
     * @return list of status reasons
     */
    private List<Pacs002.StatusReasonInformation> buildStatusReasons(
            ConverterContext context,
            String returnStatus,
            Pacs004.PaymentReturnTransactionInformation txn) {

        List<Pacs002.StatusReasonInformation> reasons = new ArrayList<>();

        switch (returnStatus) {
            case "ACCP":
            case "ACSC":
                // No reason needed for acceptance
                break;

            case "RJCT":
                // Build rejection reasons
                String rejectionCode = context.getAttribute("rejectionCode", String.class);
                String rejectionReason = context.getAttribute("rejectionReason", String.class);

                if (rejectionCode == null) {
                    rejectionCode = "NOOR"; // No original transaction reference
                }
                if (rejectionReason == null) {
                    rejectionReason = "Return cannot be processed";
                }

                reasons.add(Pacs002.StatusReasonInformation.builder()
                        .reason(StatusReason.builder()
                                .reasonCode(rejectionCode)
                                .additionalInformation(List.of(rejectionReason))
                                .build())
                        .build());
                break;

            case "PDNG":
                // Build pending reason
                String pendingReason = context.getAttribute("pendingReason", String.class);
                if (pendingReason == null) {
                    pendingReason = "Return requires manual verification";
                }

                reasons.add(Pacs002.StatusReasonInformation.builder()
                        .reason(StatusReason.builder()
                                .reasonCode("MS03") // Not specified - pending
                                .additionalInformation(List.of(pendingReason))
                                .build())
                        .build());
                break;
        }

        return reasons;
    }

    /**
     * Builds original transaction reference.
     *
     * @param txn return transaction from pacs.004
     * @return original transaction reference
     */
    private Pacs002.OriginalTransactionReference buildOriginalTransactionReference(
            Pacs004.PaymentReturnTransactionInformation txn) {

        if (txn.getOriginalTransactionReference() == null) {
            return null;
        }

        Pacs004.OriginalTransactionReference ref = txn.getOriginalTransactionReference();

        return Pacs002.OriginalTransactionReference.builder()
                .interbankSettlementAmount(ref.getInterbankSettlementAmount())
                .interbankSettlementDate(ref.getInterbankSettlementDate())
                .debtor(ref.getDebtor())
                .debtorAccount(ref.getDebtorAccount())
                .debtorAgent(ref.getDebtorAgent())
                .creditor(ref.getCreditor())
                .creditorAccount(ref.getCreditorAccount())
                .creditorAgent(ref.getCreditorAgent())
                .remittanceInformation(ref.getRemittanceInformation())
                .build();
    }

    /**
     * Helper method to create pacs.002 for accepted return.
     *
     * @param source pacs.004
     * @param context converter context
     * @return Mono<Pacs002>
     */
    public Mono<Pacs002> createAcceptedReturn(
            Pacs004 source,
            ConverterContext context) {
        context.setAttribute("returnValid", true);
        context.setAttribute("returnSettled", false);
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.002 for settled return.
     *
     * @param source pacs.004
     * @param context converter context
     * @return Mono<Pacs002>
     */
    public Mono<Pacs002> createSettledReturn(
            Pacs004 source,
            ConverterContext context) {
        context.setAttribute("returnValid", true);
        context.setAttribute("returnSettled", true);
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.002 for rejected return.
     *
     * @param source pacs.004
     * @param context converter context
     * @param reasonCode rejection reason code
     * @param reason rejection reason
     * @return Mono<Pacs002>
     */
    public Mono<Pacs002> createRejectedReturn(
            Pacs004 source,
            ConverterContext context,
            String reasonCode,
            String reason) {
        context.setAttribute("returnValid", false);
        context.setAttribute("rejectionCode", reasonCode);
        context.setAttribute("rejectionReason", reason);
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.002 for pending return.
     *
     * @param source pacs.004
     * @param context converter context
     * @param pendingReason reason for pending status
     * @return Mono<Pacs002>
     */
    public Mono<Pacs002> createPendingReturn(
            Pacs004 source,
            ConverterContext context,
            String pendingReason) {
        context.setAttribute("returnValid", true);
        context.setAttribute("requiresReview", true);
        context.setAttribute("pendingReason", pendingReason);
        return convert(source, context);
    }
}
