package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs004;
import com.fednow.iso20022.domain.pacs.Pacs008;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pacs008ToPacs004Converter.
 * Tests payment to return conversion.
 */
@DisplayName("Pacs008ToPacs004Converter Tests")
class Pacs008ToPacs004ConverterTest extends AbstractConverterTest {

    private Pacs008ToPacs004Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs008ToPacs004Converter();
    }

    @Test
    @DisplayName("Should convert payment to return")
    void shouldConvertPaymentToReturn() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("returnReasonCode", "AC01");
        context.setAttribute("returnExplanation", "Incorrect account number");

        // When
        Mono<Pacs004> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            assertThat(pacs004).isNotNull();
            assertThat(pacs004.getGroupHeader()).isNotNull();
            assertThat(pacs004.getTransactionInformation()).hasSize(1);

            // Verify agents are reversed
            assertThat(pacs004.getGroupHeader().getInstructingAgent())
                    .isEqualTo(pacs008.getGroupHeader().getInstructedAgent());
            assertThat(pacs004.getGroupHeader().getInstructedAgent())
                    .isEqualTo(pacs008.getGroupHeader().getInstructingAgent());
        });
    }

    @Test
    @DisplayName("Should preserve original payment IDs in return")
    void shouldPreserveOriginalPaymentIds() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("returnReasonCode", "AC01");

        // When
        Mono<Pacs004> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            Pacs004.PaymentReturnTransactionInformation returnInfo =
                    pacs004.getTransactionInformation().get(0);

            assertThat(returnInfo.getOriginalEndToEndId()).isEqualTo("E2E123");
            assertThat(returnInfo.getOriginalTransactionId()).isNotNull();
            assertThat(returnInfo.getOriginalUetr()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should include return reason information")
    void shouldIncludeReturnReason() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String reasonCode = "AC04";
        String explanation = "Account closed";
        context.setAttribute("returnReasonCode", reasonCode);
        context.setAttribute("returnExplanation", explanation);

        // When
        Mono<Pacs004> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            Pacs004.PaymentReturnTransactionInformation returnInfo =
                    pacs004.getTransactionInformation().get(0);

            assertThat(returnInfo.getReturnReasonInformation()).isNotEmpty();
            assertThat(returnInfo.getReturnReasonInformation().get(0).getReasonCode())
                    .isEqualTo(reasonCode);
            assertThat(returnInfo.getReturnReasonInformation().get(0).getAdditionalInformation())
                    .contains(explanation);
        });
    }

    @Test
    @DisplayName("Should handle incorrect account return")
    void shouldHandleIncorrectAccountReturn() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String accountNumber = "9876543210";

        // When
        Mono<Pacs004> result = converter.createIncorrectAccountReturn(
                pacs008, context, accountNumber);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            Pacs004.ReturnReasonInformation reason =
                    pacs004.getTransactionInformation().get(0)
                            .getReturnReasonInformation().get(0);

            assertThat(reason.getReasonCode()).isEqualTo("AC01");
            assertThat(reason.getAdditionalInformation())
                    .anyMatch(info -> info.contains(accountNumber));
        });
    }

    @Test
    @DisplayName("Should handle closed account return")
    void shouldHandleClosedAccountReturn() {
        // Given
        Pacs008 pacs008 = createValidPacs008();

        // When
        Mono<Pacs004> result = converter.createClosedAccountReturn(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            Pacs004.ReturnReasonInformation reason =
                    pacs004.getTransactionInformation().get(0)
                            .getReturnReasonInformation().get(0);

            assertThat(reason.getReasonCode()).isEqualTo("AC04");
            assertThat(reason.getAdditionalInformation())
                    .anyMatch(info -> info.contains("Account closed"));
        });
    }

    @Test
    @DisplayName("Should handle fraud return")
    void shouldHandleFraudReturn() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String fraudDetails = "Fraudulent transaction detected by ML model";

        // When
        Mono<Pacs004> result = converter.createFraudReturn(pacs008, context, fraudDetails);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            Pacs004.ReturnReasonInformation reason =
                    pacs004.getTransactionInformation().get(0)
                            .getReturnReasonInformation().get(0);

            assertThat(reason.getReasonCode()).isEqualTo("FRAD");
            assertThat(reason.getAdditionalInformation())
                    .anyMatch(info -> info.contains(fraudDetails));
        });
    }

    @Test
    @DisplayName("Should handle duplicate payment return")
    void shouldHandleDuplicateReturn() {
        // Given
        Pacs008 pacs008 = createValidPacs008();

        // When
        Mono<Pacs004> result = converter.createDuplicateReturn(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            Pacs004.ReturnReasonInformation reason =
                    pacs004.getTransactionInformation().get(0)
                            .getReturnReasonInformation().get(0);

            assertThat(reason.getReasonCode()).isEqualTo("DUPL");
            assertThat(reason.getAdditionalInformation())
                    .anyMatch(info -> info.contains("Duplicate"));
        });
    }

    @Test
    @DisplayName("Should include original transaction reference")
    void shouldIncludeOriginalTransactionReference() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("returnReasonCode", "AC01");

        // When
        Mono<Pacs004> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            Pacs004.OriginalTransactionReference ref =
                    pacs004.getTransactionInformation().get(0)
                            .getOriginalTransactionReference();

            assertThat(ref).isNotNull();
            assertThat(ref.getInterbankSettlementAmount()).isNotNull();
            assertThat(ref.getDebtor()).isNotNull();
            assertThat(ref.getCreditor()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should set return amount equal to original amount")
    void shouldSetReturnAmountEqualToOriginal() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("returnReasonCode", "AC01");

        // When
        Mono<Pacs004> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            Pacs004.PaymentReturnTransactionInformation returnInfo =
                    pacs004.getTransactionInformation().get(0);

            assertThat(returnInfo.getOriginalInterbankSettlementAmount())
                    .isEqualTo(returnInfo.getReturnedInterbankSettlementAmount());
        });
    }

    @Test
    @DisplayName("Should include original group information")
    void shouldIncludeOriginalGroupInformation() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("returnReasonCode", "AC01");

        // When
        Mono<Pacs004> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            Pacs004.OriginalGroupInformation groupInfo =
                    pacs004.getTransactionInformation().get(0)
                            .getOriginalGroupInformation();

            assertThat(groupInfo).isNotNull();
            assertThat(groupInfo.getOriginalMessageId())
                    .isEqualTo(pacs008.getGroupHeader().getMessageId());
            assertThat(groupInfo.getOriginalMessageNameIdentification())
                    .isEqualTo("pacs.008.001.11");
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
                        .instructingAgent(createBank("INSTUS33", "Instructing Bank"))
                        .instructedAgent(createBank("INSTDUS33", "Instructed Bank"))
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
                                .remittanceInformation(List.of("Payment for services"))
                                .build()
                ))
                .build();
    }
}
