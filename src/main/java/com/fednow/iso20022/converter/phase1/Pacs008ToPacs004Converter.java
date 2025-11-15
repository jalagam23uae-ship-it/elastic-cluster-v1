package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pacs.Pacs004;
import com.fednow.iso20022.domain.pacs.Pacs008;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Pacs008ToPacs004Converter - Payment to Return
 *
 * Converts pacs.008 (FI to FI Customer Credit Transfer) to pacs.004 (Payment Return).
 *
 * This converter generates a payment return when the creditor agent (receiving bank)
 * needs to return a payment that was received through FedNow.
 *
 * Key Difference from pacs.007:
 * - pacs.004: Return initiated by CREDITOR agent (receiving bank)
 * - pacs.007: Reversal initiated by DEBTOR agent (originating bank)
 *
 * Common Return Scenarios:
 * 1. Account closed or incorrect account number (AC01, AC04)
 * 2. Account blocked or restricted (AC06)
 * 3. Beneficiary deceased or unavailable (BENO, MD07)
 * 4. Customer refuses payment (CUST)
 * 5. Fraud detected by receiving bank (FRAD)
 * 6. Duplicate payment received (DUPL)
 * 7. Technical issues at receiving bank (TECH)
 *
 * FedNow Requirements:
 * - Returns must follow FedNow timing rules
 * - Return reasons must be valid ISO 20022 codes
 * - Original payment details must be preserved
 * - UETR must be maintained for tracking
 *
 * Message Flow:
 * Creditor Agent → pacs.004 (return) → FedNow → Debtor Agent
 * Debtor Agent → pacs.002 (acknowledgment) → FedNow → Creditor Agent
 * Debtor Agent → pain.007 (customer notification) → Customer
 */
@Slf4j
@Component
public class Pacs008ToPacs004Converter extends AbstractMessageConverter<Pacs008, Pacs004> {

    public Pacs008ToPacs004Converter() {
        super(Pacs008.class, Pacs004.class, "Pacs008ToPacs004Converter");
    }

    @Override
    protected Mono<Pacs004> doConvert(Pacs008 source, ConverterContext context) {
        logStep("Starting pacs.008 → pacs.004 conversion (payment to return)");

        // Generate message ID for pacs.004
        enrichContextWithIds(context, "RTN", false, false);

        return Mono.fromCallable(() -> {
            // Get return reason from context
            String returnReasonCode = context.getAttribute("returnReasonCode", String.class);
            if (returnReasonCode == null) {
                returnReasonCode = "AC01"; // Default to incorrect account
            }

            String returnExplanation = context.getAttribute("returnExplanation", String.class);
            if (returnExplanation == null) {
                returnExplanation = "Payment return requested";
            }

            // Build pacs.004
            Pacs004 pacs004 = Pacs004.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .transactionInformation(buildReturnTransactions(source, context,
                            returnReasonCode, returnExplanation))
                    .build();

            logStep("Successfully created pacs.004 payment return");
            return pacs004;
        });
    }

    /**
     * Builds group header for pacs.004.
     *
     * @param source pacs.008
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(Pacs008 source, ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                // For returns, agents are reversed compared to original payment
                // Instructing agent is the creditor agent (initiating return)
                // Instructed agent is the debtor agent (receiving return)
                .instructingAgent(source.getGroupHeader().getInstructedAgent())
                .instructedAgent(source.getGroupHeader().getInstructingAgent())
                .build();
    }

    /**
     * Builds return transactions.
     *
     * @param source pacs.008
     * @param context converter context
     * @param reasonCode return reason code
     * @param explanation return explanation
     * @return list of return transactions
     */
    private List<Pacs004.PaymentReturnTransactionInformation> buildReturnTransactions(
            Pacs008 source,
            ConverterContext context,
            String reasonCode,
            String explanation) {

        List<Pacs004.PaymentReturnTransactionInformation> returns = new ArrayList<>();

        for (Pacs008.CreditTransferTransactionInformation txn :
                source.getCreditTransferTransactionInformation()) {

            Pacs004.PaymentReturnTransactionInformation returnInfo =
                    buildReturnTransaction(txn, source, context, reasonCode, explanation);
            returns.add(returnInfo);
        }

        return returns;
    }

    /**
     * Builds a single return transaction.
     *
     * @param txn transaction from pacs.008
     * @param source pacs.008
     * @param context converter context
     * @param reasonCode return reason code
     * @param explanation return explanation
     * @return return transaction for pacs.004
     */
    private Pacs004.PaymentReturnTransactionInformation buildReturnTransaction(
            Pacs008.CreditTransferTransactionInformation txn,
            Pacs008 source,
            ConverterContext context,
            String reasonCode,
            String explanation) {

        return Pacs004.PaymentReturnTransactionInformation.builder()
                .returnId(idGenerator.generateReturnId())
                // Original group information
                .originalGroupInformation(buildOriginalGroupInformation(source))
                // Preserve original IDs for tracking
                .originalInstructionId(txn.getPaymentIdentification().getInstructionId())
                .originalEndToEndId(txn.getPaymentIdentification().getEndToEndId())
                .originalTransactionId(txn.getPaymentIdentification().getTransactionId())
                .originalUetr(txn.getPaymentIdentification().getUetr())
                // Original amount (what was received)
                .originalInterbankSettlementAmount(txn.getInterbankSettlementAmount())
                .originalInterbankSettlementDate(txn.getInterbankSettlementDate())
                // Returned amount (what's being sent back)
                .returnedInterbankSettlementAmount(txn.getInterbankSettlementAmount())
                // Settlement date for return (typically T+0)
                .interbankSettlementDate(context.getCurrentDate())
                // Agents (reversed from original payment)
                .instructingAgent(txn.getInstructedAgent()) // Creditor agent returns
                .instructedAgent(txn.getInstructingAgent()) // To debtor agent
                // Return reasons
                .returnReasonInformation(buildReturnReasons(txn, reasonCode, explanation))
                // Original transaction reference
                .originalTransactionReference(buildOriginalTransactionReference(txn))
                .build();
    }

    /**
     * Builds original group information.
     *
     * @param source pacs.008
     * @return original group information
     */
    private Pacs004.OriginalGroupInformation buildOriginalGroupInformation(Pacs008 source) {
        return Pacs004.OriginalGroupInformation.builder()
                .originalMessageId(source.getGroupHeader().getMessageId())
                .originalMessageNameIdentification("pacs.008.001.11")
                .originalCreationDateTime(source.getGroupHeader().getCreationDateTime())
                .build();
    }

    /**
     * Builds return reason information.
     *
     * @param txn transaction from pacs.008
     * @param reasonCode return reason code
     * @param explanation return explanation
     * @return list of return reasons
     */
    private List<Pacs004.ReturnReasonInformation> buildReturnReasons(
            Pacs008.CreditTransferTransactionInformation txn,
            String reasonCode,
            String explanation) {

        List<Pacs004.ReturnReasonInformation> reasons = new ArrayList<>();

        // Create return reason
        Pacs004.ReturnReasonInformation reason = Pacs004.ReturnReasonInformation.builder()
                .originator(txn.getCreditor()) // Creditor initiated the return
                .reasonCode(reasonCode)
                .additionalInformation(List.of(
                        "Payment return",
                        explanation))
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
    private Pacs004.OriginalTransactionReference buildOriginalTransactionReference(
            Pacs008.CreditTransferTransactionInformation txn) {

        return Pacs004.OriginalTransactionReference.builder()
                .interbankSettlementAmount(txn.getInterbankSettlementAmount())
                .interbankSettlementDate(txn.getInterbankSettlementDate())
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
     * Helper method to create pacs.004 for incorrect account.
     *
     * @param source pacs.008
     * @param context converter context
     * @param accountNumber incorrect account number
     * @return Mono<Pacs004>
     */
    public Mono<Pacs004> createIncorrectAccountReturn(
            Pacs008 source,
            ConverterContext context,
            String accountNumber) {
        context.setAttribute("returnReasonCode", "AC01");
        context.setAttribute("returnExplanation",
                "Incorrect account number: " + accountNumber);
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.004 for closed account.
     *
     * @param source pacs.008
     * @param context converter context
     * @return Mono<Pacs004>
     */
    public Mono<Pacs004> createClosedAccountReturn(
            Pacs008 source,
            ConverterContext context) {
        context.setAttribute("returnReasonCode", "AC04");
        context.setAttribute("returnExplanation", "Account closed");
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.004 for blocked account.
     *
     * @param source pacs.008
     * @param context converter context
     * @return Mono<Pacs004>
     */
    public Mono<Pacs004> createBlockedAccountReturn(
            Pacs008 source,
            ConverterContext context) {
        context.setAttribute("returnReasonCode", "AC06");
        context.setAttribute("returnExplanation", "Account blocked");
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.004 for fraud detection.
     *
     * @param source pacs.008
     * @param context converter context
     * @param fraudDetails fraud detection details
     * @return Mono<Pacs004>
     */
    public Mono<Pacs004> createFraudReturn(
            Pacs008 source,
            ConverterContext context,
            String fraudDetails) {
        context.setAttribute("returnReasonCode", "FRAD");
        context.setAttribute("returnExplanation", fraudDetails);
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.004 for duplicate payment.
     *
     * @param source pacs.008
     * @param context converter context
     * @return Mono<Pacs004>
     */
    public Mono<Pacs004> createDuplicateReturn(
            Pacs008 source,
            ConverterContext context) {
        context.setAttribute("returnReasonCode", "DUPL");
        context.setAttribute("returnExplanation", "Duplicate payment received");
        return convert(source, context);
    }

    /**
     * Helper method to create pacs.004 for customer refusal.
     *
     * @param source pacs.008
     * @param context converter context
     * @param customerReason customer's reason for refusal
     * @return Mono<Pacs004>
     */
    public Mono<Pacs004> createCustomerRefusalReturn(
            Pacs008 source,
            ConverterContext context,
            String customerReason) {
        context.setAttribute("returnReasonCode", "CUST");
        context.setAttribute("returnExplanation",
                "Customer refused payment: " + customerReason);
        return convert(source, context);
    }
}
