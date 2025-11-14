package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pain.Pain002;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Pacs002ToPain002Converter - Status Translation Converter
 *
 * Converts pacs.002 (FI to FI Payment Status Report) to pain.002 (Customer Payment Status Report).
 *
 * This converter translates technical status codes to customer-friendly messages:
 * - Preserves End-to-End ID for customer tracking
 * - Translates ISO 20022 reason codes to user-friendly text
 * - Provides actionable information for rejected payments
 *
 * Message Flow:
 * FedNow/Bank → Customer: pacs.002 → pain.002
 *
 * Key Translations:
 * - AC01 → "Invalid account number provided"
 * - AC04 → "Account closed - please verify with recipient"
 * - AM04 → "Insufficient funds in your account"
 * - ACCP → "Payment successfully completed and funds transferred"
 */
@Slf4j
@Component
public class Pacs002ToPain002Converter extends AbstractMessageConverter<Pacs002, Pain002> {

    public Pacs002ToPain002Converter() {
        super(Pacs002.class, Pain002.class, "Pacs002ToPain002Converter");
    }

    @Override
    protected Mono<Pain002> doConvert(Pacs002 source, ConverterContext context) {
        logStep("Starting pacs.002 → pain.002 conversion");

        // Generate customer-facing message ID
        enrichContextWithIds(context, "CUST-STS", false, false);

        return Mono.fromCallable(() -> {
            // Build pain.002
            Pain002 pain002 = Pain002.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .originalPaymentInformationAndStatus(
                            buildPaymentInformationStatuses(source, context))
                    .build();

            logStep("Successfully created pain.002 for customer");
            return pain002;
        });
    }

    /**
     * Builds group header for pain.002.
     *
     * @param source pacs.002
     * @param context converter context
     * @return group header
     */
    private GroupHeader buildGroupHeader(Pacs002 source, ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds payment information and status list.
     *
     * For customer messages, we group by original payment information ID.
     *
     * @param source pacs.002
     * @param context converter context
     * @return list of payment information statuses
     */
    private List<Pain002.OriginalPaymentInformationAndStatus> buildPaymentInformationStatuses(
            Pacs002 source, ConverterContext context) {

        List<Pain002.OriginalPaymentInformationAndStatus> paymentStatuses = new ArrayList<>();

        // For simplicity, create one payment information block
        // In reality, you'd group by original payment information ID
        Pain002.OriginalPaymentInformationAndStatus pmtSts =
                Pain002.OriginalPaymentInformationAndStatus.builder()
                        // Use a default payment information ID or derive from context
                        .originalPaymentInformationId("PMT-001")
                        // Determine overall status
                        .paymentInformationStatus(determineOverallStatus(source))
                        // Build transaction statuses
                        .transactionInformationAndStatus(buildTransactionStatuses(source, context))
                        .build();

        paymentStatuses.add(pmtSts);

        return paymentStatuses;
    }

    /**
     * Determines overall payment information status.
     *
     * @param source pacs.002
     * @return overall status
     */
    private String determineOverallStatus(Pacs002 source) {
        if (source.getTransactionInformationAndStatus() == null ||
                source.getTransactionInformationAndStatus().isEmpty()) {
            return "RJCT";
        }

        // Check if all transactions have same status
        String firstStatus = source.getTransactionInformationAndStatus().get(0)
                .getTransactionStatus();

        boolean allSame = source.getTransactionInformationAndStatus().stream()
                .allMatch(txn -> firstStatus.equals(txn.getTransactionStatus()));

        if (allSame) {
            return firstStatus;
        } else {
            return "PART"; // Partially processed
        }
    }

    /**
     * Builds transaction status entries.
     *
     * @param source pacs.002
     * @param context converter context
     * @return list of transaction statuses
     */
    private List<Pain002.TransactionInformationAndStatus> buildTransactionStatuses(
            Pacs002 source, ConverterContext context) {

        List<Pain002.TransactionInformationAndStatus> statuses = new ArrayList<>();

        for (Pacs002.TransactionInformationAndStatus interbankStatus :
                source.getTransactionInformationAndStatus()) {

            Pain002.TransactionInformationAndStatus customerStatus =
                    buildTransactionStatus(interbankStatus, context);
            statuses.add(customerStatus);
        }

        return statuses;
    }

    /**
     * Builds a single transaction status entry.
     *
     * @param interbankStatus status from pacs.002
     * @param context converter context
     * @return customer status for pain.002
     */
    private Pain002.TransactionInformationAndStatus buildTransactionStatus(
            Pacs002.TransactionInformationAndStatus interbankStatus,
            ConverterContext context) {

        return Pain002.TransactionInformationAndStatus.builder()
                .statusId(idGenerator.generateStatusId())
                // Preserve original IDs for customer tracking
                .originalInstructionId(interbankStatus.getOriginalInstructionId())
                .originalEndToEndId(interbankStatus.getOriginalEndToEndId())
                .originalTransactionId(interbankStatus.getOriginalTransactionId())
                // Transaction status (same as pacs.002)
                .transactionStatus(interbankStatus.getTransactionStatus())
                // Translate status reasons to customer-friendly messages
                .statusReasonInformation(
                        translateStatusReasons(interbankStatus))
                // Acceptance datetime
                .acceptanceDateTime(interbankStatus.getAcceptanceDateTimeacceptanceDateTimeclearingSystemReference())
                // Account servicer reference (bank's internal reference)
                .accountServicerReference(interbankStatus.getAccountServicingReference())
                // Clearing system reference (FedNow reference)
                .clearingSystemReference(interbankStatus.getClearingSystemReference())
                // Original transaction reference
                .originalTransactionReference(
                        buildOriginalTransactionReference(interbankStatus))
                .build();
    }

    /**
     * Translates status reasons from pacs.002 to customer-friendly messages.
     *
     * @param interbankStatus status from pacs.002
     * @return list of customer-friendly status reasons
     */
    private List<Pain002.StatusReasonInformation> translateStatusReasons(
            Pacs002.TransactionInformationAndStatus interbankStatus) {

        List<Pain002.StatusReasonInformation> customerReasons = new ArrayList<>();

        // Check transaction status
        String txnStatus = interbankStatus.getTransactionStatus();

        if ("ACCP".equals(txnStatus) || "ACSC".equals(txnStatus)) {
            // Accepted - add success message
            customerReasons.add(
                    Pain002.StatusReasonInformation.CustomerFriendlyMessages.acceptanceMessage());
            return customerReasons;
        }

        if ("PDNG".equals(txnStatus)) {
            // Pending - add pending message
            customerReasons.add(
                    Pain002.StatusReasonInformation.CustomerFriendlyMessages.pendingMessage(
                            "Review in progress"));
            return customerReasons;
        }

        // For rejections, translate reason codes
        if (interbankStatus.getStatusReasonInformation() != null) {
            for (Pacs002.StatusReasonInformation interbankReason :
                    interbankStatus.getStatusReasonInformation()) {

                String reasonCode = interbankReason.getReason() != null
                        ? interbankReason.getReason().getReasonCode()
                        : null;

                if (reasonCode != null) {
                    customerReasons.add(
                            Pain002.StatusReasonInformation.CustomerFriendlyMessages
                                    .translateRejectionCode(reasonCode));
                }
            }
        }

        // If no specific reasons, add generic rejection message
        if (customerReasons.isEmpty()) {
            customerReasons.add(
                    Pain002.StatusReasonInformation.CustomerFriendlyMessages
                            .translateRejectionCode("MS03"));
        }

        return customerReasons;
    }

    /**
     * Builds original transaction reference.
     *
     * @param interbankStatus status from pacs.002
     * @return original transaction reference
     */
    private Pain002.OriginalTransactionReference buildOriginalTransactionReference(
            Pacs002.TransactionInformationAndStatus interbankStatus) {

        if (interbankStatus.getOriginalTransactionReference() == null) {
            return null;
        }

        Pacs002.OriginalTransactionReference interbankRef =
                interbankStatus.getOriginalTransactionReference();

        return Pain002.OriginalTransactionReference.builder()
                .amount(interbankRef.getInterbankSettlementAmount())
                .requestedExecutionDate(interbankRef.getInterbankSettlementDate())
                .creditor(interbankRef.getCreditor())
                .creditorAccount(interbankRef.getCreditorAccount())
                .remittanceInformation(interbankRef.getRemittanceInformation())
                .build();
    }
}
