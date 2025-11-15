package com.fednow.iso20022.api.controller;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.converter.phase1.*;
import com.fednow.iso20022.domain.camt.Camt054;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pacs.Pacs004;
import com.fednow.iso20022.domain.pacs.Pacs007;
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

    // Phase 1 Critical Operations Converters (NEW)
    private final Pacs008ToPacs004Converter pacs008ToPacs004Converter;
    private final Pacs004ToPacs002Converter pacs004ToPacs002Converter;
    private final Pacs007ToPacs002Converter pacs007ToPacs002Converter;
    private final AnyMessageToAdmi002Converter anyMessageToAdmi002Converter;
    private final AuthFailureToAdmi002Converter authFailureToAdmi002Converter;
    private final SchemaFailureToAdmi002Converter schemaFailureToAdmi002Converter;

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

    // ==================== NEW PHASE 1 CRITICAL OPERATIONS ENDPOINTS ====================

    @Operation(
            summary = "Convert payment to return",
            description = """
                    Converts pacs.008 (FI to FI Customer Credit Transfer) to pacs.004 (Payment Return).

                    Creditor agent initiates return when unable to credit the beneficiary account.

                    **Return Scenarios:**
                    - AC01: Incorrect account number
                    - AC04: Account closed
                    - AC06: Account blocked
                    - FRAD: Fraudulent payment detected
                    - DUPL: Duplicate payment received
                    - CUST: Customer refused payment

                    **Key Features:**
                    - Reverses agent roles (creditor initiates)
                    - Preserves all original transaction IDs and UETR
                    - Generates unique return ID
                    - Includes return reason codes

                    **Use Case:** Creditor bank receives payment → Cannot credit customer → Generate return → Send back to debtor bank
                    """
    )
    @PostMapping(value = "/pacs008-to-pacs004",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs004>> convertPacs008ToPacs004(
            @RequestBody Pacs008 pacs008,
            @Parameter(description = "Return reason code (AC01, AC04, AC06, FRAD, DUPL, CUST)", example = "AC01")
            @RequestParam(defaultValue = "AC01") String returnReasonCode,
            @Parameter(description = "Return explanation", example = "Incorrect account number")
            @RequestParam(required = false) String returnExplanation) {

        log.info("Converting pacs.008 to pacs.004 (Payment Return) - Reason: {}", returnReasonCode);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("returnReasonCode", returnReasonCode);
        if (returnExplanation != null) {
            context.setAttribute("returnExplanation", returnExplanation);
        }

        return pacs008ToPacs004Converter.convert(pacs008, context)
                .map(pacs004 -> ApiResponse.success(pacs004,
                        "Successfully generated payment return"))
                .doOnSuccess(response -> log.info("Payment return generated - Return ID: {}",
                        response.getData().getTransactionInformation().get(0).getReturnId()));
    }

    @Operation(
            summary = "Acknowledge payment return",
            description = """
                    Converts pacs.004 (Payment Return) to pacs.002 (Payment Status Report).

                    Debtor agent acknowledges the return with acceptance or rejection status.

                    **Status Codes:**
                    - ACCP: Return accepted - funds will be returned to debtor
                    - ACSC: Return accepted, settled, and credited to debtor
                    - RJCT: Return rejected (invalid reason, timing, no original found)
                    - PDNG: Return pending manual review

                    **Rejection Reasons:**
                    - NOAS: No answer from beneficiary
                    - LEGL: Legal decision to reject
                    - NOOR: No original transaction reference
                    - FF01: Invalid format or reference

                    **Response Time:** <5 seconds (FedNow requirement)

                    **Use Case:** Debtor bank receives pacs.004 return → Validate → Accept/Reject → Send pacs.002 acknowledgment
                    """
    )
    @PostMapping(value = "/pacs004-to-pacs002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs002>> convertPacs004ToPacs002(
            @RequestBody Pacs004 pacs004) {

        log.info("Converting pacs.004 to pacs.002 (Return Acknowledgment) - Message ID: {}",
                pacs004.getGroupHeader().getMessageId());

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("returnValid", true); // Default to accepting the return

        return pacs004ToPacs002Converter.convert(pacs004, context)
                .map(pacs002 -> ApiResponse.success(pacs002,
                        "Successfully generated return acknowledgment"))
                .doOnSuccess(response -> log.info("Return acknowledgment generated - Status: {}",
                        response.getData().getTransactionInformationAndStatus().get(0)
                                .getTransactionStatus()));
    }

    @Operation(
            summary = "Acknowledge payment reversal",
            description = """
                    Converts pacs.007 (FI to FI Payment Reversal) to pacs.002 (Payment Status Report).

                    Creditor agent acknowledges the reversal request from debtor agent.

                    **FedNow CRITICAL Requirements:**
                    - Reversals must be initiated within 15 seconds of original payment
                    - Must respond within 5 seconds
                    - Cannot reverse settled payments
                    - UETR tracking maintained

                    **Status Codes:**
                    - ACCP: Reversal accepted
                    - ACSC: Reversal accepted and processed immediately
                    - RJCT: Reversal rejected (timing expired, already settled)
                    - PDNG: Reversal pending manual review

                    **Rejection Reasons:**
                    - TM01: Cut-off time (15-second window expired)
                    - LEGL: Legal decision (payment settled, cannot reverse)
                    - NOOR: No original transaction reference

                    **Use Case:** Debtor bank sends pacs.007 reversal → Creditor bank validates timing → Accept/Reject → Send pacs.002
                    """
    )
    @PostMapping(value = "/pacs007-to-pacs002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Pacs002>> convertPacs007ToPacs002(
            @RequestBody Pacs007 pacs007) {

        log.info("Converting pacs.007 to pacs.002 (Reversal Acknowledgment) - Message ID: {}",
                pacs007.getGroupHeader().getMessageId());

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("reversalValid", true);
        context.setAttribute("reversalTimingValid", true); // Validate timing in real implementation

        return pacs007ToPacs002Converter.convert(pacs007, context)
                .map(pacs002 -> ApiResponse.success(pacs002,
                        "Successfully generated reversal acknowledgment"))
                .doOnSuccess(response -> log.info("Reversal acknowledgment generated - Status: {}",
                        response.getData().getTransactionInformationAndStatus().get(0)
                                .getTransactionStatus()));
    }

    @Operation(
            summary = "Generate system error notification",
            description = """
                    Converts any ISO 20022 message to admi.002 (System Event Notification) for generic errors.

                    Handles unexpected system errors during message processing.

                    **Error Categories:**
                    - SYSF: System Failure (database down, service unavailable)
                    - NETF: Network Failure (connection timeout, network down)
                    - CONF: Configuration Failure (missing config, invalid setup)
                    - RESF: Resource Failure (out of memory, disk full, CPU)
                    - DBNF: Database Failure (connection pool exhausted, query timeout)
                    - UNKN: Unknown Error (unexpected exceptions)

                    **Response Time:** <1 second for critical events

                    **Use Case:** Unexpected error during processing → Extract context → Generate admi.002 → Send to originator → Log for ops
                    """
    )
    @PostMapping(value = "/any-to-admi002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Admi002>> convertAnyToAdmi002(
            @RequestBody Object message,
            @Parameter(description = "Error code (SYSF, NETF, CONF, RESF, DBNF, UNKN)", example = "SYSF")
            @RequestParam String errorCode,
            @Parameter(description = "Error message", example = "Database connection failed")
            @RequestParam String errorMessage,
            @Parameter(description = "Affected component", example = "Database")
            @RequestParam(required = false, defaultValue = "System") String errorComponent) {

        log.error("Generating generic error notification - Error Code: {}, Message: {}", errorCode, errorMessage);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("errorCode", errorCode);
        context.setAttribute("errorMessage", errorMessage);
        context.setAttribute("errorComponent", errorComponent);
        context.setAttribute("errorSeverity", "FATAL");

        return anyMessageToAdmi002Converter.convert(message, context)
                .map(admi002 -> ApiResponse.success(admi002,
                        "Successfully generated system error notification"))
                .doOnSuccess(response -> log.info("System error notification generated - Code: {}", errorCode));
    }

    @Operation(
            summary = "Generate security event notification",
            description = """
                    Converts authentication/security failures to admi.002 (System Event Notification).

                    Handles authentication, authorization, and security-related failures.

                    **Security Event Types:**
                    - AUTHF: Authentication Failure (invalid credentials, expired certificates)
                    - ENCF: Encryption Failure (TLS handshake, decryption errors)
                    - AUTZ: Authorization Failure (insufficient permissions)
                    - SECV: Security Violation (suspicious activity, rate limiting)
                    - CERT: Certificate Issues (expired, invalid)
                    - SIGN: Signature Verification Failure

                    **Severity Escalation:**
                    - 1-2 failures: WARNING
                    - 3-4 failures: ERROR
                    - 5+ failures: FATAL (triggers account lockout)

                    **Compliance:** PCI DSS, Federal banking standards, FedNow security guidelines

                    **Use Case:** Invalid login attempt → Track failures → Generate admi.002 → Lock account if threshold reached → Alert security team
                    """
    )
    @PostMapping(value = "/auth-failure-to-admi002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Admi002>> convertAuthFailureToAdmi002(
            @RequestBody Object message,
            @Parameter(description = "Security event type (AUTHF, ENCF, AUTZ, SECV, CERT, SIGN)", example = "AUTHF")
            @RequestParam String securityEventType,
            @Parameter(description = "Failure reason", example = "Invalid credentials")
            @RequestParam String failureReason,
            @Parameter(description = "Username", example = "john.doe@bank.com")
            @RequestParam(required = false) String username,
            @Parameter(description = "IP address", example = "192.168.1.100")
            @RequestParam(required = false) String ipAddress,
            @Parameter(description = "Consecutive failure count", example = "1")
            @RequestParam(required = false, defaultValue = "1") Integer failureCount) {

        log.warn("Generating security event notification - Event: {}, Reason: {}, User: {}, IP: {}",
                securityEventType, failureReason, username, ipAddress);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("securityEventType", securityEventType);
        context.setAttribute("failureReason", failureReason);
        context.setAttribute("affectedComponent", "Authentication");
        if (username != null) context.setAttribute("username", username);
        if (ipAddress != null) context.setAttribute("ipAddress", ipAddress);
        if (failureCount != null) context.setAttribute("failureCount", failureCount);

        return authFailureToAdmi002Converter.convert(message, context)
                .map(admi002 -> ApiResponse.success(admi002,
                        "Successfully generated security event notification"))
                .doOnSuccess(response -> log.info("Security event notification generated - Event: {}", securityEventType));
    }

    @Operation(
            summary = "Generate schema validation error notification",
            description = """
                    Converts schema validation failures to admi.002 (System Event Notification).

                    Handles XML/JSON schema validation failures and format errors.

                    **Validation Failure Types:**
                    - SCHF: Schema Validation (XSD validation failures)
                    - FMTF: Format Validation (date/time, currency, BIC, IBAN)
                    - STRF: Structural Validation (invalid XML/JSON structure)
                    - ENCF: Character Encoding (invalid UTF-8)
                    - MISS: Missing Required Fields
                    - LENG: Field Length Violations

                    **Features:**
                    - Detailed field-level error information
                    - Line/column number tracking for XML errors
                    - Multiple validation error aggregation
                    - Pattern violation detection

                    **Common Errors:**
                    - Missing messageId (required field)
                    - Invalid BIC code format (must be 8 or 11 chars)
                    - Invalid currency code (must be 3-letter ISO code)
                    - Field length violations (messageId max 35 characters)

                    **Response Time:** <1 second with detailed error information

                    **Use Case:** Receive malformed XML → Schema validation fails → Generate admi.002 with line/column → Send to sender
                    """
    )
    @PostMapping(value = "/schema-failure-to-admi002",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Admi002>> convertSchemaFailureToAdmi002(
            @RequestBody Object message,
            @Parameter(description = "Validation type (SCHF, FMTF, STRF, ENCF, MISS, LENG)", example = "SCHF")
            @RequestParam String validationType,
            @Parameter(description = "Validation message", example = "Invalid XML structure")
            @RequestParam String validationMessage,
            @Parameter(description = "Message type", example = "pacs.008.001.11")
            @RequestParam String messageType,
            @Parameter(description = "Field name that failed validation", example = "GroupHeader.MessageId")
            @RequestParam(required = false) String fieldName,
            @Parameter(description = "Field value that failed", example = "INVALID_VALUE")
            @RequestParam(required = false) String fieldValue) {

        log.error("Generating schema validation error notification - Type: {}, Message: {}, Field: {}",
                validationType, validationMessage, fieldName);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("validationType", validationType);
        context.setAttribute("validationMessage", validationMessage);
        context.setAttribute("messageType", messageType);
        if (fieldName != null) context.setAttribute("fieldName", fieldName);
        if (fieldValue != null) context.setAttribute("fieldValue", fieldValue);

        return schemaFailureToAdmi002Converter.convert(message, context)
                .map(admi002 -> ApiResponse.success(admi002,
                        "Successfully generated schema validation error notification"))
                .doOnSuccess(response -> log.info("Schema validation error notification generated - Type: {}", validationType));
    }
}
