package com.fednow.iso20022.converter.phase3;

import com.fednow.iso20022.converter.core.AbstractMessageConverter;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.GroupHeader;
import com.fednow.iso20022.domain.pacs.Pacs028;
import com.fednow.iso20022.domain.pain.Pain013;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Pain013ToPacs028Converter - Activation Request to Status Request
 *
 * Converts pain.013 (Creditor Payment Activation Request) to pacs.028 (FI to FI Payment Status Request).
 *
 * This converter handles the scenario where activating/deactivating a payment instruction
 * requires checking the current status of related payments.
 *
 * Business Scenarios:
 *
 * 1. **Pre-Activation Status Check**
 *    - Before activating suspended payments
 *    - Verify no pending/failed transactions
 *    - Ensure clean state before reactivation
 *
 * 2. **Pre-Deactivation Verification**
 *    - Before suspending payment instructions
 *    - Check for in-flight transactions
 *    - Ensure safe to suspend without orphaning payments
 *
 * 3. **Reactivation Validation**
 *    - Customer requests reactivation after suspension
 *    - Check status of last payments before suspension
 *    - Verify account/mandate still valid
 *
 * 4. **Compliance and Audit**
 *    - Regulatory requirement to track payment status
 *    - Before making activation changes
 *    - Document current state for audit trail
 *
 * Workflow:
 * 1. Receive pain.013 (activation/deactivation request)
 * 2. Generate pacs.028 to query payment status
 * 3. Wait for pacs.002 response with current status
 * 4. Proceed with activation/deactivation if safe
 * 5. Generate activation response
 *
 * Use Cases:
 * - Subscription pause/resume
 * - Standing order activation after account changes
 * - Temporary payment suspension verification
 * - Mandate reactivation after dispute resolution
 */
@Slf4j
@Component
public class Pain013ToPacs028Converter extends AbstractMessageConverter<Pain013, Pacs028> {

    public Pain013ToPacs028Converter() {
        super(Pain013.class, Pacs028.class, "Pain013ToPacs028Converter");
    }

    @Override
    protected Mono<Pacs028> doConvert(Pain013 source, ConverterContext context) {
        logStep("Starting pain.013 → pacs.028 conversion (activation request → status request)");

        // Generate message ID for status request
        enrichContextWithIds(context, "ACTREQ", false, false);

        return Mono.fromCallable(() -> {
            // Validate pain.013
            validatePain013(source);

            // Build pacs.028
            Pacs028 pacs028 = Pacs028.builder()
                    .groupHeader(buildGroupHeader(source, context))
                    .originalGroupInformation(buildOriginalGroupInformation(source))
                    .transactionInformationStatus(buildTransactionStatusRequests(source, context))
                    .build();

            logStep("Successfully created pacs.028 status request from activation request");
            return pacs028;
        });
    }

    /**
     * Validates pain.013 input.
     */
    private void validatePain013(Pain013 source) {
        if (source.getGroupHeader() == null) {
            throw new IllegalArgumentException("Group header is required");
        }

        if (source.getCreditorPaymentActivationRequest() == null ||
            source.getCreditorPaymentActivationRequest().isEmpty()) {
            throw new IllegalArgumentException(
                    "Creditor payment activation request is required");
        }

        Pain013.CreditorPaymentActivationRequest request =
                source.getCreditorPaymentActivationRequest().get(0);

        if (request.getOriginalPaymentInstruction() == null) {
            throw new IllegalArgumentException(
                    "Original payment instruction reference is required");
        }

        logStep("pain.013 validation passed");
    }

    /**
     * Builds group header for pacs.028.
     */
    private GroupHeader buildGroupHeader(Pain013 source, ConverterContext context) {
        Pain013.CreditorPaymentActivationRequest firstRequest =
                source.getCreditorPaymentActivationRequest().get(0);

        return GroupHeader.builder()
                .messageId(context.getGeneratedMessageId())
                .creationDateTime(context.getCurrentTimestamp())
                // Requesting bank is instructing agent
                .instructingAgent(firstRequest.getAccountServicer())
                // Bank holding the payment instruction is instructed agent
                .instructedAgent(firstRequest.getAccountServicer())
                .build();
    }

    /**
     * Builds original group information from payment instruction.
     */
    private Pacs028.OriginalGroupInformation buildOriginalGroupInformation(Pain013 source) {
        Pain013.CreditorPaymentActivationRequest firstRequest =
                source.getCreditorPaymentActivationRequest().get(0);

        Pain013.OriginalPaymentInstruction origInstr =
                firstRequest.getOriginalPaymentInstruction();

        return Pacs028.OriginalGroupInformation.builder()
                .originalMessageId(origInstr.getOriginalMessageId())
                .originalMessageNameIdentification(
                        origInstr.getOriginalMessageNameId() != null
                                ? origInstr.getOriginalMessageNameId()
                                : "pacs.008.001.11")
                .originalCreationDateTime(context.getCurrentTimestamp())
                .build();
    }

    /**
     * Builds transaction status requests.
     */
    private List<Pacs028.PaymentTransactionInformation> buildTransactionStatusRequests(
            Pain013 source, ConverterContext context) {

        List<Pacs028.PaymentTransactionInformation> statusRequests = new ArrayList<>();

        for (Pain013.CreditorPaymentActivationRequest activationRequest :
                source.getCreditorPaymentActivationRequest()) {

            Pacs028.PaymentTransactionInformation statusRequest =
                    buildStatusRequest(activationRequest, context);
            statusRequests.add(statusRequest);
        }

        return statusRequests;
    }

    /**
     * Builds a single status request.
     */
    private Pacs028.PaymentTransactionInformation buildStatusRequest(
            Pain013.CreditorPaymentActivationRequest activationRequest,
            ConverterContext context) {

        Pain013.OriginalPaymentInstruction origInstr =
                activationRequest.getOriginalPaymentInstruction();

        return Pacs028.PaymentTransactionInformation.builder()
                .statusRequestId(generateStatusRequestId())
                // Original payment identifiers
                .originalInstructionId(origInstr.getOriginalInstructionId())
                .originalEndToEndId(origInstr.getOriginalEndToEndId())
                // Agents
                .instructingAgent(activationRequest.getAccountServicer())
                .instructedAgent(activationRequest.getAccountServicer())
                .build();
    }

    /**
     * Generates status request ID.
     */
    private String generateStatusRequestId() {
        return String.format("SREQ-%s", idGenerator.generateTransactionId());
    }

    /**
     * Helper method to create status request before activation.
     */
    public Mono<Pacs028> createPreActivationStatusCheck(
            Pain013 source,
            ConverterContext context) {
        context.setAttribute("requestType", "PRE_ACTIVATION");
        context.setAttribute("requestPurpose",
                "Verify payment status before activation");
        return convert(source, context);
    }

    /**
     * Helper method to create status request before deactivation.
     */
    public Mono<Pacs028> createPreDeactivationStatusCheck(
            Pain013 source,
            ConverterContext context) {
        context.setAttribute("requestType", "PRE_DEACTIVATION");
        context.setAttribute("requestPurpose",
                "Check for in-flight payments before suspension");
        return convert(source, context);
    }

    /**
     * Helper method to create status request for reactivation validation.
     */
    public Mono<Pacs028> createReactivationValidation(
            Pain013 source,
            ConverterContext context) {
        context.setAttribute("requestType", "REACTIVATION_VALIDATION");
        context.setAttribute("requestPurpose",
                "Validate payment history before reactivation");
        return convert(source, context);
    }

    /**
     * Helper method to create audit trail status request.
     */
    public Mono<Pacs028> createAuditTrailRequest(
            Pain013 source,
            ConverterContext context,
            String auditReason) {
        context.setAttribute("requestType", "AUDIT_TRAIL");
        context.setAttribute("requestPurpose", auditReason);
        return convert(source, context);
    }
}
