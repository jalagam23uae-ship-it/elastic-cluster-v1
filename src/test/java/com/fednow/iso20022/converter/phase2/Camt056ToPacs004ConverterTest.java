package com.fednow.iso20022.converter.phase2;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.camt.Camt056;
import com.fednow.iso20022.domain.pacs.Pacs004;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Camt056ToPacs004Converter.
 * Tests cancellation request to payment return conversion.
 */
@DisplayName("Camt056ToPacs004Converter Tests")
class Camt056ToPacs004ConverterTest extends AbstractConverterTest {

    private Camt056ToPacs004Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Camt056ToPacs004Converter();
    }

    @Test
    @DisplayName("Should convert cancellation request to payment return")
    void shouldConvertCancellationRequestToPaymentReturn() {
        // Given
        Camt056 camt056 = createCancellationRequest("CUST");

        // When
        Mono<Pacs004> result = converter.convert(camt056, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            assertThat(pacs004).isNotNull();
            assertThat(pacs004.getGroupHeader()).isNotNull();
            assertThat(pacs004.getPaymentReturnInformation()).hasSize(1);

            Pacs004.PaymentReturnInformation returnInfo =
                    pacs004.getPaymentReturnInformation().get(0);
            assertThat(returnInfo.getReturnReasonInformation()).isNotNull();
            assertThat(returnInfo.getReturnReasonInformation().get(0).getReasonCode())
                    .isEqualTo("CUST");
        });
    }

    @Test
    @DisplayName("Should preserve original payment identifiers from cancellation")
    void shouldPreserveOriginalPaymentIdentifiers() {
        // Given
        String expectedE2E = "ORIG-E2E-CANCEL-123";
        String expectedTxnId = "ORIG-TXN-456";
        String expectedUetr = generateUetr();

        Camt056 camt056 = createCancellationRequest("DUPL");
        camt056.getCancellationRequest().getOriginalPaymentInformation()
                .setOriginalEndToEndId(expectedE2E);
        camt056.getCancellationRequest().getOriginalPaymentInformation()
                .setOriginalTransactionId(expectedTxnId);
        camt056.getCancellationRequest().getOriginalPaymentInformation()
                .setOriginalUetr(expectedUetr);

        // When
        Mono<Pacs004> result = converter.convert(camt056, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            Pacs004.PaymentReturnInformation returnInfo =
                    pacs004.getPaymentReturnInformation().get(0);
            assertThat(returnInfo.getOriginalEndToEndId()).isEqualTo(expectedE2E);
            assertThat(returnInfo.getOriginalTransactionId()).isEqualTo(expectedTxnId);
            assertThat(returnInfo.getOriginalUetr()).isEqualTo(expectedUetr);
        });
    }

    @Test
    @DisplayName("Should handle fraud cancellation reason")
    void shouldHandleFraudCancellation() {
        // Given
        Camt056 camt056 = createCancellationRequest("FRAD");

        // When
        Mono<Pacs004> result = converter.convert(camt056, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            String reasonCode = pacs004.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getReasonCode();
            assertThat(reasonCode).isEqualTo("FRAD");
        });
    }

    @Test
    @DisplayName("Should handle duplicate payment cancellation")
    void shouldHandleDuplicatePaymentCancellation() {
        // Given
        Camt056 camt056 = createCancellationRequest("DUPL");

        // When
        Mono<Pacs004> result = converter.convert(camt056, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            String reasonCode = pacs004.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getReasonCode();
            assertThat(reasonCode).isEqualTo("DUPL");
        });
    }

    @Test
    @DisplayName("Should include cancellation reason details")
    void shouldIncludeCancellationReasonDetails() {
        // Given
        String reasonDetail = "Payment sent to wrong account - customer request";
        Camt056 camt056 = createCancellationRequest("CUST");
        camt056.getCancellationRequest().getCancellationReason()
                .setAdditionalInformation(List.of(reasonDetail));

        // When
        Mono<Pacs004> result = converter.convert(camt056, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            List<String> actualInfo = pacs004.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getAdditionalInformation();
            assertThat(actualInfo).containsExactly(reasonDetail);
        });
    }

    @Test
    @DisplayName("Should preserve return amount from original payment")
    void shouldPreserveReturnAmount() {
        // Given
        Camt056 camt056 = createCancellationRequest("CUST");
        camt056.getCancellationRequest().getOriginalPaymentInformation()
                .setOriginalAmount(createUsdAmount("500.00"));

        // When
        Mono<Pacs004> result = converter.convert(camt056, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            assertThat(pacs004.getPaymentReturnInformation().get(0)
                    .getReturnedInterbankSettlementAmount().getValue())
                    .isEqualByComparingTo("500.00");
        });
    }

    @Test
    @DisplayName("Should handle technical error cancellation")
    void shouldHandleTechnicalErrorCancellation() {
        // Given
        Camt056 camt056 = createCancellationRequest("TECH");

        // When
        Mono<Pacs004> result = converter.convert(camt056, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            String reasonCode = pacs004.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getReasonCode();
            assertThat(reasonCode).isEqualTo("TECH");
        });
    }

    // ==================== Helper Methods ====================

    private Camt056 createCancellationRequest(String cancellationReasonCode) {
        return Camt056.builder()
                .assignment(Camt056.Assignment.builder()
                        .assignmentId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .assigner(createParty("Assigning Bank", null))
                        .assignee(createParty("Receiving Bank", null))
                        .build())
                .cancellationRequest(Camt056.CancellationRequest.builder()
                        .caseId("CASE-" + generateTransactionId())
                        .originalPaymentInformation(Camt056.OriginalPaymentInformation.builder()
                                .originalMessageId(generateMessageId())
                                .originalMessageNameId("pacs.008.001.11")
                                .originalEndToEndId("E2E123")
                                .originalTransactionId(generateTransactionId())
                                .originalUetr(generateUetr())
                                .originalAmount(createUsdAmount("100.00"))
                                .build())
                        .cancellationReason(Camt056.CancellationReason.builder()
                                .reasonCode(cancellationReasonCode)
                                .build())
                        .build())
                .build();
    }
}
