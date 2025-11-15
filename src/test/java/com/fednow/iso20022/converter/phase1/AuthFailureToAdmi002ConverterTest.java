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
 * Unit tests for AuthFailureToAdmi002Converter.
 * Tests authentication and security event handling.
 */
@DisplayName("AuthFailureToAdmi002Converter Tests")
class AuthFailureToAdmi002ConverterTest extends AbstractConverterTest {

    private AuthFailureToAdmi002Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new AuthFailureToAdmi002Converter();
    }

    @Test
    @DisplayName("Should convert authentication failure to admi.002")
    void shouldConvertAuthFailureToAdmi002() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("securityEventType", "AUTHF");
        context.setAttribute("failureReason", "Invalid credentials");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002).isNotNull();
            assertThat(admi002.getReason()).isNotNull();
            assertThat(admi002.getReason().getCode()).isEqualTo("AUTHF");
            assertThat(admi002.getReason().getSeverity()).isEqualTo("FATAL");
        });
    }

    @Test
    @DisplayName("Should handle invalid credentials")
    void shouldHandleInvalidCredentials() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String username = "john.doe@bank.com";
        String ipAddress = "192.168.1.100";
        int failureCount = 1;

        // When
        Mono<Admi002> result = converter.createInvalidCredentials(
                pacs008, context, username, ipAddress, failureCount);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("AUTHF");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(username))
                    .anyMatch(info -> info.contains(ipAddress));
        });
    }

    @Test
    @DisplayName("Should escalate severity for multiple failures")
    void shouldEscalateSeverityForMultipleFailures() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String username = "john.doe@bank.com";
        String ipAddress = "192.168.1.100";
        int failureCount = 5;

        // When
        Mono<Admi002> result = converter.createInvalidCredentials(
                pacs008, context, username, ipAddress, failureCount);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getSeverity()).isEqualTo("FATAL");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains("Consecutive failures: 5"));
            assertThat(admi002.getReason().getRecommendedAction())
                    .contains("Account locked");
        });
    }

    @Test
    @DisplayName("Should handle expired certificate")
    void shouldHandleExpiredCertificate() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String certificateSubject = "CN=bank.example.com";

        // When
        Mono<Admi002> result = converter.createExpiredCertificate(
                pacs008, context, certificateSubject);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("CERT");
            assertThat(admi002.getReason().getAffectedComponent())
                    .isEqualTo("Certificate Management");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(certificateSubject));
        });
    }

    @Test
    @DisplayName("Should handle signature verification failure")
    void shouldHandleSignatureFailure() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String details = "Signature algorithm mismatch";

        // When
        Mono<Admi002> result = converter.createSignatureFailure(pacs008, context, details);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("SIGN");
            assertThat(admi002.getReason().getAffectedComponent())
                    .isEqualTo("Digital Signature");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(details));
        });
    }

    @Test
    @DisplayName("Should handle encryption failure")
    void shouldHandleEncryptionFailure() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String details = "TLS handshake failed";

        // When
        Mono<Admi002> result = converter.createEncryptionFailure(pacs008, context, details);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("ENCF");
            assertThat(admi002.getReason().getSeverity()).isEqualTo("FATAL");
            assertThat(admi002.getReason().getRecommendedAction())
                    .contains("TLS configuration");
        });
    }

    @Test
    @DisplayName("Should handle authorization failure")
    void shouldHandleAuthorizationFailure() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String username = "operator@bank.com";
        String requiredPermission = "PAYMENT_APPROVAL";

        // When
        Mono<Admi002> result = converter.createAuthorizationFailure(
                pacs008, context, username, requiredPermission);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("AUTZ");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(requiredPermission));
        });
    }

    @Test
    @DisplayName("Should handle suspicious activity")
    void shouldHandleSuspiciousActivity() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String username = "suspect@bank.com";
        String ipAddress = "10.0.0.1";
        String activityDetails = "Multiple large transactions from new location";

        // When
        Mono<Admi002> result = converter.createSuspiciousActivity(
                pacs008, context, username, ipAddress, activityDetails);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("SECV");
            assertThat(admi002.getReason().getAffectedComponent())
                    .isEqualTo("Security Monitoring");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(activityDetails));
        });
    }

    @Test
    @DisplayName("Should handle rate limiting")
    void shouldHandleRateLimiting() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String username = "api-client@bank.com";
        String ipAddress = "203.0.113.50";
        int requestCount = 1500;
        int limit = 1000;

        // When
        Mono<Admi002> result = converter.createRateLimitExceeded(
                pacs008, context, username, ipAddress, requestCount, limit);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getCode()).isEqualTo("SECV");
            assertThat(admi002.getReason().getAffectedComponent())
                    .isEqualTo("Rate Limiting");
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains(String.valueOf(requestCount)))
                    .anyMatch(info -> info.contains(String.valueOf(limit)));
        });
    }

    @Test
    @DisplayName("Should warn about multiple failed attempts")
    void shouldWarnAboutMultipleAttempts() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String username = "john.doe@bank.com";
        String ipAddress = "192.168.1.100";
        int failureCount = 3;

        // When
        Mono<Admi002> result = converter.createInvalidCredentials(
                pacs008, context, username, ipAddress, failureCount);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getAdditionalInformation())
                    .anyMatch(info -> info.contains("Multiple failed attempts"));
        });
    }

    @Test
    @DisplayName("Should include actionable recommended action")
    void shouldIncludeActionableRecommendedAction() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("securityEventType", "AUTHF");
        context.setAttribute("failureReason", "Authentication failed");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getReason().getRecommendedAction()).isNotNull();
            assertThat(admi002.getReason().getRecommendedAction()).isNotEmpty();
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
