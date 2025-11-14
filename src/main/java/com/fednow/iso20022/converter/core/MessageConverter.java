package com.fednow.iso20022.converter.core;

import reactor.core.publisher.Mono;

/**
 * Base interface for all ISO 20022 message converters.
 *
 * @param <S> Source message type
 * @param <T> Target message type
 */
public interface MessageConverter<S, T> {

    /**
     * Converts a source message to a target message reactively.
     *
     * @param source the source message to convert
     * @param context converter context with enrichment data
     * @return Mono emitting the converted target message
     */
    Mono<T> convert(S source, ConverterContext context);

    /**
     * Gets the source message type.
     *
     * @return source message class
     */
    Class<S> getSourceType();

    /**
     * Gets the target message type.
     *
     * @return target message class
     */
    Class<T> getTargetType();

    /**
     * Gets the converter name for logging and metrics.
     *
     * @return converter name
     */
    String getConverterName();

    /**
     * Validates the source message before conversion.
     * Override to add custom validation logic.
     *
     * @param source the source message
     * @param context converter context
     * @return Mono<Void> that completes if valid, errors if invalid
     */
    default Mono<Void> validate(S source, ConverterContext context) {
        return Mono.empty();
    }
}
