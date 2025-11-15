package com.fednow.iso20022.converter.phase2;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.camt.Camt029;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pain.Pain002;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Camt029ToPain002Converter - Investigation Result to Customer Payment Status
 *
 * Converts camt.029 (Resolution of Investigation) to pain.002 (Customer Payment Status Report).
 *
 * This converter translates the outcome of a payment investigation into a customer-friendly
 * status report that can be sent to the customer who initiated the investigation request.
 *
 * Investigation Scenarios:
 * 1. **Missing Payment**: Customer claims payment not received
 * 2. **Payment Discrepancy**: Amount or details don't match
 * 3. **Customer Complaint**: Disputed transaction
 * 4. **Regulatory Inquiry**: Compliance investigation
 * 5. **Fraud Investigation**: Suspected fraudulent activity
 *
 * Resolution Outcomes Translated:
 * - **RSLV (Resolved)**: Payment found → Status ACCP or details provided
 * - **PNDG (Pending)**: Investigation ongoing → Status PDNG
 * - **CNCL (Cancelled)**: Payment cancelled → Status RJCT
 * - **NRES (No Resolution)**: Cannot resolve → Status with explanation
 *
 * Customer-Friendly Messages:
 * - Payment found and processed successfully
 * - Payment was returned (with reason)
 * - Investigation ongoing (expected completion time)
 * - Payment not found in our records
 * - Corrective action taken (credit applied, etc.)
 *
 * Message Flow:
 * Bank completes investigation → camt.029 generated → Convert to pain.002 → Send to customer
 */
@Slf4j
@Component
public class Camt029ToPain002Converter extends AbstractMessageConverter<Camt029, Pain002> {

    public Camt029ToPain002Converter() {
        super(Camt029.class, Pain002.class, "Camt029ToPain002Converter");
    }

    @Override
    protected Mono<Pain002> doConvert(Camt029 source, ConverterContext context) {
        logStep("Starting camt.029 → pain.002 conversion (investigation result → customer status)");

        // Generate message ID for pain.002
        enrichContextWithIds(context, "INVRES", false, false);

        return Mono.fromCallable(() -> {
            // Build pain.002
            Pain002 pain002 = Pain002.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .originalGroupInformationAndStatus(
                            buildOriginalGroupInformation(source))
                    .transactionInformationAndStatus(
                            buildTransactionStatus(source, context))
                    .build();

            logStep("Successfully created pain.002 customer status from investigation result");
            return pain002;
        });
    }

    /**
     * Builds group header for pain.002.
     */
    private GroupHeader buildGroupHeader(Camt029 source, ConverterContext context) {
        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds original group information.
     */
    private Pain002.OriginalGroupInformationAndStatus buildOriginalGroupInformation(
            Camt029 source) {

        Camt029.InvestigatedCase investigatedCase =
                source.getResolutionOfInvestigation().getInvestigatedCase();

        String groupStatus = mapInvestigationStatusToGroupStatus(
                source.getResolutionOfInvestigation().getInvestigationStatus());

        return Pain002.OriginalGroupInformationAndStatus.builder()
                .originalMessageId(investigatedCase.getOriginalMessageId())
                .originalMessageNameIdentification(
                        investigatedCase.getOriginalMessageNameIdentification())
                .originalCreationDateTime(investigatedCase.getOriginalCreationDateTime())
                .groupStatus(groupStatus)
                .build();
    }

    /**
     * Maps investigation status to payment group status.
     */
    private String mapInvestigationStatusToGroupStatus(String investigationStatus) {
        if (investigationStatus == null) {
            return "PDNG";
        }

        switch (investigationStatus) {
            case "RSLV":  // Resolved
                return "ACCP";
            case "PNDG":  // Pending
                return "PDNG";
            case "CNCL":  // Cancelled
                return "RJCT";
            case "NRES":  // No resolution
                return "RJCT";
            default:
                return "PDNG";
        }
    }

    /**
     * Builds transaction status information.
     */
    private List<Pain002.TransactionInformationAndStatus> buildTransactionStatus(
            Camt029 source,
            ConverterContext context) {

        List<Pain002.TransactionInformationAndStatus> statusList = new ArrayList<>();

        Camt029.InvestigatedCase investigatedCase =
                source.getResolutionOfInvestigation().getInvestigatedCase();

        List<Camt029.InvestigationResult> results =
                source.getResolutionOfInvestigation().getInvestigationResult();

        if (results != null && !results.isEmpty()) {
            for (Camt029.InvestigationResult result : results) {
                Pain002.TransactionInformationAndStatus status =
                        buildTransactionStatusFromResult(investigatedCase, result, context);
                statusList.add(status);
            }
        } else {
            // No specific results, create generic status
            statusList.add(buildGenericTransactionStatus(investigatedCase, source, context));
        }

        return statusList;
    }

    /**
     * Builds transaction status from investigation result.
     */
    private Pain002.TransactionInformationAndStatus buildTransactionStatusFromResult(
            Camt029.InvestigatedCase investigatedCase,
            Camt029.InvestigationResult result,
            ConverterContext context) {

        Pain002.TransactionInformationAndStatus.TransactionInformationAndStatusBuilder builder =
                Pain002.TransactionInformationAndStatus.builder()
                        .statusId(generateStatusId())
                        .originalInstructionId(investigatedCase.getOriginalInstructionId())
                        .originalEndToEndId(investigatedCase.getOriginalEndToEndId())
                        .originalTransactionId(investigatedCase.getOriginalTransactionId())
                        .originalUetr(investigatedCase.getOriginalUetr());

        // Determine transaction status and reason
        if (result.getInvestigationExecutionConfirmation() != null) {
            buildStatusFromConfirmation(builder, result.getInvestigationExecutionConfirmation());
        } else if (result.getRejectionReason() != null) {
            buildStatusFromRejection(builder, result.getRejectionReason());
        } else {
            // Default to pending
            builder.transactionStatus("PDNG");
        }

        return builder.build();
    }

    /**
     * Builds status from investigation confirmation.
     */
    private void buildStatusFromConfirmation(
            Pain002.TransactionInformationAndStatus.TransactionInformationAndStatusBuilder builder,
            Camt029.InvestigationExecutionConfirmation confirmation) {

        String confirmationCode = confirmation.getConfirmationCode();

        switch (confirmationCode) {
            case "ACPT":  // Payment accepted/processed
                builder.transactionStatus("ACCP");
                builder.statusReasonInformation(List.of(
                        Pain002.StatusReasonInformation.builder()
                                .reasonCode("MS03")
                                .additionalInformation(List.of(
                                        "Investigation resolved: Payment was successfully processed"))
                                .build()));
                break;

            case "CNCL":  // Payment cancelled
                builder.transactionStatus("RJCT");
                if (confirmation.getCancellationDetails() != null) {
                    builder.statusReasonInformation(List.of(
                            Pain002.StatusReasonInformation.builder()
                                    .reasonCode(confirmation.getCancellationDetails()
                                            .getCancellationReasonCode())
                                    .additionalInformation(
                                            confirmation.getCancellationDetails()
                                                    .getAdditionalCancellationInformation())
                                    .build()));
                }
                break;

            case "MODI":  // Payment modified
                builder.transactionStatus("ACCP");
                builder.statusReasonInformation(List.of(
                        Pain002.StatusReasonInformation.builder()
                                .reasonCode("MS03")
                                .additionalInformation(List.of(
                                        "Investigation resolved: Payment was modified and processed"))
                                .build()));
                break;

            case "PDNG":  // Still pending
                builder.transactionStatus("PDNG");
                builder.statusReasonInformation(List.of(
                        Pain002.StatusReasonInformation.builder()
                                .reasonCode("MS03")
                                .additionalInformation(List.of(
                                        "Investigation ongoing: Your payment is still being investigated"))
                                .build()));
                break;

            case "RJCT":  // Payment rejected
                builder.transactionStatus("RJCT");
                if (confirmation.getOriginalPaymentInformation() != null &&
                    confirmation.getOriginalPaymentInformation().getStatusReasonCode() != null) {
                    builder.statusReasonInformation(List.of(
                            Pain002.StatusReasonInformation.builder()
                                    .reasonCode(confirmation.getOriginalPaymentInformation()
                                            .getStatusReasonCode())
                                    .additionalInformation(List.of(
                                            "Investigation resolved: Payment was rejected"))
                                    .build()));
                }
                break;

            default:
                builder.transactionStatus("PDNG");
        }
    }

    /**
     * Builds status from rejection reason.
     */
    private void buildStatusFromRejection(
            Pain002.TransactionInformationAndStatus.TransactionInformationAndStatusBuilder builder,
            Camt029.RejectionReason rejectionReason) {

        builder.transactionStatus("RJCT");

        String reasonCode = rejectionReason.getReasonCode();
        String customerMessage = mapRejectionReasonToCustomerMessage(reasonCode);

        List<String> additionalInfo = rejectionReason.getAdditionalReasonInformation() != null
                ? rejectionReason.getAdditionalReasonInformation()
                : List.of(customerMessage);

        builder.statusReasonInformation(List.of(
                Pain002.StatusReasonInformation.builder()
                        .reasonCode(reasonCode)
                        .additionalInformation(additionalInfo)
                        .build()));
    }

    /**
     * Maps rejection reason codes to customer-friendly messages.
     */
    private String mapRejectionReasonToCustomerMessage(String reasonCode) {
        if (reasonCode == null) {
            return "Investigation could not be completed";
        }

        switch (reasonCode) {
            case "NFND":
                return "Payment not found in our records. Please verify payment details and contact us if you believe this is an error.";
            case "NPAY":
                return "No payment was made with the provided details. Please check your records.";
            case "TIMO":
                return "Investigation timed out. Please submit a new inquiry with additional details.";
            case "CUST":
                return "Investigation cancelled at your request.";
            default:
                return "Investigation could not resolve this issue. Please contact customer service for assistance.";
        }
    }

    /**
     * Builds generic transaction status when no specific results available.
     */
    private Pain002.TransactionInformationAndStatus buildGenericTransactionStatus(
            Camt029.InvestigatedCase investigatedCase,
            Camt029 source,
            ConverterContext context) {

        String investigationStatus = source.getResolutionOfInvestigation().getInvestigationStatus();
        String transactionStatus = mapInvestigationStatusToGroupStatus(investigationStatus);

        return Pain002.TransactionInformationAndStatus.builder()
                .statusId(generateStatusId())
                .originalInstructionId(investigatedCase.getOriginalInstructionId())
                .originalEndToEndId(investigatedCase.getOriginalEndToEndId())
                .originalTransactionId(investigatedCase.getOriginalTransactionId())
                .originalUetr(investigatedCase.getOriginalUetr())
                .transactionStatus(transactionStatus)
                .statusReasonInformation(List.of(
                        Pain002.StatusReasonInformation.builder()
                                .reasonCode("MS03")
                                .additionalInformation(List.of(
                                        "Investigation " + investigationStatus.toLowerCase() +
                                        ". Please check your account or contact customer service for details."))
                                .build()))
                .build();
    }

    /**
     * Generates status ID.
     */
    private String generateStatusId() {
        return String.format("STS-%s", idGenerator.generateTransactionId());
    }
}
