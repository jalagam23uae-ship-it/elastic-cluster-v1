package com.fednow.iso20022.converter.phase3;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pacs.Pacs003;
import com.fednow.iso20022.domain.pacs.Pacs004;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Pacs003ToPacs004Converter - Direct Debit to Payment Return
 *
 * Converts pacs.003 (FI to FI Customer Direct Debit) to pacs.004 (Payment Return).
 *
 * This converter generates a return message when a direct debit cannot be processed
 * or when the debtor disputes/rejects the direct debit collection.
 *
 * Common Return Scenarios for Direct Debits:
 * 1. No mandate on file
 * 2. Mandate cancelled by debtor
 * 3. Insufficient funds in debtor account
 * 4. Account closed or blocked
 * 5. Debtor disputes the collection (unauthorized)
 * 6. Amount exceeds mandate limit
 * 7. Collection outside mandate validity period
 *
 * Return Time Windows:
 * - Technical errors: Immediate return
 * - No mandate: Within 2 business days
 * - Customer dispute: Up to 60 days (varies by mandate type)
 *
 * Message Flow:
 * Debtor Bank receives pacs.003 → Validation fails → Generate pacs.004 → Return to Creditor Bank
 */
@Slf4j
@Component
public class Pacs003ToPacs004Converter extends AbstractMessageConverter<Pacs003, Pacs004> {

    public Pacs003ToPacs004Converter() {
        super(Pacs003.class, Pacs004.class, "Pacs003ToPacs004Converter");
    }

    @Override
    protected Mono<Pacs004> doConvert(Pacs003 source, ConverterContext context) {
        logStep("Starting pacs.003 → pacs.004 conversion (direct debit → payment return)");

        // Generate message ID for return
        enrichContextWithIds(context, "DDRET", false, false);

        return Mono.fromCallable(() -> {
            // Get return reason from context
            String returnReasonCode = context.getAttribute("returnReasonCode", String.class);
            if (returnReasonCode == null) {
                returnReasonCode = "MD01"; // Default: No mandate
            }

            String returnExplanation = context.getAttribute("returnExplanation", String.class);
            if (returnExplanation == null) {
                returnExplanation = "Direct debit collection rejected";
            }

            // Build pacs.004
            Pacs004 pacs004 = Pacs004.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .originalGroupInformation(buildOriginalGroupInformation(source))
                    .transactionInformation(buildReturnTransactions(source, context,
                            returnReasonCode, returnExplanation))
                    .build();

            logStep("Successfully created pacs.004 direct debit return");
            return pacs004;
        });
    }

    /**
     * Builds group header for pacs.004.
     */
    private GroupHeader buildGroupHeader(Pacs003 source, ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                // For returns, debtor agent (who received DD) is now instructing agent
                // Creditor agent (who sent DD) is now instructed agent
                .instructingAgent(source.getGroupHeader().getInstructedAgent())
                .instructedAgent(source.getGroupHeader().getInstructingAgent())
                .build();
    }

    /**
     * Builds original group information.
     */
    private Pacs004.OriginalGroupInformation buildOriginalGroupInformation(Pacs003 source) {
        return Pacs004.OriginalGroupInformation.builder()
                .originalMessageId(source.getGroupHeader().getMessageId())
                .originalMessageNameIdentification("pacs.003.001.10")
                .originalCreationDateTime(source.getGroupHeader().getCreationDateTime())
                .build();
    }

    /**
     * Builds return transaction information.
     */
    private List<Pacs004.PaymentReturnTransactionInformation> buildReturnTransactions(
            Pacs003 source,
            ConverterContext context,
            String reasonCode,
            String explanation) {

        List<Pacs004.PaymentReturnTransactionInformation> returns = new ArrayList<>();

        for (Pacs003.DirectDebitTransactionInformation ddTxn :
                source.getDirectDebitTransactionInformation()) {

            Pacs004.PaymentReturnTransactionInformation returnTxn =
                    buildReturnTransaction(ddTxn, context, reasonCode, explanation);
            returns.add(returnTxn);
        }

        return returns;
    }

    /**
     * Builds a single return transaction.
     */
    private Pacs004.PaymentReturnTransactionInformation buildReturnTransaction(
            Pacs003.DirectDebitTransactionInformation ddTxn,
            ConverterContext context,
            String reasonCode,
            String explanation) {

        return Pacs004.PaymentReturnTransactionInformation.builder()
                .returnId(idGenerator.generateReturnId())
                // Preserve original IDs
                .originalInstructionId(ddTxn.getPaymentIdentification().getInstructionId())
                .originalEndToEndId(ddTxn.getPaymentIdentification().getEndToEndId())
                .originalTransactionId(ddTxn.getPaymentIdentification().getTransactionId())
                .originalUetr(ddTxn.getPaymentIdentification().getUetr())
                // Return amount
                .returnedInterbankSettlementAmount(ddTxn.getInterbankSettlementAmount())
                // Settlement date for return
                .interbankSettlementDate(context.getCurrentDate())
                // Return reasons
                .returnReasonInformation(buildReturnReasons(ddTxn, reasonCode, explanation))
                // Original transaction reference
                .originalTransactionReference(buildOriginalTransactionReference(ddTxn))
                // Agents (reversed from original DD)
                .instructingAgent(ddTxn.getInstructedAgent()) // Debtor agent
                .instructedAgent(ddTxn.getInstructingAgent()) // Creditor agent
                .build();
    }

    /**
     * Builds return reason information.
     */
    private List<Pacs004.ReturnReasonInformation> buildReturnReasons(
            Pacs003.DirectDebitTransactionInformation ddTxn,
            String reasonCode,
            String explanation) {

        List<Pacs004.ReturnReasonInformation> reasons = new ArrayList<>();

        // Create return reason
        Pacs004.ReturnReasonInformation reason = Pacs004.ReturnReasonInformation.builder()
                .originator(ddTxn.getDebtor()) // Debtor is refusing the direct debit
                .reasonCode(reasonCode)
                .additionalReasonInformation(List.of(
                        mapReasonCodeToMessage(reasonCode),
                        explanation))
                .build();

        reasons.add(reason);

        return reasons;
    }

    /**
     * Maps return reason codes to user-friendly messages.
     */
    private String mapReasonCodeToMessage(String reasonCode) {
        switch (reasonCode) {
            case "MD01":
                return "No mandate on file - direct debit authorization not found";
            case "MD02":
                return "Mandate cancelled - customer has revoked authorization";
            case "MD06":
                return "Disputed authorized transaction - customer claims unauthorized";
            case "MD07":
                return "Mandate invalid - mandate details do not match";
            case "AM04":
                return "Insufficient funds - not enough balance to complete direct debit";
            case "AC04":
                return "Account closed - debtor account no longer active";
            case "AC06":
                return "Account blocked - debtor account is restricted";
            case "AM09":
                return "Amount exceeds mandate limit - collection exceeds authorized amount";
            case "NARR":
                return "Narrative reason provided";
            default:
                return "Direct debit return";
        }
    }

    /**
     * Builds original transaction reference.
     */
    private Pacs004.OriginalTransactionReference buildOriginalTransactionReference(
            Pacs003.DirectDebitTransactionInformation ddTxn) {

        return Pacs004.OriginalTransactionReference.builder()
                .interbankSettlementAmount(ddTxn.getInterbankSettlementAmount())
                .interbankSettlementDate(ddTxn.getInterbankSettlementDate())
                // Note: For direct debits, the roles are reversed from credit transfers
                // Debtor = account being debited
                // Creditor = account receiving the collection
                .debtor(ddTxn.getDebtor())
                .debtorAccount(ddTxn.getDebtorAccount())
                .debtorAgent(ddTxn.getDebtorAgent())
                .creditor(ddTxn.getCreditor())
                .creditorAccount(ddTxn.getCreditorAccount())
                .creditorAgent(ddTxn.getCreditorAgent())
                .remittanceInformation(ddTxn.getRemittanceInformation())
                .build();
    }

    /**
     * Helper method to create return for missing mandate.
     */
    public Mono<Pacs004> createNoMandateReturn(
            Pacs003 source,
            ConverterContext context,
            String mandateId) {
        context.setAttribute("returnReasonCode", "MD01");
        context.setAttribute("returnExplanation",
                String.format("No mandate found with ID: %s", mandateId));
        return convert(source, context);
    }

    /**
     * Helper method to create return for cancelled mandate.
     */
    public Mono<Pacs004> createCancelledMandateReturn(
            Pacs003 source,
            ConverterContext context,
            String mandateId,
            String cancellationDate) {
        context.setAttribute("returnReasonCode", "MD02");
        context.setAttribute("returnExplanation",
                String.format("Mandate %s was cancelled on %s", mandateId, cancellationDate));
        return convert(source, context);
    }

    /**
     * Helper method to create return for insufficient funds.
     */
    public Mono<Pacs004> createInsufficientFundsReturn(
            Pacs003 source,
            ConverterContext context) {
        context.setAttribute("returnReasonCode", "AM04");
        context.setAttribute("returnExplanation",
                "Insufficient funds in debtor account to complete direct debit");
        return convert(source, context);
    }

    /**
     * Helper method to create return for disputed transaction.
     */
    public Mono<Pacs004> createDisputedTransactionReturn(
            Pacs003 source,
            ConverterContext context,
            String disputeReason) {
        context.setAttribute("returnReasonCode", "MD06");
        context.setAttribute("returnExplanation",
                String.format("Customer disputes this direct debit: %s", disputeReason));
        return convert(source, context);
    }

    /**
     * Helper method to create return for amount exceeding mandate limit.
     */
    public Mono<Pacs004> createAmountExceedsLimitReturn(
            Pacs003 source,
            ConverterContext context,
            String requestedAmount,
            String mandateLimit) {
        context.setAttribute("returnReasonCode", "AM09");
        context.setAttribute("returnExplanation",
                String.format("Requested amount %s exceeds mandate limit of %s",
                        requestedAmount, mandateLimit));
        return convert(source, context);
    }
}
