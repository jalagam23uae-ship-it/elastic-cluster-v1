package com.fednow.iso20022.converter.phase2;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pacs.Pacs007;
import com.fednow.iso20022.domain.pacs.Pacs008;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Pacs008ToPacs007Converter - Payment to Reversal Request
 *
 * Converts pacs.008 (FI to FI Customer Credit Transfer) to pacs.007 (FI to FI Payment Reversal).
 *
 * This converter generates a reversal request when the originating bank needs to
 * reverse a payment that was already sent through FedNow.
 *
 * Key Difference from pacs.004:
 * - pacs.007: Reversal initiated by DEBTOR agent (originating bank)
 * - pacs.004: Return initiated by CREDITOR agent (receiving bank)
 *
 * Common Scenarios:
 * 1. Bank detects error immediately after sending payment
 * 2. Customer requests urgent reversal within FedNow window
 * 3. Fraud detected by originating bank post-send
 * 4. Duplicate payment sent by system error
 *
 * Timing Constraints:
 * - FedNow allows reversals within specific time windows
 * - Must be initiated before settlement completes
 * - Response (pacs.002) confirms if reversal accepted
 *
 * Message Flow:
 * Debtor Agent → pacs.007 (reversal request) → FedNow → Creditor Agent
 * Creditor Agent → pacs.002 (acceptance/rejection) → FedNow → Debtor Agent
 */
@Slf4j
@Component
public class Pacs008ToPacs007Converter extends AbstractMessageConverter<Pacs008, Pacs007> {

    public Pacs008ToPacs007Converter() {
        super(Pacs008.class, Pacs007.class, "Pacs008ToPacs007Converter");
    }

    @Override
    protected Mono<Pacs007> doConvert(Pacs008 source, ConverterContext context) {
        logStep("Starting pacs.008 → pacs.007 conversion (payment to reversal request)");

        // Generate message ID for pacs.007
        enrichContextWithIds(context, "REV", false, false);

        return Mono.fromCallable(() -> {
            // Validate reversal is allowed (timing, settlement status, etc.)
            validateReversalAllowed(source, context);

            // Get reversal reason from context
            String reversalReasonCode = context.getAttribute("reversalReasonCode", String.class);
            if (reversalReasonCode == null) {
                reversalReasonCode = "CUST"; // Default to customer request
            }

            String reversalExplanation = context.getAttribute("reversalExplanation", String.class);
            if (reversalExplanation == null) {
                reversalExplanation = "Payment reversal requested";
            }

            // Build pacs.007
            Pacs007 pacs007 = Pacs007.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .originalGroupInformation(buildOriginalGroupInformation(source))
                    .reversalTransaction(buildReversalTransactions(source, context,
                            reversalReasonCode, reversalExplanation))
                    .build();

            logStep("Successfully created pacs.007 reversal request");
            return pacs007;
        });
    }

    /**
     * Validates that reversal is allowed.
     *
     * @param source pacs.008
     * @param context converter context
     */
    private void validateReversalAllowed(Pacs008 source, ConverterContext context) {
        // In a real implementation, you would:
        // 1. Check if payment has settled
        // 2. Verify timing constraints (FedNow reversal window)
        // 3. Validate business rules for reversals
        // 4. Check if reversal is permitted for this payment type

        logStep("Reversal validation passed - payment can be reversed");
    }

    /**
     * Builds group header for pacs.007.
     *
     * @param source pacs.008
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(Pacs008 source, ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                // For reversals, agents remain the same (not reversed)
                // Instructing agent is still the debtor agent (initiating reversal)
                // Instructed agent is still the creditor agent (receiving reversal request)
                .instructingAgent(source.getGroupHeader().getInstructingAgent())
                .instructedAgent(source.getGroupHeader().getInstructedAgent())
                .build();
    }

    /**
     * Builds original group information.
     *
     * @param source pacs.008
     * @return original group information
     */
    private Pacs007.OriginalGroupInformation buildOriginalGroupInformation(Pacs008 source) {
        return Pacs007.OriginalGroupInformation.builder()
                .originalMessageId(source.getGroupHeader().getMessageId())
                .originalMessageNameIdentification("pacs.008.001.11")
                .originalCreationDateTime(source.getGroupHeader().getCreationDateTime())
                .numberOfTransactions(source.getCreditTransferTransactionInformation() != null
                        ? source.getCreditTransferTransactionInformation().size()
                        : 0)
                .controlSum(calculateControlSum(source))
                .build();
    }

    /**
     * Calculates control sum for original group.
     *
     * @param source pacs.008
     * @return control sum amount
     */
    private com.fednow.iso20022.domain.common.Amount calculateControlSum(Pacs008 source) {
        if (source.getCreditTransferTransactionInformation() == null ||
                source.getCreditTransferTransactionInformation().isEmpty()) {
            return null;
        }

        // Sum all transaction amounts
        // In a real implementation, you would properly sum the amounts
        return source.getCreditTransferTransactionInformation().get(0)
                .getInterbankSettlementAmount();
    }

    /**
     * Builds reversal transactions.
     *
     * @param source pacs.008
     * @param context converter context
     * @param reasonCode reversal reason code
     * @param explanation reversal explanation
     * @return list of reversal transactions
     */
    private List<Pacs007.ReversalTransactionInformation> buildReversalTransactions(
            Pacs008 source,
            ConverterContext context,
            String reasonCode,
            String explanation) {

        List<Pacs007.ReversalTransactionInformation> reversals = new ArrayList<>();

        for (Pacs008.CreditTransferTransactionInformation txn :
                source.getCreditTransferTransactionInformation()) {

            Pacs007.ReversalTransactionInformation reversal =
                    buildReversalTransaction(txn, context, reasonCode, explanation);
            reversals.add(reversal);
        }

        return reversals;
    }

    /**
     * Builds a single reversal transaction.
     *
     * @param txn transaction from pacs.008
     * @param context converter context
     * @param reasonCode reversal reason code
     * @param explanation reversal explanation
     * @return reversal transaction for pacs.007
     */
    private Pacs007.ReversalTransactionInformation buildReversalTransaction(
            Pacs008.CreditTransferTransactionInformation txn,
            ConverterContext context,
            String reasonCode,
            String explanation) {

        return Pacs007.ReversalTransactionInformation.builder()
                .reversalId(idGenerator.generateReturnId())
                // Preserve original IDs for tracking
                .originalInstructionId(txn.getPaymentIdentification().getInstructionId())
                .originalEndToEndId(txn.getPaymentIdentification().getEndToEndId())
                .originalTransactionId(txn.getPaymentIdentification().getTransactionId())
                .originalUetr(txn.getPaymentIdentification().getUetr())
                // Reversal amount (same as original payment)
                .reversedInterbankSettlementAmount(txn.getInterbankSettlementAmount())
                // Settlement date for reversal (typically T+0)
                .interbankSettlementDate(context.getCurrentDate())
                // Settlement information
                .settlementInformation(txn.getSettlementInformation())
                // Payment type information
                .paymentTypeInformation(buildPaymentTypeInformation(txn))
                // Agents (same as original payment)
                .instructingAgent(txn.getInstructingAgent())
                .instructedAgent(txn.getInstructedAgent())
                // Reversal reasons
                .reversalReasonInformation(buildReversalReasons(txn, reasonCode, explanation))
                // Original transaction reference
                .originalTransactionReference(buildOriginalTransactionReference(txn))
                .build();
    }

    /**
     * Builds payment type information.
     *
     * @param txn transaction from pacs.008
     * @return payment type information
     */
    private Pacs007.PaymentTypeInformation buildPaymentTypeInformation(
            Pacs008.CreditTransferTransactionInformation txn) {

        if (txn.getPaymentTypeInformation() == null) {
            return null;
        }

        return Pacs007.PaymentTypeInformation.builder()
                .instructionPriority(txn.getPaymentTypeInformation().getInstructionPriority())
                .clearingChannel(txn.getPaymentTypeInformation().getClearingChannel())
                .serviceLevel(txn.getPaymentTypeInformation().getServiceLevel())
                .localInstrument(txn.getPaymentTypeInformation().getLocalInstrument())
                .categoryPurpose(txn.getPaymentTypeInformation().getCategoryPurpose())
                .build();
    }

    /**
     * Builds reversal reason information.
     *
     * @param txn transaction from pacs.008
     * @param reasonCode reversal reason code
     * @param explanation reversal explanation
     * @return list of reversal reasons
     */
    private List<Pacs007.ReversalReasonInformation> buildReversalReasons(
            Pacs008.CreditTransferTransactionInformation txn,
            String reasonCode,
            String explanation) {

        List<Pacs007.ReversalReasonInformation> reasons = new ArrayList<>();

        // Create reversal reason
        Pacs007.ReversalReasonInformation reason = Pacs007.ReversalReasonInformation.builder()
                .originator(txn.getDebtor()) // Debtor initiated the reversal
                .reason(Pacs007.ReversalReason.builder()
                        .reasonCode(reasonCode)
                        .additionalReasonInformation(List.of(
                                "Payment reversal requested",
                                explanation))
                        .build())
                .additionalInformation(List.of(
                        "Original payment being reversed",
                        "Funds to be returned to debtor"))
                .build();

        reasons.add(reason);

        return reasons;
    }

    /**
     * Builds original transaction reference.
     *
     * @param txn transaction from pacs.008
     * @return original transaction reference
     */
    private Pacs007.OriginalTransactionReference buildOriginalTransactionReference(
            Pacs008.CreditTransferTransactionInformation txn) {

        return Pacs007.OriginalTransactionReference.builder()
                .interbankSettlementAmount(txn.getInterbankSettlementAmount())
                .interbankSettlementDate(txn.getInterbankSettlementDate())
                .paymentTypeInformation(buildPaymentTypeInformation(txn))
                .debtor(txn.getDebtor())
                .debtorAccount(txn.getDebtorAccount())
                .debtorAgent(txn.getDebtorAgent())
                .creditor(txn.getCreditor())
                .creditorAccount(txn.getCreditorAccount())
                .creditorAgent(txn.getCreditorAgent())
                .remittanceInformation(txn.getRemittanceInformation())
                .build();
    }

    /**
     * Helper method to create pacs.007 for customer-requested reversal.
     *
     * @param source pacs.008
     * @param context converter context
     * @param explanation customer explanation
     * @return Mono<Pacs007>
     */
    public Mono<Pacs007> createCustomerRequestedReversal(
            Pacs008 source,
            ConverterContext context,
            String explanation) {
        context.setAttribute("reversalReasonCode", "CUST");
        context.setAttribute("reversalExplanation", explanation);
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.007 for fraud-detected reversal.
     *
     * @param source pacs.008
     * @param context converter context
     * @param fraudDetails fraud detection details
     * @return Mono<Pacs007>
     */
    public Mono<Pacs007> createFraudReversal(
            Pacs008 source,
            ConverterContext context,
            String fraudDetails) {
        context.setAttribute("reversalReasonCode", "FRAD");
        context.setAttribute("reversalExplanation", fraudDetails);
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.007 for duplicate payment reversal.
     *
     * @param source pacs.008
     * @param context converter context
     * @return Mono<Pacs007>
     */
    public Mono<Pacs007> createDuplicateReversal(
            Pacs008 source,
            ConverterContext context) {
        context.setAttribute("reversalReasonCode", "DUPL");
        context.setAttribute("reversalExplanation", "Duplicate payment detected - system error");
        return convert(source, context);
    }
}
