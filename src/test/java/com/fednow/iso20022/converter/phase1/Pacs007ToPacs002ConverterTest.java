package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pacs.Pacs007;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pacs007ToPacs002Converter.
 * Tests reversal acknowledgment conversion.
 */
@DisplayName("Pacs007ToPacs002Converter Tests")
class Pacs007ToPacs002ConverterTest extends AbstractConverterTest {

    private Pacs007ToPacs002Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs007ToPacs002Converter();
    }

    @Test
    @DisplayName("Should convert reversal to acknowledgment")
    void shouldConvertReversalToAcknowledgment() {
        // Given
        Pacs007 pacs007 = createValidPacs007();

        // When
        Mono<Pacs002> result = converter.convert(pacs007, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002).isNotNull();
            assertThat(pacs002.getGroupHeader()).isNotNull();
            assertThat(pacs002.getTransactionInformationAndStatus()).hasSize(1);
        });
    }

    @Test
    @DisplayName("Should acknowledge accepted reversal")
    void shouldAcknowledgeAcceptedReversal() {
        // Given
        Pacs007 pacs007 = createValidPacs007();

        // When
        Mono<Pacs002> result = converter.createAcceptedReversal(pacs007, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002.getTransactionInformationAndStatus().get(0)
                    .getTransactionStatus()).isEqualTo("ACCP");
        });
    }

    @Test
    @DisplayName("Should acknowledge settled reversal")
    void shouldAcknowledgeSettledReversal() {
        // Given
        Pacs007 pacs007 = createValidPacs007();

        // When
        Mono<Pacs002> result = converter.createSettledReversal(pacs007, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002.getTransactionInformationAndStatus().get(0)
                    .getTransactionStatus()).isEqualTo("ACSC");
        });
    }

    @Test
    @DisplayName("Should reject reversal when timing expired")
    void shouldRejectReversalWhenTimingExpired() {
        // Given
        Pacs007 pacs007 = createValidPacs007();

        // When
        Mono<Pacs002> result = converter.createTimingExpiredRejection(pacs007, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            Pacs002.TransactionInformationAndStatus status =
                    pacs002.getTransactionInformationAndStatus().get(0);

            assertThat(status.getTransactionStatus()).isEqualTo("RJCT");
            assertThat(status.getStatusReasonInformation()).isNotEmpty();
            assertThat(status.getStatusReasonInformation().get(0).getReason().getReasonCode())
                    .isEqualTo("TM01");
            assertThat(status.getStatusReasonInformation().get(0).getReason()
                    .getAdditionalInformation())
                    .anyMatch(info -> info.contains("15 seconds"));
        });
    }

    @Test
    @DisplayName("Should reject reversal when already settled")
    void shouldRejectReversalWhenAlreadySettled() {
        // Given
        Pacs007 pacs007 = createValidPacs007();

        // When
        Mono<Pacs002> result = converter.createAlreadySettledRejection(pacs007, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            Pacs002.TransactionInformationAndStatus status =
                    pacs002.getTransactionInformationAndStatus().get(0);

            assertThat(status.getTransactionStatus()).isEqualTo("RJCT");
            assertThat(status.getStatusReasonInformation()).isNotEmpty();
            assertThat(status.getStatusReasonInformation().get(0).getReason().getReasonCode())
                    .isEqualTo("LEGL");
            assertThat(status.getStatusReasonInformation().get(0).getReason()
                    .getAdditionalInformation())
                    .anyMatch(info -> info.contains("already settled"));
        });
    }

    @Test
    @DisplayName("Should handle pending reversal")
    void shouldHandlePendingReversal() {
        // Given
        Pacs007 pacs007 = createValidPacs007();
        String pendingReason = "Reversal requires manual review due to high amount";

        // When
        Mono<Pacs002> result = converter.createPendingReversal(
                pacs007, context, pendingReason);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            Pacs002.TransactionInformationAndStatus status =
                    pacs002.getTransactionInformationAndStatus().get(0);

            assertThat(status.getTransactionStatus()).isEqualTo("PDNG");
            assertThat(status.getStatusReasonInformation()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("Should preserve original reversal IDs")
    void shouldPreserveOriginalReversalIds() {
        // Given
        Pacs007 pacs007 = createValidPacs007();

        // When
        Mono<Pacs002> result = converter.convert(pacs007, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            Pacs002.TransactionInformationAndStatus status =
                    pacs002.getTransactionInformationAndStatus().get(0);

            assertThat(status.getOriginalEndToEndId()).isEqualTo("E2E-ORIG-789");
            assertThat(status.getOriginalTransactionId()).isNotNull();
            assertThat(status.getOriginalUetr()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should reverse agents in response")
    void shouldReverseAgentsInResponse() {
        // Given
        Pacs007 pacs007 = createValidPacs007();

        // When
        Mono<Pacs002> result = converter.convert(pacs007, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            // Agents should be reversed
            assertThat(pacs002.getGroupHeader().getInstructingAgent())
                    .isEqualTo(pacs007.getGroupHeader().getInstructedAgent());
            assertThat(pacs002.getGroupHeader().getInstructedAgent())
                    .isEqualTo(pacs007.getGroupHeader().getInstructingAgent());
        });
    }

    @Test
    @DisplayName("Should include acceptance datetime for accepted reversals")
    void shouldIncludeAcceptanceDateTimeForAccepted() {
        // Given
        Pacs007 pacs007 = createValidPacs007();

        // When
        Mono<Pacs002> result = converter.createAcceptedReversal(pacs007, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002.getTransactionInformationAndStatus().get(0)
                    .getAcceptanceDateTime()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should include clearing system reference for accepted reversals")
    void shouldIncludeClearingSystemReference() {
        // Given
        Pacs007 pacs007 = createValidPacs007();

        // When
        Mono<Pacs002> result = converter.createAcceptedReversal(pacs007, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002.getTransactionInformationAndStatus().get(0)
                    .getClearingSystemReference()).isNotNull();
        });
    }

    // ==================== Helper Methods ====================

    private Pacs007 createValidPacs007() {
        return Pacs007.builder()
                .groupHeader(com.fednow.iso20022.domain.common.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .instructingAgent(createBank("DEBTUS33", "Debtor Bank"))
                        .instructedAgent(createBank("CREDUS33", "Creditor Bank"))
                        .build())
                .originalGroupInformation(Pacs007.OriginalGroupInformation.builder()
                        .originalMessageId(generateMessageId())
                        .originalMessageNameIdentification("pacs.008.001.11")
                        .originalCreationDateTime(getCurrentTimestamp().minusSeconds(10))
                        .build())
                .reversalTransaction(List.of(
                        Pacs007.ReversalTransactionInformation.builder()
                                .reversalId(generateReturnId())
                                .originalEndToEndId("E2E-ORIG-789")
                                .originalTransactionId(generateTransactionId())
                                .originalUetr(generateUetr())
                                .reversedInterbankSettlementAmount(createUsdAmount("750.00"))
                                .interbankSettlementDate(getCurrentDate())
                                .reversalReasonInformation(List.of(
                                        Pacs007.ReversalReasonInformation.builder()
                                                .reason(Pacs007.ReversalReason.builder()
                                                        .reasonCode("CUST")
                                                        .additionalReasonInformation(List.of(
                                                                "Customer requested reversal"))
                                                        .build())
                                                .build()
                                ))
                                .instructingAgent(createBank("DEBTUS33", "Debtor Bank"))
                                .instructedAgent(createBank("CREDUS33", "Creditor Bank"))
                                .build()
                ))
                .build();
    }
}
