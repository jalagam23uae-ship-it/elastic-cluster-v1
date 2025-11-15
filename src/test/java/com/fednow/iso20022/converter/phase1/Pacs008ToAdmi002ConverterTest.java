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
 * Unit tests for Pacs008ToAdmi002Converter.
 * Tests payment to system event notification conversion.
 */
@DisplayName("Pacs008ToAdmi002Converter Tests")
class Pacs008ToAdmi002ConverterTest extends AbstractConverterTest {

    private Pacs008ToAdmi002Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs008ToAdmi002Converter();
    }

    @Test
    @DisplayName("Should create settlement completion notification")
    void shouldCreateSettlementCompletionNotification() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("eventType", "STLM");
        context.setAttribute("eventDescription", "Payment settled successfully");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002).isNotNull();
            assertThat(admi002.getMessageHeader()).isNotNull();
            assertThat(admi002.getSystemEventNotification()).isNotNull();
            assertThat(admi002.getSystemEventNotification().getEventType()).isEqualTo("STLM");
        });
    }

    @Test
    @DisplayName("Should create rejection notification")
    void shouldCreateRejectionNotification() {
        // Given
        Pacs008 pacs008 = createValidPacs008();

        // When
        Mono<Admi002> result = converter.createSystemRejection(pacs008, "AC01",
                "Invalid account number", context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getSystemEventNotification().getEventType()).isEqualTo("RJCT");
            assertThat(admi002.getSystemEventNotification().getRejectionReason()).isNotNull();
            assertThat(admi002.getSystemEventNotification().getRejectionReason().getReasonCode())
                    .isEqualTo("AC01");
            assertThat(admi002.getSystemEventNotification().getRejectionReason()
                    .getAdditionalInformation()).containsExactly("Invalid account number");
        });
    }

    @Test
    @DisplayName("Should create settlement notification")
    void shouldCreateSettlementNotification() {
        // Given
        Pacs008 pacs008 = createValidPacs008();

        // When
        Mono<Admi002> result = converter.createSettlementNotification(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getSystemEventNotification().getEventType()).isEqualTo("STLM");
            assertThat(admi002.getSystemEventNotification().getEventDescription())
                    .contains("settled successfully");
        });
    }

    @Test
    @DisplayName("Should preserve original message reference")
    void shouldPreserveOriginalMessageReference() {
        // Given
        String originalMsgId = "ORIG-MSG-12345";
        Pacs008 pacs008 = createValidPacs008();
        pacs008.getGroupHeader().setMessageId(originalMsgId);
        context.setAttribute("eventType", "STLM");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getSystemEventNotification().getOriginalMessageId())
                    .isEqualTo(originalMsgId);
        });
    }

    @Test
    @DisplayName("Should include payment transaction reference")
    void shouldIncludePaymentTransactionReference() {
        // Given
        String expectedE2E = "E2E-PAYMENT-789";
        String expectedTxnId = "TXN-456";
        Pacs008 pacs008 = createValidPacs008();
        pacs008.getCreditTransferTransactionInformation().get(0).getPaymentId()
                .setEndToEndId(expectedE2E);
        pacs008.getCreditTransferTransactionInformation().get(0).getPaymentId()
                .setTransactionId(expectedTxnId);
        context.setAttribute("eventType", "STLM");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getSystemEventNotification().getTransactionIdentification())
                    .isNotNull();
            assertThat(admi002.getSystemEventNotification().getTransactionIdentification()
                    .getEndToEndId()).isEqualTo(expectedE2E);
            assertThat(admi002.getSystemEventNotification().getTransactionIdentification()
                    .getTransactionId()).isEqualTo(expectedTxnId);
        });
    }

    @Test
    @DisplayName("Should set event timestamp")
    void shouldSetEventTimestamp() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("eventType", "STLM");

        // When
        Mono<Admi002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, admi002 -> {
            assertThat(admi002.getSystemEventNotification().getEventDateTime()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should handle different event types")
    void shouldHandleDifferentEventTypes() {
        // Test different event types
        String[] eventTypes = {"STLM", "RJCT", "PDNG", "PROC"};

        for (String eventType : eventTypes) {
            Pacs008 pacs008 = createValidPacs008();
            context.setAttribute("eventType", eventType);

            Mono<Admi002> result = converter.convert(pacs008, context);

            verifyConversionSuccess(result, admi002 -> {
                assertThat(admi002.getSystemEventNotification().getEventType())
                        .isEqualTo(eventType);
            });
        }
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
                                .instructingAgent(createBank("INSTUS33", "Instructing Bank"))
                                .instructedAgent(createBank("INSTDUS33", "Instructed Bank"))
                                .debtor(createParty("John Doe", "987654321"))
                                .creditor(createParty("Jane Smith", "123456789"))
                                .build()
                ))
                .build();
    }
}
