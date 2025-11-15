package com.fednow.iso20022.api.controller;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.converter.phase2.*;
import com.fednow.iso20022.domain.admi.Admi007;
import com.fednow.iso20022.domain.camt.Camt029;
import com.fednow.iso20022.domain.camt.Camt056;
import com.fednow.iso20022.domain.pacs.Pacs004;
import com.fednow.iso20022.domain.pacs.Pacs007;
import com.fednow.iso20022.domain.pacs.Pacs008;
import com.fednow.iso20022.domain.pain.Pain002;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST API Controller for Phase 2 - Investigation & Exception Handling.
 *
 * Endpoints for payment cancellations, reversals, and receipt acknowledgments.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/convert/investigation")
@RequiredArgsConstructor
@Tag(name = "Phase 2: Investigation & Exceptions", description = "Payment cancellation, reversal, and acknowledgment conversions")
public class Phase2InvestigationController {

    private final Camt056ToPacs004Converter camt056ToPacs004Converter;
    private final Pacs008ToPacs007Converter pacs008ToPacs007Converter;
    private final AnyMessageToAdmi007Converter anyMessageToAdmi007Converter;
    private final Camt029ToPain002Converter camt029ToPain002Converter;

    @Operation(
            summary = "Convert cancellation request to payment return",
            description = """
                    Converts camt.056 (FI to FI Payment Cancellation Request) to pacs.004 (Payment Return).

                    Processes a request to cancel/return a payment that has been sent.

                    **Cancellation Scenarios:**
                    - CUST: Customer requests cancellation
                    - DUPL: Duplicate payment sent
                    - FRAD: Fraud detected after payment sent
                    - TECH: Technical error discovered
                    - AGNT: Incorrect agent/routing

                    **FedNow Windows:**
                    - Payment must not be settled
                    - Must be within allowed return window
                    - Some returns require debtor authorization

                    **Workflow:**
                    1. Receive camt.056 cancellation request
                    2. Validate payment can be returned
                    3. Generate pacs.004 payment return
                    4. Send through FedNow
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Cancellation converted to payment return",
                    content = @Content(schema = @Schema(implementation = Pacs004.class))
            )
    })
    @PostMapping(value = "/camt056-to-pacs004",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs004>> convertCamt056ToPacs004(
            @Parameter(description = "Payment cancellation request", required = true)
            @RequestBody Camt056 camt056) {

        log.info("Converting camt.056 to pacs.004 - Cancellation request");

        ConverterContext context = ConverterContext.builder().build();

        return camt056ToPacs004Converter.convert(camt056, context)
                .map(pacs004 -> ApiResponse.success(pacs004,
                        "Successfully converted cancellation to payment return"))
                .doOnSuccess(response -> log.info("Payment return generated from cancellation"));
    }

    @Operation(
            summary = "Generate payment reversal request",
            description = """
                    Converts pacs.008 (FI to FI Customer Credit Transfer) to pacs.007 (FI to FI Payment Reversal).

                    Generates a reversal request when the originating bank needs to reverse a payment.

                    **Key Difference:**
                    - pacs.007: Reversal initiated by DEBTOR agent (originating bank)
                    - pacs.004: Return initiated by CREDITOR agent (receiving bank)

                    **Reversal Scenarios:**
                    - CUST: Customer requests urgent reversal
                    - FRAD: Fraud detected by originating bank
                    - DUPL: Duplicate payment (system error)
                    - TECH: Technical error discovered

                    **Timing:**
                    - Must be initiated before settlement
                    - Response (pacs.002) confirms if accepted
                    - FedNow has strict time windows

                    **Workflow:**
                    1. Detect need for reversal
                    2. Generate pacs.007
                    3. Send to FedNow
                    4. Await pacs.002 acceptance/rejection
                    """
    )
    @PostMapping(value = "/pacs008-to-pacs007",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs007>> convertPacs008ToPacs007(
            @RequestBody Pacs008 pacs008,
            @Parameter(description = "Reversal reason code", example = "FRAD")
            @RequestParam String reasonCode,
            @Parameter(description = "Reversal explanation")
            @RequestParam(required = false) String explanation) {

        log.info("Converting pacs.008 to pacs.007 - Reversal reason: {}", reasonCode);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("reversalReasonCode", reasonCode);
        if (explanation != null) {
            context.setAttribute("reversalExplanation", explanation);
        }

        return pacs008ToPacs007Converter.convert(pacs008, context)
                .map(pacs007 -> ApiResponse.success(pacs007,
                        "Successfully generated payment reversal request"))
                .doOnSuccess(response -> log.info("Payment reversal generated"));
    }

    @Operation(
            summary = "Generate receipt acknowledgment",
            description = """
                    Converts any ISO 20022 message to admi.007 (Receipt Acknowledgement).

                    Universal acknowledgment that confirms message receipt and validates structure.

                    **Purpose:**
                    - Confirms message was received
                    - Validates message structure (schema)
                    - Confirms authentication passed
                    - Provides receipt timestamp
                    - Does NOT indicate business acceptance (that's pacs.002)

                    **Response Timing:**
                    - MUST send within 500ms of receipt
                    - Sent before business validation
                    - Critical for non-repudiation

                    **Status Codes:**
                    - ACPT: Message received and structure valid
                    - RJCT: Message rejected (schema, auth, duplicate)

                    **Rejection Reasons:**
                    - SCHF: Schema validation failed
                    - AUTHF: Authentication failed
                    - ENCF: Encryption/signature invalid
                    - DUPL: Duplicate message

                    **Workflow:**
                    1. Receive ISO 20022 message
                    2. Validate structure and authentication
                    3. Generate admi.007 immediately
                    4. Send acknowledgment
                    5. Continue with business processing
                    """
    )
    @PostMapping(value = "/any-to-admi007",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Admi007>> convertAnyToAdmi007(
            @RequestBody Object sourceMessage,
            @Parameter(description = "Receipt status: ACPT or RJCT", example = "ACPT")
            @RequestParam(defaultValue = "ACPT") String receiptStatus,
            @Parameter(description = "Original message ID")
            @RequestParam String originalMessageId,
            @Parameter(description = "Original message type", example = "pacs.008.001.11")
            @RequestParam String originalMessageType) {

        log.info("Generating admi.007 receipt acknowledgment - Status: {}", receiptStatus);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("receiptStatus", receiptStatus);
        context.setAttribute("originalMessageId", originalMessageId);
        context.setAttribute("originalMessageNameId", originalMessageType);

        return anyMessageToAdmi007Converter.convert(sourceMessage, context)
                .map(admi007 -> ApiResponse.success(admi007,
                        "Successfully generated receipt acknowledgment"))
                .doOnSuccess(response -> log.info("Receipt acknowledgment generated"));
    }

    @Operation(
            summary = "Generate successful receipt acknowledgment",
            description = "Helper endpoint to generate successful receipt acknowledgment (ACPT status)"
    )
    @PostMapping(value = "/success-receipt",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Admi007>> generateSuccessReceipt(
            @RequestBody Object sourceMessage,
            @RequestParam String originalMessageId,
            @RequestParam String originalMessageType) {

        log.info("Generating successful receipt - Message: {}", originalMessageId);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("originalMessageId", originalMessageId);
        context.setAttribute("originalMessageNameId", originalMessageType);

        return anyMessageToAdmi007Converter.createSuccessfulReceipt(sourceMessage, context)
                .map(admi007 -> ApiResponse.success(admi007,
                        "Message received successfully"))
                .doOnSuccess(response -> log.info("Successful receipt generated"));
    }

    @Operation(
            summary = "Generate authentication failure acknowledgment",
            description = "Helper endpoint to generate authentication failure acknowledgment"
    )
    @PostMapping(value = "/auth-failure-receipt",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Admi007>> generateAuthFailureReceipt(
            @RequestBody Object sourceMessage,
            @RequestParam String originalMessageId,
            @RequestParam String originalMessageType) {

        log.info("Generating auth failure receipt - Message: {}", originalMessageId);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("originalMessageId", originalMessageId);
        context.setAttribute("originalMessageNameId", originalMessageType);

        return anyMessageToAdmi007Converter.createAuthenticationFailure(sourceMessage, context)
                .map(admi007 -> ApiResponse.success(admi007,
                        "Authentication failure acknowledgment generated"))
                .doOnSuccess(response -> log.info("Auth failure receipt generated"));
    }

    @Operation(
            summary = "Convert investigation result to customer status",
            description = """
                    Converts camt.029 (Resolution of Investigation) to pain.002 (Customer Payment Status Report).

                    Translates payment investigation outcome into customer-friendly status report.

                    **Investigation Scenarios:**
                    - Missing payment (customer claims not received)
                    - Payment discrepancy (amount/details don't match)
                    - Customer complaint (disputed transaction)
                    - Regulatory inquiry (compliance investigation)
                    - Fraud investigation (suspected fraud)

                    **Resolution Outcomes:**
                    - RSLV (Resolved): Payment found and processed → Status ACCP
                    - PNDG (Pending): Investigation ongoing → Status PDNG
                    - CNCL (Cancelled): Payment cancelled → Status RJCT
                    - NRES (No Resolution): Cannot resolve → Status RJCT with explanation

                    **Customer Messages:**
                    - Payment found and processed successfully
                    - Payment was returned (with reason)
                    - Investigation ongoing (expected completion)
                    - Payment not found in records
                    - Corrective action taken

                    **Workflow:**
                    1. Bank completes investigation
                    2. Generate camt.029 with results
                    3. Convert to pain.002
                    4. Send customer-friendly status to customer
                    """
    )
    @PostMapping(value = "/camt029-to-pain002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pain002>> convertCamt029ToPain002(
            @Parameter(description = "Investigation resolution message", required = true)
            @RequestBody Camt029 camt029) {

        log.info("Converting camt.029 to pain.002 - Investigation result to customer status");

        ConverterContext context = ConverterContext.builder().build();

        return camt029ToPain002Converter.convert(camt029, context)
                .map(pain002 -> ApiResponse.success(pain002,
                        "Successfully converted investigation result to customer status"))
                .doOnSuccess(response -> log.info("Customer status generated from investigation result"));
    }
}
