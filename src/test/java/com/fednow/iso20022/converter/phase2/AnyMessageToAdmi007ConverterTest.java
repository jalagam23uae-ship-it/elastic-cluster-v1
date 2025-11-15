package com.fednow.iso20022.converter.phase2;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.admi.Admi007;
import com.fednow.iso20022.domain.pacs.Pacs008;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AnyMessageToAdmi007Converter.
 * Tests receipt acknowledgment generation from any message.
 */
@DisplayName("AnyMessageToAdmi007Converter Tests")
class AnyMessageToAdmi007ConverterTest extends AbstractConverterTest {

    private AnyMessageToAdmi007Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new AnyMessageToAdmi007Converter();
    }

    @Test
    @DisplayName("Should create successful receipt acknowledgment")
    void shouldCreateSuccessfulReceipt() {
        // Given
        Pacs008 sourceMessage = createValidPacs008();
        context.setAttribute("originalMessageId", "MSG-12345");
        context.setAttribute("originalMessageNameId", "pacs.008.001.11");

        // When
        Mono<Admi007> result = converter.createSuccessfulReceipt(sourceMessage, context);

        // Then
        verifyConversionSuccess(result, admi007 -> {
            assertThat(admi007).isNotNull();
            assertThat(admi007.getReceiptAcknowledgement()).isNotNull();
            assertThat(admi007.getReceiptAcknowledgement().getOriginalMessageIdentification())
                    .isNotNull();
            assertThat(admi007.getReceiptAcknowledgement().getReceiptStatus())
                    .isEqualTo("ACPT");
        });
    }

    @Test
    @DisplayName("Should create authentication failure acknowledgment")
    void shouldCreateAuthenticationFailure() {
        // Given
        Pacs008 sourceMessage = createValidPacs008();
        context.setAttribute("originalMessageId", "MSG-12345");
        context.setAttribute("originalMessageNameId", "pacs.008.001.11");

        // When
        Mono<Admi007> result = converter.createAuthenticationFailure(sourceMessage, context);

        // Then
        verifyConversionSuccess(result, admi007 -> {
            assertThat(admi007.getReceiptAcknowledgement().getReceiptStatus())
                    .isEqualTo("RJCT");
            assertThat(admi007.getReceiptAcknowledgement().getRejectionReason()).isNotNull();
            assertThat(admi007.getReceiptAcknowledgement().getRejectionReason().getReasonCode())
                    .isEqualTo("AUTHF");
        });
    }

    @Test
    @DisplayName("Should create schema validation failure acknowledgment")
    void shouldCreateSchemaValidationFailure() {
        // Given
        Pacs008 sourceMessage = createValidPacs008();
        context.setAttribute("originalMessageId", "MSG-12345");
        context.setAttribute("originalMessageNameId", "pacs.008.001.11");

        // When
        Mono<Admi007> result = converter.createSchemaFailure(sourceMessage, context);

        // Then
        verifyConversionSuccess(result, admi007 -> {
            assertThat(admi007.getReceiptAcknowledgement().getReceiptStatus())
                    .isEqualTo("RJCT");
            assertThat(admi007.getReceiptAcknowledgement().getRejectionReason().getReasonCode())
                    .isEqualTo("SCHF");
        });
    }

    @Test
    @DisplayName("Should create duplicate message acknowledgment")
    void shouldCreateDuplicateAcknowledgment() {
        // Given
        Pacs008 sourceMessage = createValidPacs008();
        context.setAttribute("originalMessageId", "MSG-12345");
        context.setAttribute("originalMessageNameId", "pacs.008.001.11");

        // When
        Mono<Admi007> result = converter.createDuplicateMessageRejection(sourceMessage, context);

        // Then
        verifyConversionSuccess(result, admi007 -> {
            assertThat(admi007.getReceiptAcknowledgement().getReceiptStatus())
                    .isEqualTo("RJCT");
            assertThat(admi007.getReceiptAcknowledgement().getRejectionReason().getReasonCode())
                    .isEqualTo("DUPL");
        });
    }

    @Test
    @DisplayName("Should preserve original message identification")
    void shouldPreserveOriginalMessageIdentification() {
        // Given
        String expectedMsgId = "ORIG-MSG-789";
        String expectedMsgType = "pacs.008.001.11";
        Pacs008 sourceMessage = createValidPacs008();
        context.setAttribute("originalMessageId", expectedMsgId);
        context.setAttribute("originalMessageNameId", expectedMsgType);

        // When
        Mono<Admi007> result = converter.createSuccessfulReceipt(sourceMessage, context);

        // Then
        verifyConversionSuccess(result, admi007 -> {
            Admi007.OriginalMessageIdentification origMsgId =
                    admi007.getReceiptAcknowledgement().getOriginalMessageIdentification();
            assertThat(origMsgId.getMessageId()).isEqualTo(expectedMsgId);
            assertThat(origMsgId.getMessageNameIdentification()).isEqualTo(expectedMsgType);
        });
    }

    @Test
    @DisplayName("Should set receipt timestamp")
    void shouldSetReceiptTimestamp() {
        // Given
        Pacs008 sourceMessage = createValidPacs008();
        context.setAttribute("originalMessageId", "MSG-12345");
        context.setAttribute("originalMessageNameId", "pacs.008.001.11");

        // When
        Mono<Admi007> result = converter.createSuccessfulReceipt(sourceMessage, context);

        // Then
        verifyConversionSuccess(result, admi007 -> {
            assertThat(admi007.getReceiptAcknowledgement().getReceiptDateTime()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should handle rejection with custom reason")
    void shouldHandleCustomRejectionReason() {
        // Given
        Pacs008 sourceMessage = createValidPacs008();
        context.setAttribute("originalMessageId", "MSG-12345");
        context.setAttribute("originalMessageNameId", "pacs.008.001.11");
        context.setAttribute("receiptStatus", "RJCT");
        context.setAttribute("rejectionReasonCode", "ENCF");
        context.setAttribute("rejectionExplanation", "Encryption signature invalid");

        // When
        Mono<Admi007> result = converter.convert(sourceMessage, context);

        // Then
        verifyConversionSuccess(result, admi007 -> {
            assertThat(admi007.getReceiptAcknowledgement().getReceiptStatus())
                    .isEqualTo("RJCT");
            assertThat(admi007.getReceiptAcknowledgement().getRejectionReason().getReasonCode())
                    .isEqualTo("ENCF");
            assertThat(admi007.getReceiptAcknowledgement().getRejectionReason()
                    .getAdditionalInformation()).containsExactly("Encryption signature invalid");
        });
    }

    @Test
    @DisplayName("Should generate unique acknowledgment ID")
    void shouldGenerateUniqueAcknowledgmentId() {
        // Given
        Pacs008 sourceMessage = createValidPacs008();
        context.setAttribute("originalMessageId", "MSG-12345");
        context.setAttribute("originalMessageNameId", "pacs.008.001.11");

        // When
        Mono<Admi007> result1 = converter.createSuccessfulReceipt(sourceMessage, context);
        Mono<Admi007> result2 = converter.createSuccessfulReceipt(sourceMessage, context);

        // Then
        verifyConversionSuccess(result1, admi007_1 -> {
            verifyConversionSuccess(result2, admi007_2 -> {
                assertThat(admi007_1.getReceiptAcknowledgement().getAcknowledgmentId())
                        .isNotEqualTo(admi007_2.getReceiptAcknowledgement().getAcknowledgmentId());
            });
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
                                .instructingAgent(createBank("INSTUS33", "Instructing Bank"))
                                .instructedAgent(createBank("INSTDUS33", "Instructed Bank"))
                                .debtor(createParty("John Doe", "987654321"))
                                .creditor(createParty("Jane Smith", "123456789"))
                                .build()
                ))
                .build();
    }
}
