package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pacs.Pacs004;
import com.fednow.iso20022.domain.pain.Pain007;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Pacs004ToPain007Converter - Return to Reversal Converter
 *
 * Converts pacs.004 (Payment Return) to pain.007 (Customer Payment Reversal).
 *
 * This converter notifies customers that their payment has been returned:
 * - A "return" at the interbank level becomes a "reversal" for the customer
 * - Translates return reason codes to customer-friendly messages
 * - Provides information about why funds are being returned
 *
 * Message Flow:
 * Bank/FedNow → Customer: pacs.004 → pain.007
 *
 * Common Return Reasons:
 * - AC01 → "Payment returned: Incorrect account number"
 * - AC04 → "Payment returned: Recipient's account has been closed"
 * - CUST → "Payment returned at recipient's request"
 * - FRAD → "Payment returned: Suspected fraudulent transaction"
 */
@Slf4j
@Component
public class Pacs004ToPain007Converter extends AbstractMessageConverter<Pacs004, Pain007> {

    public Pacs004ToPain007Converter() {
        super(Pacs004.class, Pain007.class, "Pacs004ToPain007Converter");
    }

    @Override
    protected Mono<Pain007> doConvert(Pacs004 source, ConverterContext context) {
        logStep("Starting pacs.004 → pain.007 conversion");

        // Generate message ID for customer reversal notification
        enrichContextWithIds(context, "RTN", false, false);

        return Mono.fromCallable(() -> {
            // Build pain.007
            Pain007 pain007 = Pain007.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .originalGroupInformation(buildOriginalGroupInformation(source))
                    .originalPaymentInformationAndReversal(
                            buildPaymentInformationReversals(source, context))
                    .build();

            logStep("Successfully created pain.007 for customer");
            return pain007;
        });
    }

    /**
     * Builds group header for pain.007.
     *
     * @param source pacs.004
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(Pacs004 source, ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds original group information.
     *
     * @param source pacs.004
     * @return original group information
     */
    private Pain007.OriginalGroupInformation buildOriginalGroupInformation(Pacs004 source) {
        // Try to get original group info from first transaction
        Pacs004.PaymentReturnTransactionInformation firstTxn =
                source.getTransactionInformation() != null &&
                        !source.getTransactionInformation().isEmpty()
                        ? source.getTransactionInformation().get(0)
                        : null;

        if (firstTxn == null || firstTxn.getOriginalGroupInformation() == null) {
            return null;
        }

        Pacs004.OriginalGroupInformation origGrp = firstTxn.getOriginalGroupInformation();

        return Pain007.OriginalGroupInformation.builder()
                .originalMessageId(origGrp.getOriginalMessageId())
                .originalMessageNameIdentification("pain.001.001.11")
                .originalCreationDateTime(origGrp.getOriginalCreationDateTime())
                .build();
    }

    /**
     * Builds payment information and reversal list.
     *
     * @param source pacs.004
     * @param context converter context
     * @return list of payment information reversals
     */
    private List<Pain007.OriginalPaymentInformationAndReversal> buildPaymentInformationReversals(
            Pacs004 source, ConverterContext context) {

        List<Pain007.OriginalPaymentInformationAndReversal> reversals = new ArrayList<>();

        // Create one payment information block
        Pain007.OriginalPaymentInformationAndReversal pmtReversal =
                Pain007.OriginalPaymentInformationAndReversal.builder()
                        .reversalPaymentInformationId(idGenerator.generateReturnId())
                        .originalPaymentInformationId("PMT-001")
                        .transactionInformation(buildTransactionReversals(source, context))
                        .build();

        reversals.add(pmtReversal);

        return reversals;
    }

    /**
     * Builds transaction reversal entries.
     *
     * @param source pacs.004
     * @param context converter context
     * @return list of transaction reversals
     */
    private List<Pain007.PaymentTransactionInformation> buildTransactionReversals(
            Pacs004 source, ConverterContext context) {

        List<Pain007.PaymentTransactionInformation> reversals = new ArrayList<>();

        for (Pacs004.PaymentReturnTransactionInformation returnTxn :
                source.getTransactionInformation()) {

            Pain007.PaymentTransactionInformation reversal =
                    buildTransactionReversal(returnTxn, context);
            reversals.add(reversal);
        }

        return reversals;
    }

    /**
     * Builds a single transaction reversal entry.
     *
     * @param returnTxn return transaction from pacs.004
     * @param context converter context
     * @return customer reversal for pain.007
     */
    private Pain007.PaymentTransactionInformation buildTransactionReversal(
            Pacs004.PaymentReturnTransactionInformation returnTxn,
            ConverterContext context) {

        return Pain007.PaymentTransactionInformation.builder()
                .reversalId(idGenerator.generateReturnId())
                // Preserve original IDs for tracking
                .originalInstructionId(returnTxn.getOriginalInstructionId())
                .originalEndToEndId(returnTxn.getOriginalEndToEndId())
                .originalTransactionId(returnTxn.getOriginalTransactionId())
                // Reversed amount (amount being returned to customer)
                .reversedAmount(returnTxn.getReturnedInterbankSettlementAmount())
                // Translate return reasons to customer-friendly messages
                .reversalReasonInformation(translateReturnReasons(returnTxn))
                // Original transaction reference
                .originalTransactionReference(
                        buildOriginalTransactionReference(returnTxn))
                .build();
    }

    /**
     * Translates return reasons from pacs.004 to customer-friendly messages.
     *
     * @param returnTxn return transaction from pacs.004
     * @return list of customer-friendly reversal reasons
     */
    private List<Pain007.ReversalReasonInformation> translateReturnReasons(
            Pacs004.PaymentReturnTransactionInformation returnTxn) {

        List<Pain007.ReversalReasonInformation> customerReasons = new ArrayList<>();

        if (returnTxn.getReturnReasonInformation() != null) {
            for (Pacs004.ReturnReasonInformation returnReason :
                    returnTxn.getReturnReasonInformation()) {

                String reasonCode = returnReason.getReasonCode();

                if (reasonCode != null) {
                    customerReasons.add(
                            Pain007.ReversalReasonInformation.CustomerFriendlyReversalMessages
                                    .translateReturnCode(reasonCode));
                }
            }
        }

        // If no specific reasons, add generic return message
        if (customerReasons.isEmpty()) {
            customerReasons.add(Pain007.ReversalReasonInformation.builder()
                    .reasonCode("MS03")
                    .additionalInformation(List.of(
                            "Payment has been returned",
                            "Funds have been returned to your account"
                    ))
                    .build());
        }

        return customerReasons;
    }

    /**
     * Builds original transaction reference.
     *
     * @param returnTxn return transaction from pacs.004
     * @return original transaction reference
     */
    private Pain007.OriginalTransactionReference buildOriginalTransactionReference(
            Pacs004.PaymentReturnTransactionInformation returnTxn) {

        if (returnTxn.getOriginalTransactionReference() == null) {
            return null;
        }

        Pacs004.OriginalTransactionReference origRef =
                returnTxn.getOriginalTransactionReference();

        return Pain007.OriginalTransactionReference.builder()
                .amount(origRef.getInterbankSettlementAmount())
                .requestedExecutionDate(origRef.getInterbankSettlementDate())
                .creditor(origRef.getCreditor())
                .creditorAccount(origRef.getCreditorAccount())
                .creditorAgent(origRef.getCreditorAgent())
                .remittanceInformation(origRef.getRemittanceInformation())
                .build();
    }
}
