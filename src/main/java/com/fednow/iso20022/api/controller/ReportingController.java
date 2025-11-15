package com.fednow.iso20022.api.controller;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * REST API Controller for Analytics and Reporting.
 *
 * Provides endpoints for system health, converter performance, payment status,
 * validation errors, and comprehensive dashboards.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports & Analytics", description = "System health, analytics, and comprehensive reports")
public class ReportingController {

    private final ReportingService reportingService;

    @Operation(
            summary = "Get system health report",
            description = """
                    Retrieves comprehensive system health report including:
                    - Messages received, failed, rejected
                    - Fatal and error event counts
                    - Critical validation errors
                    - Health score (0-100) with status (HEALTHY, WARNING, CRITICAL)

                    **Use Case:** Monitor overall system health and identify issues
                    """
    )
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Map<String, Object>>> getSystemHealth() {
        log.info("Retrieving system health report");

        return reportingService.generateSystemHealthReport()
                .map(report -> ApiResponse.success(report, "System health report generated successfully"))
                .doOnSuccess(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> report = (Map<String, Object>) response.getData();
                    log.info("System health: score={}, status={}",
                            report.get("health_score"), report.get("status"));
                })
                .doOnError(e -> log.error("Failed to generate system health report", e));
    }

    @Operation(
            summary = "Get converter performance report",
            description = """
                    Retrieves performance metrics for a specific converter including:
                    - Total executions (successes and failures)
                    - Success rate percentage
                    - Execution time statistics

                    **Use Case:** Monitor converter performance and identify slow or failing converters
                    """
    )
    @GetMapping(value = "/converter/{converterName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Map<String, Object>>> getConverterPerformance(
            @Parameter(description = "Converter name (e.g., Pacs008ToPacs002Converter)", required = true)
            @PathVariable String converterName) {

        log.info("Retrieving performance report for converter: {}", converterName);

        return reportingService.generateConverterPerformanceReport(converterName)
                .map(report -> ApiResponse.success(report,
                        String.format("Performance report for %s generated successfully", converterName)))
                .doOnSuccess(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> report = (Map<String, Object>) response.getData();
                    log.info("Converter {} performance: executions={}, successRate={}%",
                            converterName, report.get("total_executions"), report.get("success_rate_percentage"));
                })
                .doOnError(e -> log.error("Failed to generate converter performance report for: {}", converterName, e));
    }

    @Operation(
            summary = "Get payment status distribution report",
            description = """
                    Retrieves payment status distribution across all payments:
                    - Accepted (ACCP), Settled (ACSC), Rejected (RJCT)
                    - Pending (PDNG), Partial (PART)
                    - Acceptance rate and rejection rate percentages

                    **Use Case:** Monitor payment processing success rates and identify rejection trends
                    """
    )
    @GetMapping(value = "/payment-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Map<String, Object>>> getPaymentStatusReport() {
        log.info("Retrieving payment status distribution report");

        return reportingService.generatePaymentStatusReport()
                .map(report -> ApiResponse.success(report, "Payment status report generated successfully"))
                .doOnSuccess(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> report = (Map<String, Object>) response.getData();
                    log.info("Payment status: total={}, acceptanceRate={}%, rejectionRate={}%",
                            report.get("total"), report.get("acceptance_rate"), report.get("rejection_rate"));
                })
                .doOnError(e -> log.error("Failed to generate payment status report", e));
    }

    @Operation(
            summary = "Get account activity report",
            description = """
                    Retrieves account activity report for a specific account and date range:
                    - Total debits and credits
                    - Net amount (credits - debits)
                    - Transaction count

                    **Use Case:** Generate account statements and transaction summaries
                    """
    )
    @GetMapping(value = "/account/{accountId}/activity", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Map<String, Object>>> getAccountActivity(
            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId,
            @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Retrieving account activity report: accountId={}, range={} to {}", accountId, startDate, endDate);

        return reportingService.generateAccountActivityReport(accountId, startDate, endDate)
                .map(report -> ApiResponse.success(report, "Account activity report generated successfully"))
                .doOnSuccess(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> report = (Map<String, Object>) response.getData();
                    log.info("Account {} activity: debits={}, credits={}, netAmount={}",
                            accountId, report.get("total_debits"), report.get("total_credits"), report.get("net_amount"));
                })
                .doOnError(e -> log.error("Failed to generate account activity report for: {}", accountId, e));
    }

    @Operation(
            summary = "Get validation error summary",
            description = """
                    Retrieves validation error summary across all messages:
                    - Count by severity (INFO, WARNING, ERROR, CRITICAL)
                    - Total error count

                    **Use Case:** Monitor validation quality and identify common validation issues
                    """
    )
    @GetMapping(value = "/validation-errors", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Map<String, Object>>> getValidationErrorReport() {
        log.info("Retrieving validation error summary report");

        return reportingService.generateValidationErrorReport()
                .map(report -> ApiResponse.success(report, "Validation error report generated successfully"))
                .doOnSuccess(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> report = (Map<String, Object>) response.getData();
                    log.info("Validation errors: total={}, critical={}",
                            report.get("total_errors"), report.get("critical_count"));
                })
                .doOnError(e -> log.error("Failed to generate validation error report", e));
    }

    @Operation(
            summary = "Get conversion analytics report",
            description = """
                    Retrieves conversion analytics across all converters:
                    - Total conversions (successful and failed)
                    - Success rate and failure rate percentages

                    **Use Case:** Monitor overall conversion success rates
                    """
    )
    @GetMapping(value = "/conversion-analytics", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Map<String, Object>>> getConversionAnalytics() {
        log.info("Retrieving conversion analytics report");

        return reportingService.generateConversionAnalyticsReport()
                .map(report -> ApiResponse.success(report, "Conversion analytics report generated successfully"))
                .doOnSuccess(response -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> report = (Map<String, Object>) response.getData();
                    log.info("Conversion analytics: total={}, successRate={}%",
                            report.get("total_conversions"), report.get("success_rate"));
                })
                .doOnError(e -> log.error("Failed to generate conversion analytics report", e));
    }

    @Operation(
            summary = "Get comprehensive dashboard report",
            description = """
                    Retrieves comprehensive dashboard combining all key reports:
                    - System health (health score, status, error counts)
                    - Payment status distribution
                    - Validation error summary
                    - Conversion analytics

                    **Use Case:** Executive dashboard with all key metrics in one call
                    """
    )
    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Map<String, Object>>> getDashboard() {
        log.info("Retrieving comprehensive dashboard report");

        return reportingService.generateDashboardReport()
                .map(report -> ApiResponse.success(report, "Dashboard report generated successfully"))
                .doOnSuccess(response -> log.info("Dashboard report generated with all key metrics"))
                .doOnError(e -> log.error("Failed to generate dashboard report", e));
    }
}
