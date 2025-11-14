package com.fednow.iso20022.converter.core;

import com.fednow.iso20022.converter.utils.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * Abstract base class for message converters.
 *
 * Provides common functionality:
 * - Validation
 * - Logging
 * - Metrics
 * - Error handling
 * - ID generation
 *
 * @param <S> Source message type
 * @param <T> Target message type
 */
@Slf4j
public abstract class AbstractMessageConverter<S, T> implements MessageConverter<S, T> {

    @Autowired
    protected IdGenerator idGenerator;

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

        return Mono.just(source)
                // Step 1: Validate source message
                .flatMap(src -> validate(src, context)
                        .thenReturn(src))

                // Step 2: Check if validation passed
                .flatMap(src -> {
                    if (!context.isValidForConversion()) {
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

                // Step 4: Log success and metrics
                .doOnSuccess(target -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    log.info("Conversion successful: {} -> {} in {}ms",
                            sourceType.getSimpleName(),
                            targetType.getSimpleName(),
                            duration.toMillis());
                })

                // Step 5: Handle errors
                .doOnError(error -> {
                    Duration duration = Duration.between(startTime, Instant.now());
                    log.error("Conversion failed: {} -> {} after {}ms - {}",
                            sourceType.getSimpleName(),
                            targetType.getSimpleName(),
                            duration.toMillis(),
                            error.getMessage(),
                            error);
                })

                // Step 6: Timeout protection
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(java.util.concurrent.TimeoutException.class, e ->
                        new ConversionException(
                                String.format("Conversion timeout: %s", converterName),
                                "TIMEOUT",
                                null
                        ));
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
}
