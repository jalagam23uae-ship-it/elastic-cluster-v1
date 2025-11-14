package com.fednow.iso20022.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Standard API response wrapper.
 *
 * @param <T> The type of the response data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "Conversion completed successfully")
    private String message;

    @Schema(description = "The response data")
    private T data;

    @Schema(description = "Error details if request failed")
    private ErrorDetails error;

    @Schema(description = "Response timestamp")
    @Builder.Default
    private ZonedDateTime timestamp = ZonedDateTime.now();

    @Schema(description = "Request correlation ID for tracing")
    private String correlationId;

    /**
     * Creates a successful response.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Creates a successful response with default message.
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Request processed successfully");
    }

    /**
     * Creates an error response.
     */
    public static <T> ApiResponse<T> error(String message, ErrorDetails error) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .timestamp(ZonedDateTime.now())
                .build();
    }

    /**
     * Creates an error response with default error details.
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return error(message, ErrorDetails.builder()
                .code(errorCode)
                .message(message)
                .build());
    }

    /**
     * Error details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Error details")
    public static class ErrorDetails {

        @Schema(description = "Error code", example = "VALIDATION_ERROR")
        private String code;

        @Schema(description = "Error message", example = "Invalid message format")
        private String message;

        @Schema(description = "Detailed error description")
        private String details;

        @Schema(description = "Field name if validation error")
        private String field;

        @Schema(description = "Stack trace (only in development)")
        private String stackTrace;
    }
}
