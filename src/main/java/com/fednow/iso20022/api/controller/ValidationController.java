package com.fednow.iso20022.api.controller;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.entity.ValidationError;
import com.fednow.iso20022.service.ValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * REST API Controller for Validation Error Management and Queries.
 *
 * Provides endpoints for:
 * - Querying validation errors by message, severity, category
 * - Checking validation status
 * - Retrieving critical errors
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/validation")
@RequiredArgsConstructor
@Tag(name = "Validation", description = "Validation error management and queries")
public class ValidationController {

    private final ValidationService validationService;

    @Operation(
            summary = "Get all validation errors for a message",
            description = """
                    Retrieves all validation errors for a specific message:
                    - Error code, category, severity
                    - Field path and error description
                    - Timestamp

                    **Use Case:** Review all validation issues for a message
                    """
    )
    @GetMapping(value = "/errors/message/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<ValidationError>>> getValidationErrors(
            @Parameter(description = "Message ID", required = true)
            @PathVariable UUID messageId) {

        log.info("Retrieving validation errors for message: {}", messageId);

        return validationService.getValidationErrors(messageId)
                .collectList()
                .map(errors -> ApiResponse.success(errors,
                        String.format("Found %d validation errors for message %s", errors.size(), messageId)))
                .doOnSuccess(response -> log.info("Found {} validation errors for message: {}",
                        response.getData().size(), messageId))
                .doOnError(e -> log.error("Failed to retrieve validation errors for: {}", messageId, e));
    }

    @Operation(
            summary = "Get critical errors for a message",
            description = """
                    Retrieves only CRITICAL severity validation errors for a message:
                    - Errors that require immediate rejection
                    - Highest priority issues

                    **Use Case:** Identify critical validation failures
                    """
    )
    @GetMapping(value = "/errors/message/{messageId}/critical", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<ValidationError>>> getCriticalErrors(
            @Parameter(description = "Message ID", required = true)
            @PathVariable UUID messageId) {

        log.info("Retrieving critical errors for message: {}", messageId);

        return validationService.getCriticalErrors(messageId)
                .collectList()
                .map(errors -> ApiResponse.success(errors,
                        String.format("Found %d critical errors for message %s", errors.size(), messageId)))
                .doOnSuccess(response -> {
                    if (!response.getData().isEmpty()) {
                        log.warn("Found {} critical validation errors for message: {}",
                                response.getData().size(), messageId);
                    }
                })
                .doOnError(e -> log.error("Failed to retrieve critical errors for: {}", messageId, e));
    }

    @Operation(
            summary = "Get all system-wide critical errors",
            description = """
                    Retrieves all CRITICAL validation errors across all messages:
                    - System-wide critical validation issues
                    - Requires immediate attention

                    **Use Case:** Monitor all critical validation failures for alerting
                    """
    )
    @GetMapping(value = "/errors/critical", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<ValidationError>>> getAllCriticalErrors() {
        log.info("Retrieving all system-wide critical errors");

        return validationService.getAllCriticalErrors()
                .collectList()
                .map(errors -> ApiResponse.success(errors,
                        String.format("Found %d system-wide critical errors", errors.size())))
                .doOnSuccess(response -> {
                    if (!response.getData().isEmpty()) {
                        log.warn("Found {} system-wide critical validation errors", response.getData().size());
                    }
                })
                .doOnError(e -> log.error("Failed to retrieve system-wide critical errors", e));
    }

    @Operation(
            summary = "Check if message has critical errors",
            description = """
                    Checks if a message has any critical validation errors:
                    - Returns true if critical errors exist
                    - Returns false if no critical errors

                    **Use Case:** Quick validation check before processing
                    """
    )
    @GetMapping(value = "/errors/message/{messageId}/has-critical", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Boolean>> hasCriticalErrors(
            @Parameter(description = "Message ID", required = true)
            @PathVariable UUID messageId) {

        log.debug("Checking for critical errors on message: {}", messageId);

        return validationService.hasCriticalErrors(messageId)
                .map(hasCritical -> ApiResponse.success(hasCritical,
                        hasCritical
                                ? String.format("Message %s has critical errors", messageId)
                                : String.format("Message %s has no critical errors", messageId)))
                .doOnSuccess(response -> {
                    if (response.getData()) {
                        log.warn("Message {} has critical validation errors", messageId);
                    }
                })
                .doOnError(e -> log.error("Failed to check critical errors for: {}", messageId, e));
    }

    @Operation(
            summary = "Check if message is valid",
            description = """
                    Checks if a message passed validation (no errors):
                    - Returns true if message has no validation errors
                    - Returns false if any validation errors exist

                    **Use Case:** Pre-conversion validation check
                    """
    )
    @GetMapping(value = "/message/{messageId}/is-valid", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<Boolean>> isValid(
            @Parameter(description = "Message ID", required = true)
            @PathVariable UUID messageId) {

        log.debug("Checking if message is valid: {}", messageId);

        return validationService.isValid(messageId)
                .map(isValid -> ApiResponse.success(isValid,
                        isValid
                                ? String.format("Message %s is valid", messageId)
                                : String.format("Message %s has validation errors", messageId)))
                .doOnSuccess(response -> {
                    if (!response.getData()) {
                        log.info("Message {} has validation errors", messageId);
                    }
                })
                .doOnError(e -> log.error("Failed to check if message is valid: {}", messageId, e));
    }

    @Operation(
            summary = "Get errors by severity",
            description = """
                    Retrieves validation errors by severity level:
                    - Severity levels: INFO, WARNING, ERROR, CRITICAL
                    - Filtered across all messages

                    **Use Case:** Monitor validation errors by severity
                    """
    )
    @GetMapping(value = "/errors/severity/{severity}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<ValidationError>>> getErrorsBySeverity(
            @Parameter(description = "Severity (INFO, WARNING, ERROR, CRITICAL)", required = true)
            @PathVariable String severity) {

        log.info("Retrieving validation errors by severity: {}", severity);

        return validationService.getErrorsBySeverity(severity)
                .collectList()
                .map(errors -> ApiResponse.success(errors,
                        String.format("Found %d validation errors with severity %s", errors.size(), severity)))
                .doOnSuccess(response -> log.info("Found {} validation errors with severity: {}",
                        response.getData().size(), severity))
                .doOnError(e -> log.error("Failed to retrieve errors by severity: {}", severity, e));
    }

    @Operation(
            summary = "Get errors by category",
            description = """
                    Retrieves validation errors by category:
                    - Categories: ACCOUNT, AMOUNT, OFAC, FRAUD, SCHEMA, etc.
                    - Filtered across all messages

                    **Use Case:** Analyze validation errors by type
                    """
    )
    @GetMapping(value = "/errors/category/{category}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<List<ValidationError>>> getErrorsByCategory(
            @Parameter(description = "Category (ACCOUNT, AMOUNT, OFAC, FRAUD, SCHEMA)", required = true)
            @PathVariable String category) {

        log.info("Retrieving validation errors by category: {}", category);

        return validationService.getErrorsByCategory(category)
                .collectList()
                .map(errors -> ApiResponse.success(errors,
                        String.format("Found %d validation errors in category %s", errors.size(), category)))
                .doOnSuccess(response -> log.info("Found {} validation errors in category: {}",
                        response.getData().size(), category))
                .doOnError(e -> log.error("Failed to retrieve errors by category: {}", category, e));
    }

    @Operation(
            summary = "Record a validation error",
            description = """
                    Records a single validation error for a message:
                    - Captures error code, category, severity
                    - Stores field path and description
                    - Timestamps the error

                    **Use Case:** Manually record validation errors during processing
                    """
    )
    @PostMapping(value = "/errors", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ApiResponse<ValidationError>> recordValidationError(
            @Parameter(description = "Validation error details", required = true)
            @RequestBody ValidationErrorRequest request) {

        log.info("Recording validation error for message: {}, code: {}", request.messageId, request.errorCode);

        return validationService.recordValidationError(
                        request.messageId,
                        request.errorCode,
                        request.errorCategory,
                        request.severity,
                        request.fieldPath,
                        request.errorDescription
                )
                .map(error -> ApiResponse.success(error,
                        String.format("Validation error recorded: %s", error.getErrorCode())))
                .doOnSuccess(response -> log.info("Validation error recorded: id={}, code={}",
                        response.getData().getId(), response.getData().getErrorCode()))
                .doOnError(e -> log.error("Failed to record validation error", e));
    }

    /**
     * Request DTO for recording validation errors.
     */
    public record ValidationErrorRequest(
            UUID messageId,
            String errorCode,
            String errorCategory,
            String severity,
            String fieldPath,
            String errorDescription
    ) {}
}
