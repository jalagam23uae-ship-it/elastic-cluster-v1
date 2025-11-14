package com.fednow.iso20022.api.controller;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.converter.phase4.*;
import com.fednow.iso20022.domain.camt.Camt052;
import com.fednow.iso20022.domain.camt.Camt053;
import com.fednow.iso20022.domain.common.Amount;
import com.fednow.iso20022.domain.pacs.Pacs008;
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
import java.time.ZonedDateTime;
import java.util.List;

/**
 * REST API Controller for Phase 4 - Account Reporting & Reconciliation.
 *
 * Endpoints for generating account reports and statements from payment transactions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/convert/reporting")
@RequiredArgsConstructor
@Tag(name = "Phase 4: Account Reporting", description = "Account report and statement generation from payments")
public class Phase4ReportingController {

    private final Pacs008ToCamt052Converter pacs008ToCamt052Converter;
    private final MultiplePacs008ToCamt052Converter multiplePacs008ToCamt052Converter;
    private final MultiplePacs008ToCamt053Converter multiplePacs008ToCamt053Converter;

    @Operation(
            summary = "Generate account report from single payment",
            description = """
                    Converts pacs.008 (FI to FI Customer Credit Transfer) to camt.052 (Bank to Customer Account Report).

                    Generates real-time account activity report for a single payment transaction.

                    **Report Context:**
                    - **Intraday Reports**: Real-time updates throughout business day
                    - **Transaction Notifications**: Corporate banking portal updates
                    - **Cash Position**: Immediate balance visibility
                    - **Liquidity Management**: Real-time cash flow tracking

                    **Use Cases:**
                    - Treasury cash positioning
                    - Real-time payment reconciliation
                    - Instant payment visibility for customers
                    - Corporate banking dashboards

                    **Report Contains:**
                    - Credit/debit indicator (CRDT for received funds)
                    - Booking and value dates
                    - Complete transaction details
                    - Bank transaction codes (PMNT-RCDT-ESCT)
                    - Current account balance (if provided)

                    **Workflow:**
                    1. Process incoming pacs.008
                    2. Update account balance
                    3. Generate camt.052 report entry
                    4. Push to customer portal/API
                    5. Customer sees real-time update
                    """
    )
    @PostMapping(value = "/pacs008-to-camt052",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Camt052>> convertPacs008ToCamt052(
            @Parameter(description = "Payment transaction", required = true)
            @RequestBody Pacs008 pacs008,
            @Parameter(description = "Account number for report", required = true, example = "1234567890")
            @RequestParam String accountNumber,
            @Parameter(description = "Current account balance", example = "15000.00")
            @RequestParam(required = false) BigDecimal currentBalance) {

        log.info("Converting pacs.008 to camt.052 - Account: {}", accountNumber);

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("accountNumber", accountNumber);

        if (currentBalance != null) {
            context.setAttribute("currentBalance", Amount.builder()
                    .value(currentBalance)
                    .currency("USD")
                    .build());
        }

        return pacs008ToCamt052Converter.convert(pacs008, context)
                .map(camt052 -> ApiResponse.success(camt052,
                        "Successfully generated account report"))
                .doOnSuccess(response -> log.info("Account report generated for account: {}", accountNumber));
    }

    @Operation(
            summary = "Generate consolidated account report from multiple payments",
            description = """
                    Converts multiple pacs.008 messages to single camt.052 (Consolidated Account Report).

                    Aggregates multiple payment transactions into one consolidated report.

                    **Aggregation Context:**
                    - **Intraday Reports**: Hourly activity summaries
                    - **Batch Processing**: End-of-processing-window reports
                    - **Bulk Payments**: Payroll, merchant settlements
                    - **High Volume**: Multiple payments consolidated

                    **Report Features:**
                    - Multiple transaction entries
                    - Opening and closing balances
                    - Total credits calculation
                    - Chronological ordering

                    **Use Cases:**
                    1. Corporate Treasury: All incoming payments for cash positioning
                    2. Payment Hubs: Multi-channel payment consolidation
                    3. Reconciliation: Match against expected receipts
                    4. Bulk Payroll: All salary payments in one report
                    5. Merchant Acquiring: Aggregate settlement payments

                    **Workflow:**
                    1. Collect multiple pacs.008 payments
                    2. Determine report period
                    3. Calculate opening balance
                    4. Aggregate all transactions
                    5. Calculate closing balance
                    6. Generate consolidated camt.052
                    """
    )
    @PostMapping(value = "/multiple-pacs008-to-camt052",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Camt052>> convertMultiplePacs008ToCamt052(
            @Parameter(description = "List of payment transactions", required = true)
            @RequestBody List<Pacs008> payments,
            @Parameter(description = "Account number for report", required = true)
            @RequestParam String accountNumber,
            @Parameter(description = "Opening balance", example = "10000.00")
            @RequestParam(required = false) BigDecimal openingBalance,
            @Parameter(description = "Report from date/time")
            @RequestParam(required = false) ZonedDateTime reportFromTime,
            @Parameter(description = "Report to date/time")
            @RequestParam(required = false) ZonedDateTime reportToTime) {

        log.info("Converting {} pacs.008 messages to consolidated camt.052", payments.size());

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("accountNumber", accountNumber);

        if (openingBalance != null) {
            context.setAttribute("openingBalance", Amount.builder()
                    .value(openingBalance)
                    .currency("USD")
                    .build());
        }

        if (reportFromTime != null) {
            context.setAttribute("reportFromTime", reportFromTime);
        }

        if (reportToTime != null) {
            context.setAttribute("reportToTime", reportToTime);
        }

        return multiplePacs008ToCamt052Converter.convert(payments, context)
                .map(camt052 -> ApiResponse.success(camt052,
                        String.format("Successfully generated consolidated report with %d payments",
                                payments.size())))
                .doOnSuccess(response -> log.info("Consolidated report generated"));
    }

    @Operation(
            summary = "Generate official account statement from multiple payments",
            description = """
                    Converts multiple pacs.008 messages to camt.053 (Official Account Statement).

                    Generates official, legally binding account statement for periodic reporting.

                    **Statement Types:**
                    - **Daily**: End-of-day statement (DAIL)
                    - **Weekly**: Week-end statement (WEEK)
                    - **Monthly**: Month-end statement (MNTH)
                    - **Quarterly**: Quarter-end statement (QURT)
                    - **Annual**: Year-end statement (YEAR)

                    **Legal Status:**
                    - Official bank statement
                    - Auditable and legally binding
                    - Used for tax reporting
                    - Basis for dispute resolution
                    - Regulatory compliance

                    **Statement Features:**
                    - Sequential statement numbering
                    - Opening and closing balances (booked & available)
                    - Transaction summary (count, total credits, total debits)
                    - Complete transaction details
                    - Audit trail markers

                    **Difference from camt.052:**
                    - camt.053: Official STATEMENT (legal record, periodic)
                    - camt.052: Account REPORT (informational, can be intraday)

                    **Use Cases:**
                    1. Month-end reconciliation
                    2. Financial statement preparation
                    3. Audit and compliance
                    4. Tax reporting (IRS, etc.)
                    5. Legal proceedings evidence

                    **Workflow:**
                    1. End-of-period trigger (daily/monthly)
                    2. Collect all period transactions
                    3. Calculate opening/closing balances
                    4. Generate transaction summary
                    5. Assign sequential statement number
                    6. Create official camt.053
                    7. Archive for legal retention
                    """
    )
    @PostMapping(value = "/multiple-pacs008-to-camt053",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Camt053>> convertMultiplePacs008ToCamt053(
            @Parameter(description = "List of payment transactions", required = true)
            @RequestBody List<Pacs008> payments,
            @Parameter(description = "Account number for statement", required = true)
            @RequestParam String accountNumber,
            @Parameter(description = "Statement number (sequential)", example = "2024-001")
            @RequestParam(required = false) String statementNumber,
            @Parameter(description = "Statement frequency: DAIL, WEEK, MNTH, QURT, YEAR", example = "DAIL")
            @RequestParam(defaultValue = "DAIL") String frequency,
            @Parameter(description = "Opening booked balance", example = "10000.00")
            @RequestParam(required = false) BigDecimal openingBalance,
            @Parameter(description = "Opening available balance", example = "9500.00")
            @RequestParam(required = false) BigDecimal openingAvailableBalance,
            @Parameter(description = "Statement from date/time")
            @RequestParam(required = false) ZonedDateTime statementFromTime,
            @Parameter(description = "Statement to date/time")
            @RequestParam(required = false) ZonedDateTime statementToTime) {

        log.info("Converting {} pacs.008 messages to official camt.053 statement", payments.size());

        ConverterContext context = ConverterContext.builder().build();
        context.setAttribute("accountNumber", accountNumber);
        context.setAttribute("frequency", frequency);

        if (statementNumber != null) {
            context.setAttribute("statementNumber", statementNumber);
        }

        if (openingBalance != null) {
            context.setAttribute("openingBalance", Amount.builder()
                    .value(openingBalance)
                    .currency("USD")
                    .build());
        }

        if (openingAvailableBalance != null) {
            context.setAttribute("openingAvailableBalance", Amount.builder()
                    .value(openingAvailableBalance)
                    .currency("USD")
                    .build());
        }

        if (statementFromTime != null) {
            context.setAttribute("statementFromTime", statementFromTime);
        }

        if (statementToTime != null) {
            context.setAttribute("statementToTime", statementToTime);
        }

        return multiplePacs008ToCamt053Converter.convert(payments, context)
                .map(camt053 -> ApiResponse.success(camt053,
                        String.format("Successfully generated official statement #%s with %d payments",
                                statementNumber != null ? statementNumber : "AUTO",
                                payments.size())))
                .doOnSuccess(response -> log.info("Official statement generated"));
    }
}
