package com.fednow.iso20022.converter.phase3;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs003;
import com.fednow.iso20022.domain.pacs.Pacs004;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pacs003ToPacs004Converter.
 * Tests direct debit to payment return conversion.
 */
@DisplayName("Pacs003ToPacs004Converter Tests")
class Pacs003ToPacs004ConverterTest extends AbstractConverterTest {

    private Pacs003ToPacs004Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs003ToPacs004Converter();
    }

    @Test
    @DisplayName("Should convert direct debit to payment return")
    void shouldConvertDirectDebitToPaymentReturn() {
        // Given
        Pacs003 pacs003 = createValidPacs003();
        context.setAttribute("returnReasonCode", "MD01"); // No mandate

        // When
        Mono<Pacs004> result = converter.convert(pacs003, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            assertThat(pacs004).isNotNull();
            assertThat(pacs004.getGroupHeader()).isNotNull();
            assertThat(pacs004.getPaymentReturnInformation()).hasSize(1);

            Pacs004.PaymentReturnInformation returnInfo =
                    pacs004.getPaymentReturnInformation().get(0);
            assertThat(returnInfo.getReturnReasonInformation().get(0).getReasonCode())
                    .isEqualTo("MD01");
        });
    }

    @Test
    @DisplayName("Should create return for no mandate (MD01)")
    void shouldCreateReturnForNoMandate() {
        // Given
        Pacs003 pacs003 = createValidPacs003();

        // When
        Mono<Pacs004> result = converter.createNoMandateReturn(pacs003, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            String reasonCode = pacs004.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getReasonCode();
            assertThat(reasonCode).isEqualTo("MD01");
        });
    }

    @Test
    @DisplayName("Should create return for cancelled mandate (MD02)")
    void shouldCreateReturnForCancelledMandate() {
        // Given
        Pacs003 pacs003 = createValidPacs003();

        // When
        Mono<Pacs004> result = converter.createCancelledMandateReturn(pacs003, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            String reasonCode = pacs004.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getReasonCode();
            assertThat(reasonCode).isEqualTo("MD02");
        });
    }

    @Test
    @DisplayName("Should create return for disputed transaction (MD06)")
    void shouldCreateReturnForDisputedTransaction() {
        // Given
        Pacs003 pacs003 = createValidPacs003();

        // When
        Mono<Pacs004> result = converter.createDisputedTransactionReturn(pacs003, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            String reasonCode = pacs004.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getReasonCode();
            assertThat(reasonCode).isEqualTo("MD06");
        });
    }

    @Test
    @DisplayName("Should create return for invalid mandate (MD07)")
    void shouldCreateReturnForInvalidMandate() {
        // Given
        Pacs003 pacs003 = createValidPacs003();

        // When
        Mono<Pacs004> result = converter.createInvalidMandateReturn(pacs003, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            String reasonCode = pacs004.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getReasonCode();
            assertThat(reasonCode).isEqualTo("MD07");
        });
    }

    @Test
    @DisplayName("Should create return for insufficient funds (AM04)")
    void shouldCreateReturnForInsufficientFunds() {
        // Given
        Pacs003 pacs003 = createValidPacs003();

        // When
        Mono<Pacs004> result = converter.createInsufficientFundsReturn(pacs003, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            String reasonCode = pacs004.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getReasonCode();
            assertThat(reasonCode).isEqualTo("AM04");
        });
    }

    @Test
    @DisplayName("Should preserve original payment identifiers")
    void shouldPreserveOriginalPaymentIdentifiers() {
        // Given
        String expectedE2E = "E2E-DD-789";
        String expectedTxnId = "TXN-DD-456";
        String expectedUetr = generateUetr();

        Pacs003 pacs003 = createValidPacs003();
        pacs003.getDirectDebitTransactionInformation().get(0).getPaymentId()
                .setEndToEndId(expectedE2E);
        pacs003.getDirectDebitTransactionInformation().get(0).getPaymentId()
                .setTransactionId(expectedTxnId);
        pacs003.getDirectDebitTransactionInformation().get(0).getPaymentId()
                .setUetr(expectedUetr);

        context.setAttribute("returnReasonCode", "MD01");

        // When
        Mono<Pacs004> result = converter.convert(pacs003, context);

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
    @DisplayName("Should preserve returned amount")
    void shouldPreserveReturnedAmount() {
        // Given
        Pacs003 pacs003 = createValidPacs003();
        pacs003.getDirectDebitTransactionInformation().get(0)
                .setInterbankSettlementAmount(createUsdAmount("150.00"));
        context.setAttribute("returnReasonCode", "MD01");

        // When
        Mono<Pacs004> result = converter.convert(pacs003, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            assertThat(pacs004.getPaymentReturnInformation().get(0)
                    .getReturnedInterbankSettlementAmount().getValue())
                    .isEqualByComparingTo("150.00");
        });
    }

    @Test
    @DisplayName("Should include return explanation")
    void shouldIncludeReturnExplanation() {
        // Given
        String explanation = "Mandate not found in debtor bank records";
        Pacs003 pacs003 = createValidPacs003();
        context.setAttribute("returnReasonCode", "MD01");
        context.setAttribute("returnExplanation", explanation);

        // When
        Mono<Pacs004> result = converter.convert(pacs003, context);

        // Then
        verifyConversionSuccess(result, pacs004 -> {
            List<String> additionalInfo = pacs004.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getAdditionalInformation();
            assertThat(additionalInfo).containsExactly(explanation);
        });
    }

    // ==================== Helper Methods ====================

    private Pacs003 createValidPacs003() {
        return Pacs003.builder()
                .groupHeader(Pacs003.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .numberOfTransactions(1)
                        .settlementMethod("INDA")
                        .build())
                .directDebitTransactionInformation(List.of(
                        Pacs003.DirectDebitTransaction.builder()
                                .paymentId(Pacs003.PaymentIdentification.builder()
                                        .endToEndId("E2E-DD-123")
                                        .transactionId(generateTransactionId())
                                        .uetr(generateUetr())
                                        .build())
                                .interbankSettlementAmount(createUsdAmount("50.00"))
                                .instructingAgent(createBank("CREDUS33", "Creditor Bank"))
                                .instructedAgent(createBank("DEBTUS33", "Debtor Bank"))
                                .creditor(createParty("Utility Company", "123456789"))
                                .debtor(createParty("Customer", "987654321"))
                                .mandateRelatedInformation(Pacs003.MandateRelatedInformation.builder()
                                        .mandateId("MANDATE-001")
                                        .dateOfSignature(LocalDate.now().minusDays(30))
                                        .sequenceType("FRST")
                                        .build())
                                .build()
                ))
                .build();
    }
}
