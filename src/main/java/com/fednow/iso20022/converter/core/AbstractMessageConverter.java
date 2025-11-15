package com.fednow.iso20022.converter.core;

import com.fednow.iso20022.converter.utils.IdGenerator;
import com.fednow.iso20022.service.ConverterOrchestrationService;
import com.fednow.iso20022.service.NotificationService;
import com.fednow.iso20022.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Abstract base class for message converters with service layer integration.
 *
 * Provides common functionality:
 * - Validation with ValidationService integration
 * - Logging
 * - Metrics tracking with ConverterOrchestrationService
 * - Error handling with NotificationService
 * - ID generation
 *
 * Service Integration (Week 3-4):
 * - ConverterOrchestrationService: Tracks conversion metrics (success/failure, execution time)
 * - ValidationService: Records validation errors in database
 * - NotificationService: Records system events for errors and critical issues
 *
 * @param <S> Source message type
 * @param <T> Target message type
 */
@Slf4j
public abstract class AbstractMessageConverter<S, T> implements MessageConverter<S, T> {

    @Autowired
    protected IdGenerator idGenerator;

    // Service layer integration (optional dependencies - converters work without services)
    @Autowired(required = false)
    protected ConverterOrchestrationService orchestrationService;

    @Autowired(required = false)
    protected ValidationService validationService;

    @Autowired(required = false)
    protected NotificationService notificationService;

    private final Class<S> sourceType;
    private final Class<T> targetType;
    private final String converterName;

    /**
     * Constructor requiring message types.
     *
     * @param sourceType source message class
     * @param targetType target message class
     * @param converterName converter name for logging
     */
    protected AbstractMessageConverter(Class<S> sourceType, Class<T> targetType, String converterName) {
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.converterName = converterName;
    }

    @Override
    public Class<S> getSourceType() {
        return sourceType;
    }

    @Override
    public Class<T> getTargetType() {
        return targetType;
    }

    @Override
    public String getConverterName() {
        return converterName;
    }

    @Override
    public Mono<T> convert(S source, ConverterContext context) {
        log.debug("Starting conversion: {} -> {}", sourceType.getSimpleName(), targetType.getSimpleName());

        Instant startTime = Instant.now();
        UUID sourceMessageId = context.getSourceMessageId();
        UUID targetMessageId = context.getTargetMessageId();

        return Mono.just(source)
                // Step 1: Validate source message
                .flatMap(src -> validate(src, context)
                        .thenReturn(src)
                        .doOnError(validationError -> {
                            // Record validation errors in database if ValidationService is available
                            if (validationService != null && sourceMessageId != null) {
                                recordValidationErrors(sourceMessageId, context)
                                        .subscribe(
                                                v -> log.debug("Validation errors recorded for message: {}", sourceMessageId),
                                                e -> log.warn("Failed to record validation errors: {}", e.getMessage())
                                        );
                            }
                        }))

                // Step 2: Check if validation passed
                .flatMap(src -> {
                    if (!context.isValidForConversion()) {
                        // Record validation failure event
                        if (notificationService != null) {
                            notificationService.recordWarningEvent(
                                    "VALIDATION_FAILED",
                                    String.format("Validation failed for %s converter", converterName),
                                    sourceMessageId
                            ).subscribe(
                                    event -> log.debug("Validation failure event recorded: {}", event.getId()),
                                    e -> log.warn("Failed to record validation event: {}", e.getMessage())
                            );
                        }

                        return Mono.error(new ConversionException(
                                String.format("Validation failed for %s", converterName),
                                "VAL_FAILED",
                                context.getValidationResults()
                        ));
                    }
                    return Mono.just(src);
                })

                // Step 3: Perform the conversion
                .flatMap(src -> doConvert(src, context))

                // Step 4: Log success and record metrics
                .flatMap(target -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    log.info("Conversion successful: {} -> {} in {}ms",
                            sourceType.getSimpleName(),
                            targetType.getSimpleName(),
                            duration.toMillis());

                    // Record successful conversion with ConverterOrchestrationService
                    if (orchestrationService != null && sourceMessageId != null && targetMessageId != null) {
                        return orchestrationService.recordConversion(
                                sourceMessageId,
                                targetMessageId,
                                converterName,
                                duration.toMillis()
                        ).thenReturn(target)
                         .doOnError(e -> log.warn("Failed to record conversion metrics: {}", e.getMessage()))
                         .onErrorReturn(target); // Continue even if metrics recording fails
                    }

                    return Mono.just(target);
                })

                // Step 5: Handle errors - record failed conversion and system events
                .doOnError(error -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    log.error("Conversion failed: {} -> {} after {}ms - {}",
                            sourceType.getSimpleName(),
                            targetType.getSimpleName(),
                            duration.toMillis(),
                            error.getMessage(),
                            error);

                    // Record failed conversion with ConverterOrchestrationService
                    if (orchestrationService != null && sourceMessageId != null) {
                        orchestrationService.recordFailedConversion(
                                sourceMessageId,
                                converterName,
                                duration.toMillis(),
                                error.getMessage()
                        ).subscribe(
                                conv -> log.debug("Failed conversion recorded: {}", conv.getId()),
                                e -> log.warn("Failed to record failed conversion: {}", e.getMessage())
                        );
                    }

                    // Record error event with NotificationService
                    if (notificationService != null) {
                        String errorCode = error instanceof ConversionException
                                ? ((ConversionException) error).getErrorCode()
                                : "CONVERSION_ERROR";

                        notificationService.recordErrorEvent(
                                errorCode,
                                String.format("%s conversion failed: %s", converterName, error.getMessage()),
                                sourceMessageId,
                                getStackTraceAsString(error)
                        ).subscribe(
                                event -> log.debug("Error event recorded: {}", event.getId()),
                                e -> log.warn("Failed to record error event: {}", e.getMessage())
                        );
                    }
                })

                // Step 6: Timeout protection
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(java.util.concurrent.TimeoutException.class, e -> {
                    // Record timeout event
                    if (notificationService != null && sourceMessageId != null) {
                        notificationService.recordErrorEvent(
                                "CONVERSION_TIMEOUT",
                                String.format("%s conversion timeout after 5 seconds", converterName),
                                sourceMessageId,
                                null
                        ).subscribe();
                    }

                    return new ConversionException(
                            String.format("Conversion timeout: %s", converterName),
                            "TIMEOUT",
                            null
                    );
                });
    }

    /**
     * Performs the actual conversion.
     * Subclasses must implement this method.
     *
     * @param source source message
     * @param context converter context
     * @return Mono emitting converted target message
     */
    protected abstract Mono<T> doConvert(S source, ConverterContext context);

    /**
     * Validates the source message.
     * Default implementation checks basic context validity.
     * Override to add custom validation.
     *
     * @param source source message
     * @param context converter context
     * @return Mono<Void> that completes if valid, errors if invalid
     */
    @Override
    public Mono<Void> validate(S source, ConverterContext context) {
        return Mono.defer(() -> {
            // Check source not null
            if (source == null) {
                return Mono.error(new ConversionException(
                        "Source message cannot be null",
                        "NULL_SOURCE",
                        null
                ));
            }

            // Check context not null
            if (context == null) {
                return Mono.error(new ConversionException(
                        "Converter context cannot be null",
                        "NULL_CONTEXT",
                        null
                ));
            }

            // Check bank context
            if (context.getBankContext() == null) {
                return Mono.error(new ConversionException(
                        "Bank context is required",
                        "NULL_BANK_CONTEXT",
                        null
                ));
            }

            return Mono.empty();
        });
    }

    /**
     * Helper method to enrich context with generated IDs.
     * Call this at the beginning of doConvert() implementation.
     *
     * @param context converter context to enrich
     * @param messageIdPrefix prefix for message ID
     * @param generateUetr whether to generate a new UETR
     * @param generateTxId whether to generate a transaction ID
     */
    protected void enrichContextWithIds(ConverterContext context, String messageIdPrefix,
                                        boolean generateUetr, boolean generateTxId) {
        if (context.getGeneratedMessageId() == null) {
            context.setGeneratedMessageId(idGenerator.generateMessageId(messageIdPrefix));
        }

        if (generateUetr && context.getGeneratedUetr() == null) {
            context.setGeneratedUetr(idGenerator.generateUetr());
        }

        if (generateTxId && context.getGeneratedTransactionId() == null) {
            context.setGeneratedTransactionId(idGenerator.generateTransactionId());
        }
    }

    /**
     * Helper method to log conversion step.
     *
     * @param step step description
     */
    protected void logStep(String step) {
        log.debug("{} - {}", converterName, step);
    }

    /**
     * Helper method to log conversion error.
     *
     * @param step step description
     * @param error error that occurred
     */
    protected void logError(String step, Throwable error) {
        log.error("{} - {} failed: {}", converterName, step, error.getMessage(), error);
    }

    /**
     * Creates a Mono that errors with ConversionException.
     *
     * @param message error message
     * @param errorCode error code
     * @return Mono that errors
     */
    protected <R> Mono<R> conversionError(String message, String errorCode) {
        return Mono.error(new ConversionException(message, errorCode, null));
    }

    /**
     * Creates a Mono that errors with ConversionException including validation results.
     *
     * @param message error message
     * @param errorCode error code
     * @param validationResults validation results
     * @return Mono that errors
     */
    protected <R> Mono<R> conversionError(String message, String errorCode,
                                          ConverterContext.ValidationResults validationResults) {
        return Mono.error(new ConversionException(message, errorCode, validationResults));
    }

    /**
     * Records validation errors from context into database using ValidationService.
     *
     * @param messageId message ID
     * @param context converter context with validation results
     * @return Mono<Void> that completes when all errors are recorded
     */
    private Mono<Void> recordValidationErrors(UUID messageId, ConverterContext context) {
        if (context.getValidationResults() == null || context.getValidationResults().getErrors() == null) {
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> {
            for (ConverterContext.ValidationError error : context.getValidationResults().getErrors()) {
                String severity = error.getSeverity() != null
                        ? error.getSeverity().name()
                        : "ERROR";

                validationService.recordValidationError(
                        messageId,
                        error.getErrorCode() != null ? error.getErrorCode() : "UNKNOWN",
                        error.getCategory() != null ? error.getCategory() : "CONVERSION",
                        severity,
                        error.getFieldPath() != null ? error.getFieldPath() : "",
                        error.getErrorMessage() != null ? error.getErrorMessage() : "Validation error"
                ).subscribe(
                        ve -> log.debug("Validation error recorded: {}", ve.getId()),
                        e -> log.warn("Failed to record validation error: {}", e.getMessage())
                );
            }
        });
    }

    /**
     * Converts stack trace to string.
     *
     * @param throwable exception
     * @return stack trace as string
     */
    private String getStackTraceAsString(Throwable throwable) {
        if (throwable == null) {
            return null;
        }

        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();

        // Limit to first 1000 characters to avoid huge strings
        return stackTrace.length() > 1000 ? stackTrace.substring(0, 1000) + "..." : stackTrace;
    }
}
