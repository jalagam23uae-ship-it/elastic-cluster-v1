package com.fednow.iso20022.api.controller;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.entity.Conversion;
import com.fednow.iso20022.entity.ConverterMetrics;
import com.fednow.iso20022.service.ConverterOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST API Controller for Converter Metrics and Performance Monitoring.
 *
 * Provides endpoints for:
 * - Converter metrics queries
 * - Conversion history tracking
 * - Performance analytics
 * - Failure analysis
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Tag(name = "Metrics & Performance", description = "Converter metrics, performance monitoring, and analytics")
public class MetricsController {

    private final ConverterOrchestrationService orchestrationService;

    @Operation(
            summary = "Get converter metrics for a specific date",
            description = """
                    Retrieves detailed metrics for a converter on a specific date:
                    - Execution count (total, success, failure)
                    - Execution times (min, max, average)
                    - Last execution timestamp

                    **Use Case:** Track daily converter performance
                    """
    )
    @GetMapping(value = "/converter/{converterName}/date/{date}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<ConverterMetrics>> getConverterMetrics(
            @Parameter(description = "Converter name (e.g., Pacs008ToPacs002Converter)", required = true)
            @PathVariable String converterName,
            @Parameter(description = "Date (YYYY-MM-DD)", required = true)
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Retrieving metrics for converter: {}, date: {}", converterName, date);

        return orchestrationService.getConverterMetrics(converterName, date)
                .map(metrics -> ApiResponse.success(metrics,
                        String.format("Metrics for %s on %s retrieved successfully", converterName, date)))
                .switchIfEmpty(Mono.just(ApiResponse.success(null,
                        String.format("No metrics found for %s on %s", converterName, date))))
                .doOnSuccess(response -> {
                    if (response.getData() != null) {
                        log.info("Converter {} metrics for {}: executions={}, successRate={}%",
                                converterName, date,
                                response.getData().getExecutionCount(),
                                calculateSuccessRate(response.getData()));
                    }
                })
                .doOnError(e -> log.error("Failed to retrieve metrics for: {}, date: {}", converterName, date, e));
    }

    @Operation(
            summary = "Get converter metrics for a date range",
            description = """
                    Retrieves metrics for a converter across a date range:
                    - Daily breakdown of executions
                    - Performance trends over time

                    **Use Case:** Analyze converter performance trends
                    """
    )
    @GetMapping(value = "/converter/{converterName}/range", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<ConverterMetrics>>> getConverterMetricsRange(
            @Parameter(description = "Converter name (e.g., Pacs008ToPacs002Converter)", required = true)
            @PathVariable String converterName,
            @Parameter(description = "Start date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (YYYY-MM-DD)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Retrieving metrics range for converter: {}, from {} to {}", converterName, startDate, endDate);

        return orchestrationService.getConverterMetricsRange(converterName, startDate, endDate)
                .collectList()
                .map(metrics -> ApiResponse.success(metrics,
                        String.format("Metrics for %s from %s to %s retrieved successfully",
                                converterName, startDate, endDate)))
                .doOnSuccess(response -> log.info("Retrieved {} metrics records for converter: {}",
                        response.getData().size(), converterName))
                .doOnError(e -> log.error("Failed to retrieve metrics range for: {}", converterName, e));
    }

    @Operation(
            summary = "Get conversion history for a message",
            description = """
                    Retrieves all conversions for a specific source message:
                    - Conversion timestamps
                    - Target messages generated
                    - Execution times
                    - Success/failure status

                    **Use Case:** Track message conversion lifecycle
                    """
    )
    @GetMapping(value = "/conversions/message/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<Conversion>>> getConversionHistory(
            @Parameter(description = "Source message ID", required = true)
            @PathVariable UUID messageId) {

        log.info("Retrieving conversion history for message: {}", messageId);

        return orchestrationService.getConversionHistory(messageId)
                .collectList()
                .map(conversions -> ApiResponse.success(conversions,
                        String.format("Conversion history for message %s retrieved successfully", messageId)))
                .doOnSuccess(response -> log.info("Found {} conversions for message: {}", response.getData().size(), messageId))
                .doOnError(e -> log.error("Failed to retrieve conversion history for: {}", messageId, e));
    }

    @Operation(
            summary = "Get recent failed conversions",
            description = """
                    Retrieves recent failed conversions for debugging:
                    - Converter name
                    - Error messages
                    - Execution times
                    - Timestamps

                    **Use Case:** Debug conversion failures and identify patterns
                    """
    )
    @GetMapping(value = "/conversions/failed", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<Conversion>>> getRecentFailedConversions(
            @Parameter(description = "Maximum number of failures to retrieve (default: 50)")
            @RequestParam(defaultValue = "50") int limit) {

        log.info("Retrieving recent failed conversions: limit={}", limit);

        return orchestrationService.getRecentFailedConversions(limit)
                .collectList()
                .map(conversions -> ApiResponse.success(conversions,
                        String.format("Retrieved %d recent failed conversions", conversions.size())))
                .doOnSuccess(response -> log.info("Found {} recent failed conversions", response.getData().size()))
                .doOnError(e -> log.error("Failed to retrieve recent failed conversions", e));
    }

    @Operation(
            summary = "Get converter success rate",
            description = """
                    Calculates the overall success rate for a converter:
                    - Percentage of successful conversions
                    - Based on all-time execution data

                    **Use Case:** Monitor converter reliability
                    """
    )
    @GetMapping(value = "/converter/{converterName}/success-rate", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Double>> getConverterSuccessRate(
            @Parameter(description = "Converter name (e.g., Pacs008ToPacs002Converter)", required = true)
            @PathVariable String converterName) {

        log.info("Calculating success rate for converter: {}", converterName);

        return orchestrationService.getConverterSuccessRate(converterName)
                .map(successRate -> ApiResponse.success(successRate,
                        String.format("Success rate for %s: %.2f%%", converterName, successRate)))
                .doOnSuccess(response -> log.info("Converter {} success rate: {:.2f}%", converterName, response.getData()))
                .doOnError(e -> log.error("Failed to calculate success rate for: {}", converterName, e));
    }

    @Operation(
            summary = "Get total executions for a converter",
            description = """
                    Retrieves the total number of executions for a converter:
                    - All-time execution count
                    - Includes both successful and failed conversions

                    **Use Case:** Track converter usage volume
                    """
    )
    @GetMapping(value = "/converter/{converterName}/executions", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Long>> getTotalExecutions(
            @Parameter(description = "Converter name (e.g., Pacs008ToPacs002Converter)", required = true)
            @PathVariable String converterName) {

        log.info("Retrieving total executions for converter: {}", converterName);

        return orchestrationService.getTotalExecutions(converterName)
                .map(count -> ApiResponse.success(count,
                        String.format("Total executions for %s: %d", converterName, count)))
                .doOnSuccess(response -> log.info("Converter {} total executions: {}", converterName, response.getData()))
                .doOnError(e -> log.error("Failed to retrieve total executions for: {}", converterName, e));
    }

    @Operation(
            summary = "Get problematic converters",
            description = """
                    Identifies converters with high failure rates:
                    - Converters exceeding the specified failure threshold
                    - Sorted by failure rate (highest first)

                    **Use Case:** Identify converters requiring attention or debugging
                    """
    )
    @GetMapping(value = "/converters/problematic", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<ConverterMetrics>>> getProblematicConverters(
            @Parameter(description = "Failure rate threshold (0.0-1.0, default: 0.1 = 10%)")
            @RequestParam(defaultValue = "0.1") double failureThreshold) {

        log.info("Retrieving problematic converters with failure rate > {}", failureThreshold);

        return orchestrationService.getProblematicConverters(failureThreshold)
                .collectList()
                .map(metrics -> ApiResponse.success(metrics,
                        String.format("Found %d converters with failure rate > %.1f%%",
                                metrics.size(), failureThreshold * 100)))
                .doOnSuccess(response -> {
                    if (!response.getData().isEmpty()) {
                        log.warn("Found {} problematic converters with failure rate > {:.1f}%",
                                response.getData().size(), failureThreshold * 100);
                    } else {
                        log.info("No problematic converters found");
                    }
                })
                .doOnError(e -> log.error("Failed to retrieve problematic converters", e));
    }

    /**
     * Helper method to calculate success rate from metrics.
     */
    private double calculateSuccessRate(ConverterMetrics metrics) {
        if (metrics.getExecutionCount() == 0) {
            return 0.0;
        }
        return (metrics.getSuccessCount() * 100.0) / metrics.getExecutionCount();
    }
}
