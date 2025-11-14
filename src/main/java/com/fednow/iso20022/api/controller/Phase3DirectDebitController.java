package com.fednow.iso20022.api.controller;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.converter.phase3.*;
import com.fednow.iso20022.domain.pacs.Pacs003;
import com.fednow.iso20022.domain.pacs.Pacs004;
import com.fednow.iso20022.domain.pacs.Pacs008;
import com.fednow.iso20022.domain.pacs.Pacs028;
import com.fednow.iso20022.domain.pain.Pain008;
import com.fednow.iso20022.domain.pain.Pain009;
import com.fednow.iso20022.domain.pain.Pain013;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * REST API Controller for Phase 3 - Direct Debit & Mandate Management.
 *
 * Endpoints for direct debit collections and mandate lifecycle management.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/convert/direct-debit")
@RequiredArgsConstructor
@Tag(name = "Phase 3: Direct Debits & Mandates", description = "Direct debit collection and mandate management conversions")
public class Phase3DirectDebitController {

    private final Pain008ToPacs003Converter pain008ToPacs003Converter;
    private final Pacs003ToPacs004Converter pacs003ToPacs004Converter;
    private final Pain009ToPacs008Converter pain009ToPacs008Converter;
    private final Pain013ToPacs028Converter pain013ToPacs028Converter;

    @Operation(
            summary = "Convert customer direct debit to interbank direct debit",
            description = """
                    Converts pain.008 (Customer Direct Debit Initiation) to pacs.003 (FI to FI Customer Direct Debit).

                    Transforms customer-initiated direct debit to interbank format for FedNow.

                    **Key Difference:**
                    - pain.001/pacs.008: PUSH payment (debtor initiates)
                    - pain.008/pacs.003: PULL payment (creditor initiates with authorization)

                    **Mandate Required:**
                    - Mandate ID must be provided
                    - Date of signature required
                    - Sequence type: FRST, RCUR, FNAL, OOFF

                    **Sequence Types:**
                    - FRST: First collection in recurring series
                    - RCUR: Recurring collection
                    - FNAL: Final collection in series
                    - OOFF: One-off collection

                    **Use Cases:**
                    - Recurring bill payments (utilities, subscriptions)
                    - Insurance premium collections
                    - Loan repayments
                    - Membership dues

                    **Workflow:**
                    1. Customer (creditor) submits pain.008
                    2. Bank validates mandate exists
                    3. Convert to pacs.003
                    4. Send to FedNow → Debtor bank
                    """
    )
    @PostMapping(value = "/pain008-to-pacs003",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs003>> convertPain008ToPacs003(
            @Parameter(description = "Customer direct debit initiation", required = true)
            @RequestBody Pain008 pain008) {

        log.info("Converting pain.008 to pacs.003 - Direct debit collection");

        ConverterContext context = ConverterContext.builder().build();

        return pain008ToPacs003Converter.convert(pain008, context)
                .map(pacs003 -> ApiResponse.success(pacs003,
                        "Successfully converted to interbank direct debit"))
                .doOnSuccess(response -> log.info("Interbank direct debit generated"));
    }

    @Operation(
            summary = "Generate direct debit return",
            description = """
                    Converts pacs.003 (FI to FI Customer Direct Debit) to pacs.004 (Payment Return).

                    Returns a direct debit when it cannot be processed or debtor disputes.

                    **Return Scenarios:**
                    - MD01: No mandate on file
                    - MD02: Mandate cancelled by debtor
                    - MD06: Disputed authorized transaction (customer claims unauthorized)
                    - MD07: Mandate invalid (details don't match)
                    - AM04: Insufficient funds
                    - AC04: Account closed
                    - AC06: Account blocked
                    - AM09: Amount exceeds mandate limit

                    **Return Windows:**
                    - Technical errors: Immediate
                    - No mandate: Within 2 business days
                    - Customer dispute: Up to 60 days

                    **Workflow:**
                    1. Receive pacs.003 direct debit
                    2. Validate mandate and account
                    3. If fails → Generate pacs.004 return
                    4. Send back to creditor bank
                    """
    )
    @PostMapping(value = "/pacs003-to-pacs004",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs004>> convertPacs003ToPacs004(
            @RequestBody Pacs003 pacs003,
            @Parameter(description = "Return reason code", example = "MD01")
            @RequestParam String returnReasonCode,
            @Parameter(description = "Return explanation")
            @RequestParam(required = false) String explanation) {

        log.info("Converting pacs.003 to pacs.004 - Return reason: {}", returnReasonCode);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("returnReasonCode", returnReasonCode);
        if (explanation != null) {
            context.setAttribute("returnExplanation", explanation);
        }

        return pacs003ToPacs004Converter.convert(pacs003, context)
                .map(pacs004 -> ApiResponse.success(pacs004,
                        "Successfully generated direct debit return"))
                .doOnSuccess(response -> log.info("Direct debit return generated"));
    }

    @Operation(
            summary = "Convert mandate setup to initial payment",
            description = """
                    Converts pain.009 (Mandate Initiation Request) to pacs.008 (FI to FI Customer Credit Transfer).

                    Handles scenario where mandate establishment triggers immediate initial payment.

                    **Use Cases:**
                    - Subscription setup with initial fee
                    - Membership signup with joining fee
                    - Insurance policy with first premium
                    - Loan setup with processing fee

                    **Workflow:**
                    1. Customer signs mandate
                    2. pain.009 submitted to establish mandate
                    3. Initial payment amount specified in context
                    4. Generate pacs.008 for setup fee
                    5. Process both mandate and payment

                    **Required Context:**
                    - initialPaymentAmount: Setup fee amount
                    - paymentPurpose: Description of initial payment

                    **Example:**
                    - Gym membership: $50 joining fee + mandate for monthly $30
                    - Magazine subscription: $10 setup + mandate for monthly $20
                    """
    )
    @PostMapping(value = "/pain009-to-pacs008",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs008>> convertPain009ToPacs008(
            @RequestBody Pain009 pain009,
            @Parameter(description = "Initial payment amount", example = "50.00", required = true)
            @RequestParam BigDecimal initialPaymentAmount,
            @Parameter(description = "Payment purpose", example = "Subscription setup fee")
            @RequestParam(required = false) String paymentPurpose) {

        log.info("Converting pain.009 to pacs.008 - Initial payment: {}", initialPaymentAmount);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("initialPaymentAmount", initialPaymentAmount);
        if (paymentPurpose != null) {
            context.setAttribute("paymentPurpose", paymentPurpose);
        }

        return pain009ToPacs008Converter.convert(pain009, context)
                .map(pacs008 -> ApiResponse.success(pacs008,
                        "Successfully generated initial payment from mandate setup"))
                .doOnSuccess(response -> log.info("Initial payment generated"));
    }

    @Operation(
            summary = "Convert activation request to status request",
            description = """
                    Converts pain.013 (Creditor Payment Activation Request) to pacs.028 (FI to FI Payment Status Request).

                    Checks payment status before activating/deactivating payment instructions.

                    **Purpose:**
                    - Verify payment status before activation changes
                    - Ensure no in-flight payments before suspension
                    - Validate safe state for reactivation

                    **Activation Actions:**
                    - ACTV: Activate suspended payment instruction
                    - CANC: Deactivate (suspend) active instruction

                    **Use Cases:**
                    - Customer pauses subscription (check no pending payments)
                    - Resume standing order (verify last payment status)
                    - Compliance audit (document current state)

                    **Workflow:**
                    1. Receive pain.013 activation request
                    2. Generate pacs.028 status query
                    3. Query FedNow for payment status
                    4. Receive pacs.002 status response
                    5. If safe → Proceed with activation/deactivation
                    6. Generate activation confirmation
                    """
    )
    @PostMapping(value = "/pain013-to-pacs028",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs028>> convertPain013ToPacs028(
            @RequestBody Pain013 pain013) {

        log.info("Converting pain.013 to pacs.028 - Activation status check");

        ConverterContext context = ConverterContext.builder().build();

        return pain013ToPacs028Converter.convert(pain013, context)
                .map(pacs028 -> ApiResponse.success(pacs028,
                        "Successfully generated status request for activation"))
                .doOnSuccess(response -> log.info("Status request generated"));
    }
}
