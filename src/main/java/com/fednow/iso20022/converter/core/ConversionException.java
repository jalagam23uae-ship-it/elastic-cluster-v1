package com.fednow.iso20022.converter.core;

import lombok.Getter;

/**
 * Exception thrown during message conversion.
 *
 * Contains:
 * - Error message
 * - Error code for categorization
 * - Optional validation results
 */
@Getter
public class ConversionException extends RuntimeException {

    /**
     * Error code for categorization.
     * Examples: VAL_FAILED, TIMEOUT, NULL_SOURCE, MAPPING_ERROR
     */
    private final String errorCode;

    /**
     * Validation results if the error was due to validation failure.
     */
    private final ConverterContext.ValidationResults validationResults;

    /**
     * Constructor with message and error code.
     *
     * @param message error message
     * @param errorCode error code
     * @param validationResults validation results (nullable)
     */
    public ConversionException(String message, String errorCode,
                               ConverterContext.ValidationResults validationResults) {
        super(message);
        this.errorCode = errorCode;
        this.validationResults = validationResults;
    }

    /**
     * Constructor with message, error code, and cause.
     *
     * @param message error message
     * @param errorCode error code
     * @param cause underlying cause
     */
    public ConversionException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.validationResults = null;
    }

    /**
     * Constructor with message and error code only.
     *
     * @param message error message
     * @param errorCode error code
     */
    public ConversionException(String message, String errorCode) {
        this(message, errorCode, (ConverterContext.ValidationResults) null);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConversionException[");
        sb.append("errorCode=").append(errorCode);
        sb.append(", message=").append(getMessage());

        if (validationResults != null && validationResults.getErrors() != null) {
            sb.append(", validationErrors=").append(validationResults.getErrors().size());
        }

        sb.append("]");
        return sb.toString();
    }
}
