package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pacs.Pacs004;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pacs004ToPacs002Converter.
 * Tests return acknowledgment conversion.
 */
@DisplayName("Pacs004ToPacs002Converter Tests")
class Pacs004ToPacs002ConverterTest extends AbstractConverterTest {

    private Pacs004ToPacs002Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs004ToPacs002Converter();
    }

    @Test
    @DisplayName("Should convert return to acknowledgment")
    void shouldConvertReturnToAcknowledgment() {
        // Given
        Pacs004 pacs004 = createValidPacs004();

        // When
        Mono<Pacs002> result = converter.convert(pacs004, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002).isNotNull();
            assertThat(pacs002.getGroupHeader()).isNotNull();
            assertThat(pacs002.getTransactionInformationAndStatus()).hasSize(1);
        });
    }

    @Test
    @DisplayName("Should acknowledge accepted return")
    void shouldAcknowledgeAcceptedReturn() {
        // Given
        Pacs004 pacs004 = createValidPacs004();

        // When
        Mono<Pacs002> result = converter.createAcceptedReturn(pacs004, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002.getTransactionInformationAndStatus().get(0)
                    .getTransactionStatus()).isEqualTo("ACCP");
        });
    }

    @Test
    @DisplayName("Should acknowledge settled return")
    void shouldAcknowledgeSettledReturn() {
        // Given
        Pacs004 pacs004 = createValidPacs004();

        // When
        Mono<Pacs002> result = converter.createSettledReturn(pacs004, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002.getTransactionInformationAndStatus().get(0)
                    .getTransactionStatus()).isEqualTo("ACSC");
        });
    }

    @Test
    @DisplayName("Should handle rejected return")
    void shouldHandleRejectedReturn() {
        // Given
        Pacs004 pacs004 = createValidPacs004();
        String reasonCode = "NOOR";
        String reason = "No original transaction reference found";

        // When
        Mono<Pacs002> result = converter.createRejectedReturn(
                pacs004, context, reasonCode, reason);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            Pacs002.TransactionInformationAndStatus status =
                    pacs002.getTransactionInformationAndStatus().get(0);

            assertThat(status.getTransactionStatus()).isEqualTo("RJCT");
            assertThat(status.getStatusReasonInformation()).isNotEmpty();
            assertThat(status.getStatusReasonInformation().get(0).getReason().getReasonCode())
                    .isEqualTo(reasonCode);
        });
    }

    @Test
    @DisplayName("Should handle pending return")
    void shouldHandlePendingReturn() {
        // Given
        Pacs004 pacs004 = createValidPacs004();
        String pendingReason = "Return requires additional verification";

        // When
        Mono<Pacs002> result = converter.createPendingReturn(
                pacs004, context, pendingReason);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            Pacs002.TransactionInformationAndStatus status =
                    pacs002.getTransactionInformationAndStatus().get(0);

            assertThat(status.getTransactionStatus()).isEqualTo("PDNG");
            assertThat(status.getStatusReasonInformation()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("Should preserve original return IDs")
    void shouldPreserveOriginalReturnIds() {
        // Given
        Pacs004 pacs004 = createValidPacs004();

        // When
        Mono<Pacs002> result = converter.convert(pacs004, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            Pacs002.TransactionInformationAndStatus status =
                    pacs002.getTransactionInformationAndStatus().get(0);

            assertThat(status.getOriginalEndToEndId()).isEqualTo("E2E-ORIG-123");
            assertThat(status.getOriginalTransactionId()).isNotNull();
            assertThat(status.getOriginalUetr()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should reverse agents in response")
    void shouldReverseAgentsInResponse() {
        // Given
        Pacs004 pacs004 = createValidPacs004();

        // When
        Mono<Pacs002> result = converter.convert(pacs004, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            // Agents should be reversed
            assertThat(pacs002.getGroupHeader().getInstructingAgent())
                    .isEqualTo(pacs004.getGroupHeader().getInstructedAgent());
            assertThat(pacs002.getGroupHeader().getInstructedAgent())
                    .isEqualTo(pacs004.getGroupHeader().getInstructingAgent());
        });
    }

    @Test
    @DisplayName("Should include acceptance datetime for accepted returns")
    void shouldIncludeAcceptanceDateTimeForAccepted() {
        // Given
        Pacs004 pacs004 = createValidPacs004();

        // When
        Mono<Pacs002> result = converter.createAcceptedReturn(pacs004, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002.getTransactionInformationAndStatus().get(0)
                    .getAcceptanceDateTime()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should include clearing system reference for accepted returns")
    void shouldIncludeClearingSystemReference() {
        // Given
        Pacs004 pacs004 = createValidPacs004();

        // When
        Mono<Pacs002> result = converter.createAcceptedReturn(pacs004, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002.getTransactionInformationAndStatus().get(0)
                    .getClearingSystemReference()).isNotNull();
        });
    }

    // ==================== Helper Methods ====================

    private Pacs004 createValidPacs004() {
        return Pacs004.builder()
                .groupHeader(com.fednow.iso20022.domain.common.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .instructingAgent(createBank("CREDUS33", "Creditor Bank"))
                        .instructedAgent(createBank("DEBTUS33", "Debtor Bank"))
                        .build())
                .transactionInformation(List.of(
                        Pacs004.PaymentReturnTransactionInformation.builder()
                                .returnId(generateReturnId())
                                .originalGroupInformation(Pacs004.OriginalGroupInformation.builder()
                                        .originalMessageId(generateMessageId())
                                        .originalMessageNameIdentification("pacs.008.001.11")
                                        .originalCreationDateTime(getCurrentTimestamp())
                                        .build())
                                .originalEndToEndId("E2E-ORIG-123")
                                .originalTransactionId(generateTransactionId())
                                .originalUetr(generateUetr())
                                .originalInterbankSettlementAmount(createUsdAmount("500.00"))
                                .returnedInterbankSettlementAmount(createUsdAmount("500.00"))
                                .returnReasonInformation(List.of(
                                        Pacs004.ReturnReasonInformation.builder()
                                                .reasonCode("AC01")
                                                .additionalInformation(List.of("Incorrect account"))
                                                .build()
                                ))
                                .instructingAgent(createBank("CREDUS33", "Creditor Bank"))
                                .instructedAgent(createBank("DEBTUS33", "Debtor Bank"))
                                .build()
                ))
                .build();
    }
}
