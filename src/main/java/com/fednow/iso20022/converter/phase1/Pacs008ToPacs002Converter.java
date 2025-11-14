package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.common.StatusReason;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pacs.Pacs008;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pacs008ToPacs002Converter - MANDATORY CRITICAL CONVERTER
 *
 * Converts pacs.008 (FI to FI Customer Credit Transfer) to pacs.002 (Payment Status Report).
 *
 * This is the business-level response to an incoming payment:
 * - ACCP: Payment accepted and will settle
 * - RJCT: Payment rejected (with reason codes)
 * - PDNG: Payment pending manual review
 *
 * CRITICAL: Every pacs.008 MUST receive a pacs.002 response within 5 seconds.
 *
 * Decision Flow:
 * 1. Technical validation passed (otherwise admi.002 would be sent)
 * 2. Business validation (accounts, OFAC, fraud, limits)
 * 3. Generate ACCP, RJCT, or PDNG based on validation results
 *
 * Field Reversals:
 * - pacs.008 InstgAgt (sender) → pacs.002 InstdAgt (responding to)
 * - pacs.008 InstdAgt (receiver) → pacs.002 InstgAgt (responding from)
 */
@Slf4j
@Component
public class Pacs008ToPacs002Converter extends AbstractMessageConverter<Pacs008, Pacs002> {

    public Pacs008ToPacs002Converter() {
        super(Pacs008.class, Pacs002.class, "Pacs008ToPacs002Converter");
    }

    @Override
    protected Mono<Pacs002> doConvert(Pacs008 source, ConverterContext context) {
        logStep("Starting pacs.008 → pacs.002 conversion");

        // Generate message ID for pacs.002
        enrichContextWithIds(context, "STS", false, false);

        return Mono.fromCallable(() -> {
            // Determine payment status based on validation results
            String paymentStatus = determinePaymentStatus(context);
            logStep("Determined payment status: " + paymentStatus);

            // Build pacs.002
            Pacs002 pacs002 = Pacs002.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .transactionInformationAndStatus(
                            buildTransactionStatuses(source, context, paymentStatus))
                    .build();

            logStep("Successfully created pacs.002 with status: " + paymentStatus);
            return pacs002;
        });
    }

    /**
     * Determines the payment status based on validation results.
     *
     * @param context converter context with validation results
     * @return ACCP, RJCT, or PDNG
     */
    private String determinePaymentStatus(ConverterContext context) {
        // If no validation results, default to ACCP (for testing)
        if (context.getValidationResults() == null) {
            return "ACCP";
        }

        ConverterContext.ValidationResults validation = context.getValidationResults();

        // Critical errors → RJCT
        if (context.hasCriticalErrors()) {
            return "RJCT";
        }

        // OFAC failure → RJCT (AG01)
        if (!context.passedOFAC()) {
            return "RJCT";
        }

        // High fraud score (>80) → RJCT
        if (context.isFraudScoreAbove(80)) {
            return "RJCT";
        }

        // Medium fraud score (60-80) → PDNG (manual review)
        if (context.isFraudScoreAbove(60)) {
            return "PDNG";
        }

        // Warnings but no errors → ACCP (accept with warnings)
        if (validation.isHasWarnings() && validation.isValid()) {
            return "ACCP";
        }

        // All checks passed → ACCP
        return validation.isValid() ? "ACCP" : "RJCT";
    }

    /**
     * Builds the group header for pacs.002.
     *
     * @param source original pacs.008
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(Pacs008 source, ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                // Reverse the agents: responder becomes instructing agent
                .instructingAgent(source.getGroupHeader().getInstructedAgent())
                .instructedAgent(source.getGroupHeader().getInstructingAgent())
                .build();
    }

    /**
     * Builds transaction status entries for each transaction in pacs.008.
     *
     * @param source original pacs.008
     * @param context converter context
     * @param paymentStatus overall payment status
     * @return list of transaction statuses
     */
    private List<Pacs002.TransactionInformationAndStatus> buildTransactionStatuses(
            Pacs008 source, ConverterContext context, String paymentStatus) {

        List<Pacs002.TransactionInformationAndStatus> statuses = new ArrayList<>();

        for (Pacs008.CreditTransferTransactionInformation txn :
                source.getCreditTransferTransactionInformation()) {

            Pacs002.TransactionInformationAndStatus status =
                    buildTransactionStatus(txn, context, paymentStatus);
            statuses.add(status);
        }

        return statuses;
    }

    /**
     * Builds a single transaction status entry.
     *
     * @param txn transaction from pacs.008
     * @param context converter context
     * @param paymentStatus payment status (ACCP/RJCT/PDNG)
     * @return transaction status
     */
    private Pacs002.TransactionInformationAndStatus buildTransactionStatus(
            Pacs008.CreditTransferTransactionInformation txn,
            ConverterContext context,
            String paymentStatus) {

        return Pacs002.TransactionInformationAndStatus.builder()
                .statusId(idGenerator.generateStatusId())
                // Original message information
                .originalGroupInformation(Pacs002.OriginalGroupInformation.builder()
                        .originalMessageId(
                                txn.getPaymentIdentification().getInstructionId())
                        .originalMessageNameIdentification("pacs.008.001.11")
                        .originalCreationDateTime(context.getCurrentTimestamp())
                        .build())
                // Preserve original IDs for tracking
                .originalInstructionId(txn.getPaymentIdentification().getInstructionId())
                .originalEndToEndId(txn.getPaymentIdentification().getEndToEndId())
                .originalTransactionId(txn.getPaymentIdentification().getTransactionId())
                .originalUetr(txn.getPaymentIdentification().getUetr())
                // Transaction status
                .transactionStatus(paymentStatus)
                // Status reasons
                .statusReasonInformation(buildStatusReasons(context, paymentStatus))
                // Acceptance datetime (only for ACCP/ACSC)
                .acceptanceDateTimeacceptanceDateTimeclearingSystemReference(
                        "ACCP".equals(paymentStatus) || "ACSC".equals(paymentStatus)
                                ? context.getCurrentTimestamp()
                                : null)
                // Clearing system reference (FedNow reference)
                .clearingSystemReference(
                        "ACCP".equals(paymentStatus)
                                ? idGenerator.generateClearingSystemReference()
                                : null)
                // Account servicer reference (bank's internal reference)
                .accountServicingReference(idGenerator.generateAccountServicerReference())
                // Original transaction reference
                .originalTransactionReference(buildOriginalTransactionReference(txn))
                .build();
    }

    /**
     * Builds status reason information based on payment status.
     *
     * @param context converter context
     * @param paymentStatus payment status
     * @return list of status reasons
     */
    private List<Pacs002.StatusReasonInformation> buildStatusReasons(
            ConverterContext context, String paymentStatus) {

        List<Pacs002.StatusReasonInformation> reasons = new ArrayList<>();

        switch (paymentStatus) {
            case "ACCP":
                // No reason needed for acceptance
                break;

            case "RJCT":
                // Build rejection reasons from validation errors
                reasons.addAll(buildRejectionReasons(context));
                break;

            case "PDNG":
                // Build pending reasons
                reasons.add(buildPendingReason(context));
                break;
        }

        return reasons;
    }

    /**
     * Builds rejection reasons from validation errors.
     *
     * @param context converter context with validation results
     * @return list of rejection reasons
     */
    private List<Pacs002.StatusReasonInformation> buildRejectionReasons(
            ConverterContext context) {

        List<Pacs002.StatusReasonInformation> reasons = new ArrayList<>();

        // If no validation results, use generic reason
        if (context.getValidationResults() == null ||
                context.getValidationResults().getErrors() == null) {
            reasons.add(Pacs002.StatusReasonInformation.builder()
                    .reason(StatusReason.builder()
                            .reasonCode("MS03")
                            .additionalInformation(List.of("Not specified reason"))
                            .build())
                    .build());
            return reasons;
        }

        // Map validation errors to ISO 20022 reason codes
        for (ConverterContext.ValidationError error :
                context.getValidationResults().getErrors()) {

            String reasonCode = mapErrorCodeToReasonCode(error.getErrorCode());
            String reasonMessage = error.getErrorMessage();

            reasons.add(Pacs002.StatusReasonInformation.builder()
                    .reason(StatusReason.builder()
                            .reasonCode(reasonCode)
                            .additionalInformation(List.of(reasonMessage))
                            .build())
                    .build());
        }

        return reasons;
    }

    /**
     * Builds pending reason.
     *
     * @param context converter context
     * @return pending reason
     */
    private Pacs002.StatusReasonInformation buildPendingReason(ConverterContext context) {
        String reason = "Manual review required";

        // Check if fraud score is the reason
        if (context.isFraudScoreAbove(60)) {
            reason = "High-risk transaction requires additional verification";
        }

        return Pacs002.StatusReasonInformation.builder()
                .reason(StatusReason.builder()
                        .reasonCode("MS03")
                        .additionalInformation(List.of(reason))
                        .build())
                .build();
    }

    /**
     * Maps validation error codes to ISO 20022 reason codes.
     *
     * @param errorCode validation error code
     * @return ISO 20022 reason code
     */
    private String mapErrorCodeToReasonCode(String errorCode) {
        if (errorCode == null) {
            return "MS03"; // Not specified
        }

        // Account-related errors
        if (errorCode.startsWith("ACC") || errorCode.startsWith("CRA")) {
            if (errorCode.contains("001") || errorCode.equals("DA001")) {
                return "AC01"; // Incorrect account number
            } else if (errorCode.contains("009") || errorCode.contains("004")) {
                return "AC04"; // Closed account
            } else if (errorCode.contains("010") || errorCode.contains("005")) {
                return "AC06"; // Blocked account
            }
        }

        // OFAC/Sanctions
        if (errorCode.startsWith("OFAC")) {
            return "AG01"; // Transaction forbidden
        }

        // Amount-related errors
        if (errorCode.startsWith("AMT")) {
            if (errorCode.contains("003")) {
                return "AM09"; // Wrong amount / Amount exceeds limit
            } else if (errorCode.contains("004") || errorCode.equals("AM004")) {
                return "AM04"; // Insufficient funds
            }
        }

        // Fraud
        if (errorCode.startsWith("FRD")) {
            if (errorCode.contains("009")) {
                return "AG01"; // Critical fraud risk - transaction forbidden
            }
            return "FRAD"; // Fraudulent payment
        }

        // Debtor/Creditor info
        if (errorCode.equals("DB001") || errorCode.equals("CR001")) {
            return "RR03"; // Missing party info
        }

        // Duplicate
        if (errorCode.contains("DUP")) {
            return "AM05"; // Duplicate payment
        }

        // XML/Format errors
        if (errorCode.startsWith("XML")) {
            return "FF01"; // Invalid format
        }

        // Bank identifier
        if (errorCode.startsWith("BANK")) {
            return "RC01"; // Bank identifier incorrect
        }

        // Default
        return "MS03"; // Not specified reason
    }

    /**
     * Builds original transaction reference.
     *
     * @param txn transaction from pacs.008
     * @return original transaction reference
     */
    private Pacs002.OriginalTransactionReference buildOriginalTransactionReference(
            Pacs008.CreditTransferTransactionInformation txn) {

        return Pacs002.OriginalTransactionReference.builder()
                .interbankSettlementAmount(txn.getInterbankSettlementAmount())
                .interbankSettlementDate(txn.getInterbankSettlementDate())
                .paymentTypeInformation(txn.getPaymentTypeInformation() != null
                        ? Pacs002.PaymentTypeInformation.builder()
                        .instructionPriority(txn.getPaymentTypeInformation()
                                .getInstructionPriority())
                        .clearingChannel(txn.getPaymentTypeInformation()
                                .getClearingChannel())
                        .serviceLevel(txn.getPaymentTypeInformation()
                                .getServiceLevel())
                        .localInstrument(txn.getPaymentTypeInformation()
                                .getLocalInstrument())
                        .categoryPurpose(txn.getPaymentTypeInformation()
                                .getCategoryPurpose())
                        .build()
                        : null)
                .debtor(txn.getDebtor())
                .debtorAccount(txn.getDebtorAccount())
                .debtorAgent(txn.getDebtorAgent())
                .creditor(txn.getCreditor())
                .creditorAccount(txn.getCreditorAccount())
                .creditorAgent(txn.getCreditorAgent())
                .remittanceInformation(txn.getRemittanceInformation())
                .build();
    }
}
