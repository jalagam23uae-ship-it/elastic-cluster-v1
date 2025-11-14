package com.fednow.iso20022.api.exception;

import com.fednow.iso20022.api.dto.ApiResponse;
import com.fednow.iso20022.converter.core.ConversionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

/**
 * Global exception handler for REST API controllers.
 *
 * Provides consistent error responses across all endpoints.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles conversion exceptions.
     */
    @ExceptionHandler(ConversionException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleConversionException(
            ConversionException ex) {

        log.error("Conversion error: {}", ex.getMessage(), ex);

        ApiResponse.ErrorDetails errorDetails = ApiResponse.ErrorDetails.builder()
                .code("CONVERSION_ERROR")
                .message(ex.getMessage())
                .details(ex.getDetails())
                .build();

        ApiResponse<Void> response = ApiResponse.error(
                "Message conversion failed",
                errorDetails);

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response));
    }

    /**
     * Handles validation exceptions.
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleValidationException(
            WebExchangeBindException ex) {

        log.error("Validation error: {}", ex.getMessage());

        String validationErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiResponse.ErrorDetails errorDetails = ApiResponse.ErrorDetails.builder()
                .code("VALIDATION_ERROR")
                .message("Request validation failed")
                .details(validationErrors)
                .build();

        ApiResponse<Void> response = ApiResponse.error(
                "Validation failed",
                errorDetails);

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response));
    }

    /**
     * Handles illegal argument exceptions.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.error("Invalid argument: {}", ex.getMessage());

        ApiResponse.ErrorDetails errorDetails = ApiResponse.ErrorDetails.builder()
                .code("INVALID_ARGUMENT")
                .message(ex.getMessage())
                .build();

        ApiResponse<Void> response = ApiResponse.error(
                "Invalid request parameters",
                errorDetails);

        return Mono.just(ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response));
    }

    /**
     * Handles illegal state exceptions.
     */
    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIllegalStateException(
            IllegalStateException ex) {

        log.error("Invalid state: {}", ex.getMessage());

        ApiResponse.ErrorDetails errorDetails = ApiResponse.ErrorDetails.builder()
                .code("INVALID_STATE")
                .message(ex.getMessage())
                .build();

        ApiResponse<Void> response = ApiResponse.error(
                "Invalid operation state",
                errorDetails);

        return Mono.just(ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response));
    }

    /**
     * Handles generic exceptions.
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleGenericException(
            Exception ex) {

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ApiResponse.ErrorDetails errorDetails = ApiResponse.ErrorDetails.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .details(ex.getMessage())
                .build();

        ApiResponse<Void> response = ApiResponse.error(
                "Internal server error",
                errorDetails);

        return Mono.just(ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response));
    }
}
