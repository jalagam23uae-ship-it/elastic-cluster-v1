package com.fednow.iso20022.api.controller;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.converter.phase1.*;
import com.fednow.iso20022.domain.camt.Camt054;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pacs.Pacs004;
import com.fednow.iso20022.domain.pacs.Pacs008;
import com.fednow.iso20022.domain.pain.Pain001;
import com.fednow.iso20022.domain.pain.Pain002;
import com.fednow.iso20022.domain.pain.Pain007;
import com.fednow.iso20022.domain.admi.Admi002;
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
 * REST API Controller for Phase 1 - Core Customer Payment Conversions.
 *
 * Endpoints for converting between customer and interbank payment messages.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/convert/payments")
@RequiredArgsConstructor
@Tag(name = "Phase 1: Customer Payments", description = "Core customer payment message conversions")
public class Phase1CustomerPaymentsController {

    private final CustomerCreditTransferToPacs008Converter pain001ToPacs008Converter;
    private final Pacs008ToPacs002Converter pacs008ToPacs002Converter;
    private final Pacs002ToPain002Converter pacs002ToPain002Converter;
    private final Pacs004ToPain007Converter pacs004ToPain007Converter;
    private final Pacs008ToCamt054Converter pacs008ToCamt054Converter;
    private final Pacs008ToAdmi002Converter pacs008ToAdmi002Converter;

    @Operation(
            summary = "Convert customer payment to interbank payment",
            description = """
                    Converts pain.001 (Customer Credit Transfer Initiation) to pacs.008 (FI to FI Customer Credit Transfer).

                    This conversion transforms a customer-initiated payment into an interbank FedNow payment message.

                    **Key Transformations:**
                    - Generates new message ID and UETR
                    - Adds FedNow settlement information (INDA, FDW)
                    - Enriches bank agent details
                    - Sets T+0 settlement date
                    - Preserves End-to-End ID for tracking

                    **Use Case:** Customer initiates payment → Bank converts to FedNow format → Send to network
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Conversion successful",
                    content = @Content(schema = @Schema(implementation = Pacs008.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid input or validation error"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @PostMapping(value = "/pain001-to-pacs008",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs008>> convertPain001ToPacs008(
            @Parameter(description = "Customer credit transfer initiation message", required = true)
            @RequestBody Pain001 pain001) {

        log.info("Converting pain.001 to pacs.008 - Message ID: {}",
                pain001.getGroupHeader().getMessageId());

        ConverterContext context = ConverterContext.builder().build();

        return pain001ToPacs008Converter.convert(pain001, context)
                .map(pacs008 -> ApiResponse.success(pacs008,
                        "Successfully converted pain.001 to pacs.008"))
                .doOnSuccess(response -> log.info("Conversion successful - UETR: {}",
                        response.getData().getCreditTransferTransactionInformation().get(0)
                                .getPaymentIdentification().getUetr()));
    }

    @Operation(
            summary = "Generate payment status report",
            description = """
                    Converts pacs.008 (FI to FI Customer Credit Transfer) to pacs.002 (Payment Status Report).

                    Receiving bank generates acceptance/rejection status after validating the payment.

                    **Status Determination:**
                    - ACCP: Payment accepted (all validations passed)
                    - RJCT: Payment rejected (validation failures, OFAC, fraud)
                    - PDNG: Payment pending (manual review required)
                    - PART: Partial acceptance (batch processing)

                    **Validation Checks:**
                    1. Account validation
                    2. Balance verification
                    3. OFAC screening
                    4. Fraud scoring
                    5. Business rules

                    **Use Case:** Receive pacs.008 → Validate → Generate pacs.002 status → Send back to sender
                    """
    )
    @PostMapping(value = "/pacs008-to-pacs002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs002>> convertPacs008ToPacs002(
            @RequestBody Pacs008 pacs008) {

        log.info("Converting pacs.008 to pacs.002 - Message ID: {}",
                pacs008.getGroupHeader().getMessageId());

        ConverterContext context = ConverterContext.builder().build();

        return pacs008ToPacs002Converter.convert(pacs008, context)
                .map(pacs002 -> ApiResponse.success(pacs002,
                        "Successfully generated payment status report"))
                .doOnSuccess(response -> log.info("Status report generated - Status: {}",
                        response.getData().getTransactionInformationAndStatus().get(0)
                                .getTransactionStatus()));
    }

    @Operation(
            summary = "Convert interbank status to customer status",
            description = """
                    Converts pacs.002 (FI to FI Payment Status Report) to pain.002 (Customer Payment Status Report).

                    Translates technical interbank status codes into customer-friendly messages.

                    **Message Translation:**
                    - AC01 → "Invalid account number provided"
                    - AC04 → "Account closed"
                    - AM04 → "Insufficient funds"
                    - AG01 → "Transaction forbidden (regulatory restriction)"

                    **Use Case:** Receive pacs.002 from FedNow → Translate to customer format → Notify customer
                    """
    )
    @PostMapping(value = "/pacs002-to-pain002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pain002>> convertPacs002ToPain002(
            @RequestBody Pacs002 pacs002) {

        log.info("Converting pacs.002 to pain.002 - Message ID: {}",
                pacs002.getGroupHeader().getMessageId());

        ConverterContext context = ConverterContext.builder().build();

        return pacs002ToPain002Converter.convert(pacs002, context)
                .map(pain002 -> ApiResponse.success(pain002,
                        "Successfully converted to customer payment status"))
                .doOnSuccess(response -> log.info("Customer status generated"));
    }

    @Operation(
            summary = "Convert payment return to customer reversal",
            description = """
                    Converts pacs.004 (Payment Return) to pain.007 (Customer Payment Reversal).

                    Notifies customer that their payment has been returned by the receiving bank.

                    **Return Reasons:**
                    - AC01: Account number incorrect
                    - AC04: Account closed
                    - AM04: Insufficient funds
                    - MD01: No mandate on file (direct debits)
                    - DUPL: Duplicate payment
                    - FRAD: Fraudulent transaction

                    **Use Case:** Receive pacs.004 return → Translate to customer format → Notify customer → Reverse accounting
                    """
    )
    @PostMapping(value = "/pacs004-to-pain007",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pain007>> convertPacs004ToPain007(
            @RequestBody Pacs004 pacs004) {

        log.info("Converting pacs.004 to pain.007 - Message ID: {}",
                pacs004.getGroupHeader().getMessageId());

        ConverterContext context = ConverterContext.builder().build();

        return pacs004ToPain007Converter.convert(pacs004, context)
                .map(pain007 -> ApiResponse.success(pain007,
                        "Successfully converted to customer payment reversal"))
                .doOnSuccess(response -> log.info("Customer reversal notification generated"));
    }

    @Operation(
            summary = "Generate account notification",
            description = """
                    Converts pacs.008 (FI to FI Customer Credit Transfer) to camt.054 (Bank to Customer Debit/Credit Notification).

                    Sends real-time notification to customer that a payment was received or sent.

                    **Notification Types:**
                    - CRDT: Credit notification (funds received)
                    - DBIT: Debit notification (funds sent)

                    **Bank Transaction Codes:**
                    - PMNT-RCDT: Received credit transfer
                    - PMNT-ICDT: Issued credit transfer

                    **Use Case:** Process pacs.008 → Update account → Send camt.054 real-time notification → Customer sees in banking app
                    """
    )
    @PostMapping(value = "/pacs008-to-camt054",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Camt054>> convertPacs008ToCamt054(
            @RequestBody Pacs008 pacs008,
            @Parameter(description = "Notification type: CRDT (credit) or DBIT (debit)", example = "CRDT")
            @RequestParam(defaultValue = "CRDT") String notificationType) {

        log.info("Converting pacs.008 to camt.054 - Notification type: {}", notificationType);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("notificationType", notificationType);

        return pacs008ToCamt054Converter.convert(pacs008, context)
                .map(camt054 -> ApiResponse.success(camt054,
                        "Successfully generated account notification"))
                .doOnSuccess(response -> log.info("Account notification generated"));
    }

    @Operation(
            summary = "Generate system event notification",
            description = """
                    Converts pacs.008 to admi.002 (System Event Notification) for technical issues.

                    Notifies sender of system-level problems that prevented payment processing.

                    **Event Codes:**
                    - AUTHF: Authentication failure
                    - ENCF: Encryption/signature failure
                    - SCHF: Schema validation failure
                    - SYSF: System failure (outage)
                    - NETF: Network failure
                    - CAPC: Capacity exceeded
                    - QHGH: Queue threshold high
                    - DEGR: Degraded service
                    - MAINT: Maintenance mode

                    **Use Case:** Technical problem occurs → Generate admi.002 → Send to originator → Log for operations
                    """
    )
    @PostMapping(value = "/pacs008-to-admi002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Admi002>> convertPacs008ToAdmi002(
            @RequestBody Pacs008 pacs008,
            @Parameter(description = "System event code", example = "SCHF")
            @RequestParam String eventCode,
            @Parameter(description = "Event description", example = "Schema validation failed")
            @RequestParam(required = false) String eventDescription) {

        log.info("Converting pacs.008 to admi.002 - Event code: {}", eventCode);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("eventCode", eventCode);
        if (eventDescription != null) {
            context.setAttribute("eventDescription", eventDescription);
        }

        return pacs008ToAdmi002Converter.convert(pacs008, context)
                .map(admi002 -> ApiResponse.success(admi002,
                        "Successfully generated system event notification"))
                .doOnSuccess(response -> log.info("System event notification generated"));
    }
}
