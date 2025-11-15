package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.common.StatusReason;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pacs.Pacs007;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Pacs007ToPacs002Converter - Reversal Acknowledgment
 *
 * Converts pacs.007 (FI to FI Payment Reversal) to pacs.002 (Payment Status Report).
 *
 * This converter generates acknowledgment for payment reversals sent by the debtor agent.
 * The creditor agent responds with pacs.002 to confirm the reversal was received and processed.
 *
 * Key Difference from pacs.004 acknowledgment:
 * - pacs.007 → pacs.002: Reversal acknowledgment (debtor agent initiated)
 * - pacs.004 → pacs.002: Return acknowledgment (creditor agent initiated)
 *
 * Message Flow:
 * Debtor Agent → pacs.007 (reversal) → FedNow → Creditor Agent
 * Creditor Agent → pacs.002 (acknowledgment) → FedNow → Debtor Agent
 *
 * Status Codes:
 * - ACCP: Reversal accepted - funds will be reversed
 * - ACSC: Reversal accepted, settled, and debited from creditor
 * - RJCT: Reversal rejected (e.g., already settled, timing window expired)
 * - PDNG: Reversal pending (awaiting manual review)
 *
 * FedNow Critical Requirements:
 * - Reversals must be initiated within 15 seconds of original payment
 * - Must respond to pacs.007 within 5 seconds
 * - UETR must be preserved for tracking
 * - Reversal acceptance depends on settlement status
 *
 * Common Rejection Reasons for Reversals:
 * - LEGL: Legal decision (payment already settled, reversal window expired)
 * - NOAS: No answer from beneficiary (creditor account not found)
 * - NOOR: No original transaction reference found
 * - TM01: Cut-off time (reversal received after settlement)
 * - FF01: Invalid file format or reference data
 */
@Slf4j
@Component
public class Pacs007ToPacs002Converter extends AbstractMessageConverter<Pacs007, Pacs002> {

    public Pacs007ToPacs002Converter() {
        super(Pacs007.class, Pacs002.class, "Pacs007ToPacs002Converter");
    }

    @Override
    protected Mono<Pacs002> doConvert(Pacs007 source, ConverterContext context) {
        logStep("Starting pacs.007 → pacs.002 conversion (reversal acknowledgment)");

        // Generate message ID for pacs.002
        enrichContextWithIds(context, "REV-ACK", false, false);

        return Mono.fromCallable(() -> {
            // Determine reversal status based on validation
            String reversalStatus = determineReversalStatus(context);
            logStep("Determined reversal status: " + reversalStatus);

            // Build pacs.002
            Pacs002 pacs002 = Pacs002.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .transactionInformationAndStatus(
                            buildTransactionStatuses(source, context, reversalStatus))
                    .build();

            logStep("Successfully created pacs.002 acknowledgment for reversal with status: " + reversalStatus);
            return pacs002;
        });
    }

    /**
     * Determines the reversal acknowledgment status.
     *
     * @param context converter context
     * @return ACCP, ACSC, RJCT, or PDNG
     */
    private String determineReversalStatus(ConverterContext context) {
        // Check if reversal validation passed
        Boolean reversalValid = context.getAttribute("reversalValid", Boolean.class);
        if (reversalValid != null && !reversalValid) {
            return "RJCT";
        }

        // Check if reversal timing is valid (within 15-second FedNow window)
        Boolean reversalTimingValid = context.getAttribute("reversalTimingValid", Boolean.class);
        if (reversalTimingValid != null && !reversalTimingValid) {
            return "RJCT"; // Reversal window expired
        }

        // Check if payment was already settled
        Boolean alreadySettled = context.getAttribute("alreadySettled", Boolean.class);
        if (alreadySettled != null && alreadySettled) {
            return "RJCT"; // Cannot reverse settled payment
        }

        // Check if reversal was immediately processed
        Boolean reversalSettled = context.getAttribute("reversalSettled", Boolean.class);
        if (reversalSettled != null && reversalSettled) {
            return "ACSC"; // Accepted, settled, and debited from creditor
        }

        // Check if reversal requires manual review
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
     * @param source pacs.007
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(Pacs007 source, ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                // Reverse the agents: creditor agent responds to debtor agent's reversal
                .instructingAgent(source.getGroupHeader().getInstructedAgent())
                .instructedAgent(source.getGroupHeader().getInstructingAgent())
                .build();
    }

    /**
     * Builds transaction status entries for each reversal in pacs.007.
     *
     * @param source pacs.007
     * @param context converter context
     * @param reversalStatus reversal acknowledgment status
     * @return list of transaction statuses
     */
    private List<Pacs002.TransactionInformationAndStatus> buildTransactionStatuses(
            Pacs007 source, ConverterContext context, String reversalStatus) {

        List<Pacs002.TransactionInformationAndStatus> statuses = new ArrayList<>();

        if (source.getReversalTransaction() != null) {
            for (Pacs007.ReversalTransactionInformation txn : source.getReversalTransaction()) {
                Pacs002.TransactionInformationAndStatus status =
                        buildTransactionStatus(txn, source, context, reversalStatus);
                statuses.add(status);
            }
        }

        return statuses;
    }

    /**
     * Builds a single transaction status entry.
     *
     * @param txn reversal transaction from pacs.007
     * @param source pacs.007
     * @param context converter context
     * @param reversalStatus reversal acknowledgment status
     * @return transaction status
     */
    private Pacs002.TransactionInformationAndStatus buildTransactionStatus(
            Pacs007.ReversalTransactionInformation txn,
            Pacs007 source,
            ConverterContext context,
            String reversalStatus) {

        return Pacs002.TransactionInformationAndStatus.builder()
                .statusId(idGenerator.generateStatusId())
                // Original reversal message information
                .originalGroupInformation(Pacs002.OriginalGroupInformation.builder()
                        .originalMessageId(source.getOriginalGroupInformation() != null
                                ? source.getOriginalGroupInformation().getOriginalMessageId()
                                : null)
                        .originalMessageNameIdentification("pacs.007.001.11")
                        .originalCreationDateTime(source.getOriginalGroupInformation() != null
                                ? source.getOriginalGroupInformation().getOriginalCreationDateTime()
                                : context.getCurrentTimestamp())
                        .build())
                // Preserve reversal IDs for tracking
                .originalInstructionId(txn.getReversalId())
                .originalEndToEndId(txn.getOriginalEndToEndId())
                .originalTransactionId(txn.getOriginalTransactionId())
                .originalUetr(txn.getOriginalUetr())
                // Reversal acknowledgment status
                .transactionStatus(reversalStatus)
                // Status reasons (for rejections or pending)
                .statusReasonInformation(buildStatusReasons(context, reversalStatus, txn))
                // Acceptance datetime (only for ACCP/ACSC)
                .acceptanceDateTime(
                        "ACCP".equals(reversalStatus) || "ACSC".equals(reversalStatus)
                                ? context.getCurrentTimestamp()
                                : null)
                // Clearing system reference (FedNow reference)
                .clearingSystemReference(
                        "ACCP".equals(reversalStatus) || "ACSC".equals(reversalStatus)
                                ? idGenerator.generateClearingSystemReference()
                                : null)
                // Account servicer reference (bank's internal reference)
                .accountServicingReference(idGenerator.generateAccountServicerReference())
                // Original transaction reference (from the reversal)
                .originalTransactionReference(buildOriginalTransactionReference(txn))
                .build();
    }

    /**
     * Builds status reason information.
     *
     * @param context converter context
     * @param reversalStatus reversal acknowledgment status
     * @param txn reversal transaction
     * @return list of status reasons
     */
    private List<Pacs002.StatusReasonInformation> buildStatusReasons(
            ConverterContext context,
            String reversalStatus,
            Pacs007.ReversalTransactionInformation txn) {

        List<Pacs002.StatusReasonInformation> reasons = new ArrayList<>();

        switch (reversalStatus) {
            case "ACCP":
            case "ACSC":
                // No reason needed for acceptance
                break;

            case "RJCT":
                // Build rejection reasons
                String rejectionCode = context.getAttribute("rejectionCode", String.class);
                String rejectionReason = context.getAttribute("rejectionReason", String.class);

                // Determine rejection code based on context
                if (rejectionCode == null) {
                    Boolean timingInvalid = context.getAttribute("reversalTimingValid", Boolean.class);
                    Boolean settled = context.getAttribute("alreadySettled", Boolean.class);

                    if (timingInvalid != null && !timingInvalid) {
                        rejectionCode = "TM01"; // Cut-off time
                        rejectionReason = "Reversal window expired (must be within 15 seconds)";
                    } else if (settled != null && settled) {
                        rejectionCode = "LEGL"; // Legal decision
                        rejectionReason = "Payment already settled, cannot reverse";
                    } else {
                        rejectionCode = "NOOR"; // No original transaction reference
                        rejectionReason = "Reversal cannot be processed";
                    }
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
                    pendingReason = "Reversal requires manual verification";
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
     * @param txn reversal transaction from pacs.007
     * @return original transaction reference
     */
    private Pacs002.OriginalTransactionReference buildOriginalTransactionReference(
            Pacs007.ReversalTransactionInformation txn) {

        if (txn.getOriginalTransactionReference() == null) {
            return null;
        }

        Pacs007.OriginalTransactionReference ref = txn.getOriginalTransactionReference();

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
     * Helper method to create pacs.002 for accepted reversal.
     *
     * @param source pacs.007
     * @param context converter context
     * @return Mono<Pacs002>
     */
    public Mono<Pacs002> createAcceptedReversal(
            Pacs007 source,
            ConverterContext context) {
        context.setAttribute("reversalValid", true);
        context.setAttribute("reversalTimingValid", true);
        context.setAttribute("reversalSettled", false);
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.002 for settled reversal.
     *
     * @param source pacs.007
     * @param context converter context
     * @return Mono<Pacs002>
     */
    public Mono<Pacs002> createSettledReversal(
            Pacs007 source,
            ConverterContext context) {
        context.setAttribute("reversalValid", true);
        context.setAttribute("reversalTimingValid", true);
        context.setAttribute("reversalSettled", true);
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.002 for reversal rejected due to timing.
     *
     * @param source pacs.007
     * @param context converter context
     * @return Mono<Pacs002>
     */
    public Mono<Pacs002> createTimingExpiredRejection(
            Pacs007 source,
            ConverterContext context) {
        context.setAttribute("reversalValid", false);
        context.setAttribute("reversalTimingValid", false);
        context.setAttribute("rejectionCode", "TM01");
        context.setAttribute("rejectionReason", "Reversal window expired (must be within 15 seconds)");
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.002 for reversal rejected due to settlement.
     *
     * @param source pacs.007
     * @param context converter context
     * @return Mono<Pacs002>
     */
    public Mono<Pacs002> createAlreadySettledRejection(
            Pacs007 source,
            ConverterContext context) {
        context.setAttribute("reversalValid", false);
        context.setAttribute("alreadySettled", true);
        context.setAttribute("rejectionCode", "LEGL");
        context.setAttribute("rejectionReason", "Payment already settled, cannot reverse");
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.002 for pending reversal.
     *
     * @param source pacs.007
     * @param context converter context
     * @param pendingReason reason for pending status
     * @return Mono<Pacs002>
     */
    public Mono<Pacs002> createPendingReversal(
            Pacs007 source,
            ConverterContext context,
            String pendingReason) {
        context.setAttribute("reversalValid", true);
        context.setAttribute("reversalTimingValid", true);
        context.setAttribute("requiresReview", true);
        context.setAttribute("pendingReason", pendingReason);
        return convert(source, context);
    }
}
