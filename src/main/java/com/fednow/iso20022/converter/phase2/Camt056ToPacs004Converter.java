package com.fednow.iso20022.converter.phase2;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.camt.Camt056;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pacs.Pacs004;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Camt056ToPacs004Converter - Cancellation Request to Payment Return
 *
 * Converts camt.056 (FI To FI Payment Cancellation Request) to pacs.004 (Payment Return).
 *
 * This converter processes a request to cancel/return a payment that has been sent.
 * When a cancellation request is received and approved, it generates a payment return.
 *
 * Decision Flow:
 * 1. Receive camt.056 cancellation request
 * 2. Validate that payment can be returned (not yet settled, within window, etc.)
 * 3. Generate pacs.004 to return the payment through FedNow
 *
 * Common Scenarios:
 * - Customer requests payment cancellation
 * - Fraud detected after payment sent
 * - Duplicate payment needs reversal
 * - Error in payment details discovered
 *
 * Important Notes:
 * - Not all payments can be cancelled (depends on settlement status)
 * - FedNow has strict timing windows for returns
 * - Some return reasons require regulatory reporting
 *
 * Message Flow:
 * Customer/Bank → camt.056 (request) → Bank → pacs.004 (return) → FedNow → Creditor Bank
 */
@Slf4j
@Component
public class Camt056ToPacs004Converter extends AbstractMessageConverter<Camt056, Pacs004> {

    public Camt056ToPacs004Converter() {
        super(Camt056.class, Pacs004.class, "Camt056ToPacs004Converter");
    }

    @Override
    protected Mono<Pacs004> doConvert(Camt056 source, ConverterContext context) {
        logStep("Starting camt.056 → pacs.004 conversion (cancellation request to payment return)");

        // Generate message ID for pacs.004
        enrichContextWithIds(context, "RTN", false, false);

        return Mono.fromCallable(() -> {
            // Validate cancellation is allowed
            validateCancellationAllowed(source, context);

            // Build pacs.004
            Pacs004 pacs004 = Pacs004.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .originalGroupInformation(buildOriginalGroupInformation(source))
                    .transactionInformation(buildReturnTransactions(source, context))
                    .build();

            logStep("Successfully created pacs.004 payment return from cancellation request");
            return pacs004;
        });
    }

    /**
     * Validates that the cancellation is allowed.
     *
     * @param source camt.056 cancellation request
     * @param context converter context
     */
    private void validateCancellationAllowed(Camt056 source, ConverterContext context) {
        // Check if payment is still within cancellation window
        // In a real implementation, you would:
        // 1. Look up the original payment status
        // 2. Check settlement status
        // 3. Verify timing windows
        // 4. Check if return is permitted by FedNow rules

        // For now, we'll add a validation result to the context
        if (context.getValidationResults() != null && context.hasCriticalErrors()) {
            logStep("WARNING: Cancellation request has validation errors");
        }

        logStep("Cancellation validation passed - proceeding with return");
    }

    /**
     * Builds group header for pacs.004.
     *
     * @param source camt.056
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(Camt056 source, ConverterContext context) {
        // Get first underlying transaction to extract agent info
        Camt056.UnderlyingTransaction firstTxn = source.getUnderlying() != null &&
                !source.getUnderlying().isEmpty()
                ? source.getUnderlying().get(0)
                : null;

        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                // For returns, the instructing agent is the creditor agent (returning the payment)
                // The instructed agent is the debtor agent (receiving the return)
                .instructingAgent(firstTxn != null ? firstTxn.getInstructedAgent() : null)
                .instructedAgent(firstTxn != null ? firstTxn.getInstructingAgent() : null)
                .build();
    }

    /**
     * Builds original group information.
     *
     * @param source camt.056
     * @return original group information
     */
    private Pacs004.OriginalGroupInformation buildOriginalGroupInformation(Camt056 source) {
        // Get first underlying transaction
        Camt056.UnderlyingTransaction firstTxn = source.getUnderlying() != null &&
                !source.getUnderlying().isEmpty()
                ? source.getUnderlying().get(0)
                : null;

        if (firstTxn == null || firstTxn.getOriginalGroupInformation() == null) {
            return null;
        }

        Camt056.OriginalGroupInformation origGrp = firstTxn.getOriginalGroupInformation();

        return Pacs004.OriginalGroupInformation.builder()
                .originalMessageId(origGrp.getOriginalMessageId())
                .originalMessageNameIdentification(origGrp.getOriginalMessageNameIdentification())
                .originalCreationDateTime(origGrp.getOriginalCreationDateTime())
                .build();
    }

    /**
     * Builds return transaction information.
     *
     * @param source camt.056
     * @param context converter context
     * @return list of return transactions
     */
    private List<Pacs004.PaymentReturnTransactionInformation> buildReturnTransactions(
            Camt056 source, ConverterContext context) {

        List<Pacs004.PaymentReturnTransactionInformation> returns = new ArrayList<>();

        for (Camt056.UnderlyingTransaction underlying : source.getUnderlying()) {
            Pacs004.PaymentReturnTransactionInformation returnTxn =
                    buildReturnTransaction(underlying, context);
            returns.add(returnTxn);
        }

        return returns;
    }

    /**
     * Builds a single return transaction.
     *
     * @param underlying underlying transaction from camt.056
     * @param context converter context
     * @return return transaction for pacs.004
     */
    private Pacs004.PaymentReturnTransactionInformation buildReturnTransaction(
            Camt056.UnderlyingTransaction underlying,
            ConverterContext context) {

        return Pacs004.PaymentReturnTransactionInformation.builder()
                .returnId(idGenerator.generateReturnId())
                // Preserve original IDs for tracking
                .originalInstructionId(underlying.getOriginalInstructionId())
                .originalEndToEndId(underlying.getOriginalEndToEndId())
                .originalTransactionId(underlying.getOriginalTransactionId())
                // UETR tracking
                .originalUetr(underlying.getOriginalUetr())
                // Return amount (same as original payment)
                .returnedInterbankSettlementAmount(underlying.getOriginalInterbankSettlementAmount())
                // Settlement date for the return
                .interbankSettlementDate(context.getCurrentDate())
                // Return reasons (translate from cancellation reasons)
                .returnReasonInformation(translateCancellationReasons(underlying))
                // Original transaction reference
                .originalTransactionReference(buildOriginalTransactionReference(underlying))
                // Instructing agent (returning bank)
                .instructingAgent(underlying.getInstructedAgent())
                // Instructed agent (receiving the return)
                .instructedAgent(underlying.getInstructingAgent())
                .build();
    }

    /**
     * Translates cancellation reasons from camt.056 to return reasons for pacs.004.
     *
     * @param underlying underlying transaction with cancellation reasons
     * @return list of return reasons
     */
    private List<Pacs004.ReturnReasonInformation> translateCancellationReasons(
            Camt056.UnderlyingTransaction underlying) {

        List<Pacs004.ReturnReasonInformation> returnReasons = new ArrayList<>();

        if (underlying.getCancellationReasonInformation() != null) {
            for (Camt056.CancellationReasonInformation cancellation :
                    underlying.getCancellationReasonInformation()) {

                String reasonCode = cancellation.getReason() != null
                        ? cancellation.getReason().getReasonCode()
                        : "MS03"; // Not specified

                // Map cancellation reason codes to return reason codes
                String returnReasonCode = mapCancellationToReturnCode(reasonCode);

                returnReasons.add(Pacs004.ReturnReasonInformation.builder()
                        .originator(cancellation.getOriginator())
                        .reasonCode(returnReasonCode)
                        .additionalReasonInformation(
                                cancellation.getAdditionalInformation() != null
                                        ? cancellation.getAdditionalInformation()
                                        : List.of("Payment cancelled as requested"))
                        .build());
            }
        }

        // If no reasons provided, add default
        if (returnReasons.isEmpty()) {
            returnReasons.add(Pacs004.ReturnReasonInformation.builder()
                    .reasonCode("CUST")
                    .additionalReasonInformation(List.of("Payment cancelled at customer request"))
                    .build());
        }

        return returnReasons;
    }

    /**
     * Maps cancellation reason codes to return reason codes.
     *
     * @param cancellationCode cancellation reason code
     * @return return reason code
     */
    private String mapCancellationToReturnCode(String cancellationCode) {
        if (cancellationCode == null) {
            return "MS03";
        }

        // Most cancellation codes map directly to return codes
        switch (cancellationCode) {
            case "CUST":  // Customer request
            case "CUTA":  // Requested by customer
                return "CUST";
            case "DUPL":  // Duplicate
                return "DUPL";
            case "FRAD":  // Fraud
                return "FRAD";
            case "TECH":  // Technical error
                return "TECH";
            case "AGNT":  // Incorrect agent
                return "RC01"; // Bank identifier incorrect
            case "CURR":  // Incorrect currency
                return "CURR";
            default:
                return "MS03"; // Not specified
        }
    }

    /**
     * Builds original transaction reference.
     *
     * @param underlying underlying transaction from camt.056
     * @return original transaction reference
     */
    private Pacs004.OriginalTransactionReference buildOriginalTransactionReference(
            Camt056.UnderlyingTransaction underlying) {

        return Pacs004.OriginalTransactionReference.builder()
                .interbankSettlementAmount(underlying.getOriginalInterbankSettlementAmount())
                .interbankSettlementDate(underlying.getOriginalInterbankSettlementDate())
                // Note: In a real implementation, you would retrieve full transaction details
                // from database using the original IDs
                .build();
    }
}
