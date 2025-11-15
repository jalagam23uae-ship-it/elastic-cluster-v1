package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.admi.Admi002;
import com.fednow.iso20022.domain.pacs.Pacs008;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SchemaFailureToAdmi002Converter.
 * Tests schema and format validation error handling.
 */
@DisplayName("SchemaFailureToAdmi002Converter Tests")
class SchemaFailureToAdmi002ConverterTest extends AbstractConverterTest {

    private SchemaFailureToAdmi002Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new SchemaFailureToAdmi002Converter();
    }

    @Test
    @DisplayName("Should convert schema failure to admi.002")
    void shouldConvertSchemaFailureToAdmi002() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("validationType", "SCHF");
        context.setAttribute("validationMessage", "Invalid XML structure");
        context.setAttribute("messageType", "pacs.008.001.11");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002).isNotNull();
            assertThat(admi002.getReason()).isNotNull();
            assertThat(admi002.getReason().getCode()).isEqualTo("SCHF");
            assertThat(admi002.getReason().getSeverity()).isEqualTo("FATAL");
        });
    }

    @Test
    @DisplayName("Should handle missing field error")
    void shouldHandleMissingFieldError() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String messageType = "pacs.008.001.11";
        String fieldName = "GroupHeader.MessageId";

        // When
        Mono<Admi002> result = converter.createMissingFieldError(
                pacs008, context, messageType, fieldName);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("MISS");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(fieldName));
            assertThat(admi002.getReason().getRecommendedAction())
                    .contains("Add required field");
        });
    }

    @Test
    @DisplayName("Should handle field length violation")
    void shouldHandleFieldLengthViolation() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String messageType = "pacs.008.001.11";
        String fieldName = "GroupHeader.MessageId";
        String fieldValue = "ThisIsAVeryLongMessageIdThatExceedsTheMaximumAllowedLengthOf35Characters";
        int maxLength = 35;

        // When
        Mono<Admi002> result = converter.createFieldLengthError(
                pacs008, context, messageType, fieldName, fieldValue, maxLength);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("LENG");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(fieldName))
                    .anyMatch(info -> info.contains(String.valueOf(maxLength)));
        });
    }

    @Test
    @DisplayName("Should handle format error")
    void shouldHandleFormatError() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String messageType = "pacs.008.001.11";
        String fieldName = "GroupHeader.CreationDateTime";
        String fieldValue = "2024-13-01T10:30:00"; // Invalid month
        String expectedFormat = "YYYY-MM-DDTHH:MM:SS";

        // When
        Mono<Admi002> result = converter.createFormatError(
                pacs008, context, messageType, fieldName, fieldValue, expectedFormat);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("FMTF");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(expectedFormat));
        });
    }

    @Test
    @DisplayName("Should handle XML structure error")
    void shouldHandleStructureError() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String messageType = "pacs.008.001.11";
        String errorMessage = "Unexpected closing tag";
        int lineNumber = 42;
        int columnNumber = 15;

        // When
        Mono<Admi002> result = converter.createStructureError(
                pacs008, context, messageType, errorMessage, lineNumber, columnNumber);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("STRF");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains("Line " + lineNumber))
                    .anyMatch(info -> info.contains("Column " + columnNumber));
        });
    }

    @Test
    @DisplayName("Should handle multiple validation errors")
    void shouldHandleMultipleValidationErrors() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String messageType = "pacs.008.001.11";
        List<String> validationErrors = List.of(
                "Missing required field: GroupHeader.MessageId",
                "Invalid BIC code format: XXXXX",
                "Field exceeds maximum length: EndToEndId",
                "Invalid currency code: ABC"
        );

        // When
        Mono<Admi002> result = converter.createMultipleValidationErrors(
                pacs008, context, messageType, validationErrors);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("SCHF");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains("4 errors"));
            // Should include first 3 errors
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains("Error 1"))
                    .anyMatch(info -> info.contains("Error 2"))
                    .anyMatch(info -> info.contains("Error 3"));
        });
    }

    @Test
    @DisplayName("Should handle invalid message type")
    void shouldHandleInvalidMessageType() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String receivedType = "pain.001.001.11";
        String expectedType = "pacs.008.001.11";

        // When
        Mono<Admi002> result = converter.createInvalidMessageType(
                pacs008, context, receivedType, expectedType);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("STRF");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(receivedType))
                    .anyMatch(info -> info.contains(expectedType));
        });
    }

    @Test
    @DisplayName("Should handle encoding error")
    void shouldHandleEncodingError() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String messageType = "pacs.008.001.11";
        String details = "Invalid UTF-8 byte sequence";

        // When
        Mono<Admi002> result = converter.createEncodingError(
                pacs008, context, messageType, details);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("ENCF");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(details));
            assertThat(admi002.getReason().getRecommendedAction())
                    .contains("UTF-8");
        });
    }

    @Test
    @DisplayName("Should handle pattern violation")
    void shouldHandlePatternViolation() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String messageType = "pacs.008.001.11";
        String fieldName = "InstructingAgent.BIC";
        String fieldValue = "INVALID";
        String pattern = "[A-Z]{6}[A-Z0-9]{2}([A-Z0-9]{3})?";

        // When
        Mono<Admi002> result = converter.createPatternViolation(
                pacs008, context, messageType, fieldName, fieldValue, pattern);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("FMTF");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(fieldName))
                    .anyMatch(info -> info.contains(pattern));
        });
    }

    @Test
    @DisplayName("Should truncate long field values")
    void shouldTruncateLongFieldValues() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String messageType = "pacs.008.001.11";
        String fieldName = "RemittanceInformation";
        String longValue = "A".repeat(100); // 100 characters
        String expectedFormat = "Max 70 characters";

        // When
        Mono<Admi002> result = converter.createFormatError(
                pacs008, context, messageType, fieldName, longValue, expectedFormat);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            // Value should be truncated to 50 characters + "..."
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains("Value:") && info.contains("..."));
        });
    }

    @Test
    @DisplayName("Should provide actionable recommended action")
    void shouldProvideActionableRecommendedAction() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("validationType", "SCHF");
        context.setAttribute("validationMessage", "Schema validation failed");
        context.setAttribute("messageType", "pacs.008.001.11");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getRecommendedAction()).isNotNull();
            assertThat(admi002.getReason().getRecommendedAction())
                    .contains("ISO 20022");
        });
    }

    // ==================== Helper Methods ====================

    private Pacs008 createValidPacs008() {
        return Pacs008.builder()
                .groupHeader(Pacs008.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .numberOfTransactions(1)
                        .settlementMethod("INDA")
                        .build())
                .creditTransferTransactionInformation(List.of(
                        Pacs008.CreditTransferTransaction.builder()
                                .paymentId(Pacs008.PaymentIdentification.builder()
                                        .endToEndId("E2E123")
                                        .transactionId(generateTransactionId())
                                        .uetr(generateUetr())
                                        .build())
                                .interbankSettlementAmount(createUsdAmount("100.00"))
                                .build()
                ))
                .build();
    }
}
