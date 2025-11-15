package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.admi.Admi002;
import com.fednow.iso20022.domain.pacs.Pacs008;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AnyMessageToAdmi002Converter.
 * Tests generic system error handling.
 */
@DisplayName("AnyMessageToAdmi002Converter Tests")
class AnyMessageToAdmi002ConverterTest extends AbstractConverterTest {

    private AnyMessageToAdmi002Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new AnyMessageToAdmi002Converter();
    }

    @Test
    @DisplayName("Should convert any message to system error")
    void shouldConvertAnyMessageToSystemError() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("errorCode", "SYSF");
        context.setAttribute("errorMessage", "Database connection failed");
        context.setAttribute("errorComponent", "Database");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002).isNotNull();
            assertThat(admi002.getMessageId()).isNotNull();
            assertThat(admi002.getCreationDateTime()).isNotNull();
            assertThat(admi002.getReason()).isNotNull();
            assertThat(admi002.getReason().getCode()).isEqualTo("SYSF");
        });
    }

    @Test
    @DisplayName("Should handle system failure")
    void shouldHandleSystemFailure() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String component = "Payment Processing";
        String details = "Service unavailable";
        Exception exception = new RuntimeException("Service down");

        // When
        Mono<Admi002> result = converter.createSystemFailure(
                pacs008, context, component, details, exception);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("SYSF");
            assertThat(admi002.getReason().getSeverity()).isEqualTo("FATAL");
            assertThat(admi002.getReason().getAffectedComponent()).isEqualTo(component);
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(details));
        });
    }

    @Test
    @DisplayName("Should handle network failure")
    void shouldHandleNetworkFailure() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String details = "Connection timeout";
        Exception exception = new java.net.SocketTimeoutException("Timeout");

        // When
        Mono<Admi002> result = converter.createNetworkFailure(
                pacs008, context, details, exception);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("NETF");
            assertThat(admi002.getReason().getSeverity()).isEqualTo("FATAL");
            assertThat(admi002.getReason().getAffectedComponent()).isEqualTo("Network");
            assertThat(admi002.getReason().getRecommendedAction())
                    .contains("network connectivity");
        });
    }

    @Test
    @DisplayName("Should handle database failure")
    void shouldHandleDatabaseFailure() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String details = "Connection pool exhausted";
        SQLException exception = new SQLException("No connections available");

        // When
        Mono<Admi002> result = converter.createDatabaseFailure(
                pacs008, context, details, exception);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("DBNF");
            assertThat(admi002.getReason().getSeverity()).isEqualTo("FATAL");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains("SQLException"));
        });
    }

    @Test
    @DisplayName("Should handle configuration error")
    void shouldHandleConfigurationError() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String details = "Missing required property: database.url";

        // When
        Mono<Admi002> result = converter.createConfigurationError(
                pacs008, context, details);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("CONF");
            assertThat(admi002.getReason().getSeverity()).isEqualTo("FATAL");
            assertThat(admi002.getReason().getAffectedComponent()).isEqualTo("Configuration");
            assertThat(admi002.getReason().getRecommendedAction())
                    .contains("configuration");
        });
    }

    @Test
    @DisplayName("Should handle resource exhaustion")
    void shouldHandleResourceExhaustion() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String resourceType = "Memory";
        String details = "Heap space exceeded";

        // When
        Mono<Admi002> result = converter.createResourceExhaustion(
                pacs008, context, resourceType, details);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("RESF");
            assertThat(admi002.getReason().getSeverity()).isEqualTo("FATAL");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(resourceType));
        });
    }

    @Test
    @DisplayName("Should handle unknown error")
    void shouldHandleUnknownError() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        Exception exception = new NullPointerException("Unexpected null value");

        // When
        Mono<Admi002> result = converter.createUnknownError(pacs008, context, exception);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("UNKN");
            assertThat(admi002.getReason().getSeverity()).isEqualTo("FATAL");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains("NullPointerException"));
        });
    }

    @Test
    @DisplayName("Should include related reference")
    void shouldIncludeRelatedReference() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("errorCode", "SYSF");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getRelatedReference()).isNotNull();
            assertThat(admi002.getRelatedReference().getMessageId()).isNotNull();
            assertThat(admi002.getRelatedReference().getMessageNameIdentification())
                    .contains("pacs.008");
        });
    }

    @Test
    @DisplayName("Should provide actionable recommended action")
    void shouldProvideRecommendedAction() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("errorCode", "SYSF");
        context.setAttribute("errorMessage", "Critical service failure");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getRecommendedAction()).isNotNull();
            assertThat(admi002.getReason().getRecommendedAction())
                    .isNotEmpty();
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
