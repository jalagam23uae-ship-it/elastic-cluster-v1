package com.fednow.iso20022.converter.phase2;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs007;
import com.fednow.iso20022.domain.pacs.Pacs008;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pacs008ToPacs007Converter.
 * Tests payment to payment reversal conversion.
 */
@DisplayName("Pacs008ToPacs007Converter Tests")
class Pacs008ToPacs007ConverterTest extends AbstractConverterTest {

    private Pacs008ToPacs007Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs008ToPacs007Converter();
    }

    @Test
    @DisplayName("Should convert payment to reversal request")
    void shouldConvertPaymentToReversalRequest() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("reversalReasonCode", "FRAD");

        // When
        Mono<Pacs007> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs007 -> {
            assertThat(pacs007).isNotNull();
            assertThat(pacs007.getGroupHeader()).isNotNull();
            assertThat(pacs007.getReversalInformation()).hasSize(1);

            Pacs007.ReversalInformation reversalInfo =
                    pacs007.getReversalInformation().get(0);
            assertThat(reversalInfo.getReversalReasonInformation()).isNotNull();
            assertThat(reversalInfo.getReversalReasonInformation().get(0).getReasonCode())
                    .isEqualTo("FRAD");
        });
    }

    @Test
    @DisplayName("Should preserve original payment identifiers")
    void shouldPreserveOriginalPaymentIdentifiers() {
        // Given
        String expectedE2E = "E2E-REVERSAL-789";
        String expectedTxnId = "TXN-REVERSAL-456";
        String expectedUetr = generateUetr();

        Pacs008 pacs008 = createValidPacs008();
        pacs008.getCreditTransferTransactionInformation().get(0).getPaymentId()
                .setEndToEndId(expectedE2E);
        pacs008.getCreditTransferTransactionInformation().get(0).getPaymentId()
                .setTransactionId(expectedTxnId);
        pacs008.getCreditTransferTransactionInformation().get(0).getPaymentId()
                .setUetr(expectedUetr);

        context.setAttribute("reversalReasonCode", "CUST");

        // When
        Mono<Pacs007> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs007 -> {
            Pacs007.ReversalInformation reversalInfo = pacs007.getReversalInformation().get(0);
            assertThat(reversalInfo.getOriginalEndToEndId()).isEqualTo(expectedE2E);
            assertThat(reversalInfo.getOriginalTransactionId()).isEqualTo(expectedTxnId);
            assertThat(reversalInfo.getOriginalUetr()).isEqualTo(expectedUetr);
        });
    }

    @Test
    @DisplayName("Should reverse instructing and instructed agents")
    void shouldReverseAgents() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String originalInstructingBic = pacs008.getCreditTransferTransactionInformation()
                .get(0).getInstructingAgent().getBic();
        String originalInstructedBic = pacs008.getCreditTransferTransactionInformation()
                .get(0).getInstructedAgent().getBic();

        context.setAttribute("reversalReasonCode", "FRAD");

        // When
        Mono<Pacs007> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs007 -> {
            // In reversal, agents should be reversed
            Pacs007.ReversalInformation reversalInfo = pacs007.getReversalInformation().get(0);
            assertThat(reversalInfo.getInstructingAgent().getBic())
                    .isEqualTo(originalInstructedBic);
            assertThat(reversalInfo.getInstructedAgent().getBic())
                    .isEqualTo(originalInstructingBic);
        });
    }

    @Test
    @DisplayName("Should handle fraud reversal")
    void shouldHandleFraudReversal() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("reversalReasonCode", "FRAD");
        context.setAttribute("reversalExplanation", "Fraudulent transaction detected");

        // When
        Mono<Pacs007> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs007 -> {
            Pacs007.ReversalReasonInformation reasonInfo =
                    pacs007.getReversalInformation().get(0).getReversalReasonInformation().get(0);
            assertThat(reasonInfo.getReasonCode()).isEqualTo("FRAD");
            assertThat(reasonInfo.getAdditionalInformation())
                    .containsExactly("Fraudulent transaction detected");
        });
    }

    @Test
    @DisplayName("Should handle customer-requested reversal")
    void shouldHandleCustomerRequestedReversal() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("reversalReasonCode", "CUST");

        // When
        Mono<Pacs007> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs007 -> {
            String reasonCode = pacs007.getReversalInformation().get(0)
                    .getReversalReasonInformation().get(0).getReasonCode();
            assertThat(reasonCode).isEqualTo("CUST");
        });
    }

    @Test
    @DisplayName("Should handle duplicate payment reversal")
    void shouldHandleDuplicatePaymentReversal() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("reversalReasonCode", "DUPL");

        // When
        Mono<Pacs007> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs007 -> {
            String reasonCode = pacs007.getReversalInformation().get(0)
                    .getReversalReasonInformation().get(0).getReasonCode();
            assertThat(reasonCode).isEqualTo("DUPL");
        });
    }

    @Test
    @DisplayName("Should preserve reversal amount")
    void shouldPreserveReversalAmount() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        pacs008.getCreditTransferTransactionInformation().get(0)
                .setInterbankSettlementAmount(createUsdAmount("750.00"));
        context.setAttribute("reversalReasonCode", "TECH");

        // When
        Mono<Pacs007> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs007 -> {
            assertThat(pacs007.getReversalInformation().get(0)
                    .getReversedInterbankSettlementAmount().getValue())
                    .isEqualByComparingTo("750.00");
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
