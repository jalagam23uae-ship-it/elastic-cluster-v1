package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs008;
import com.fednow.iso20022.domain.pain.Pain001;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pain001ToPacs008Converter.
 * Tests customer payment initiation to interbank payment conversion.
 */
@DisplayName("Pain001ToPacs008Converter Tests")
class Pain001ToPacs008ConverterTest extends AbstractConverterTest {

    private Pain001ToPacs008Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pain001ToPacs008Converter();
    }

    @Test
    @DisplayName("Should convert pain.001 to pacs.008 with all required fields")
    void shouldConvertWithAllRequiredFields() {
        // Given
        Pain001 pain001 = createValidPain001();

        // When
        Mono<Pacs008> result = converter.convert(pain001, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            assertThat(pacs008).isNotNull();
            assertThat(pacs008.getGroupHeader()).isNotNull();
            assertThat(pacs008.getGroupHeader().getMessageId()).isNotEmpty();
            assertThat(pacs008.getGroupHeader().getCreationDateTime()).isNotNull();
            assertThat(pacs008.getGroupHeader().getNumberOfTransactions()).isEqualTo(1);
            assertThat(pacs008.getGroupHeader().getSettlementMethod()).isEqualTo("INDA");

            assertThat(pacs008.getCreditTransferTransactionInformation()).hasSize(1);
            Pacs008.CreditTransferTransaction txn = pacs008.getCreditTransferTransactionInformation().get(0);

            assertThat(txn.getPaymentId()).isNotNull();
            assertThat(txn.getPaymentId().getEndToEndId()).isEqualTo("E2E123");
            assertThat(txn.getPaymentId().getTransactionId()).isNotEmpty();
            assertThat(txn.getPaymentId().getUetr()).isNotEmpty();

            assertThat(txn.getInterbankSettlementAmount()).isNotNull();
            assertThat(txn.getInterbankSettlementAmount().getValue()).isEqualByComparingTo("100.00");
            assertThat(txn.getInterbankSettlementAmount().getCurrency()).isEqualTo("USD");

            assertThat(txn.getInstructingAgent()).isNotNull();
            assertThat(txn.getInstructedAgent()).isNotNull();
            assertThat(txn.getDebtor()).isNotNull();
            assertThat(txn.getCreditor()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should preserve end-to-end ID from pain.001")
    void shouldPreserveEndToEndId() {
        // Given
        String expectedE2E = "CUSTOMER-E2E-12345";
        Pain001 pain001 = createValidPain001();
        pain001.getPaymentInformation().get(0).getCreditTransferTransactionInformation()
                .get(0).getPaymentId().setEndToEndId(expectedE2E);

        // When
        Mono<Pacs008> result = converter.convert(pain001, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            String actualE2E = pacs008.getCreditTransferTransactionInformation()
                    .get(0).getPaymentId().getEndToEndId();
            assertThat(actualE2E).isEqualTo(expectedE2E);
        });
    }

    @Test
    @DisplayName("Should generate UETR for payment tracking")
    void shouldGenerateUetr() {
        // Given
        Pain001 pain001 = createValidPain001();

        // When
        Mono<Pacs008> result = converter.convert(pain001, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            String uetr = pacs008.getCreditTransferTransactionInformation()
                    .get(0).getPaymentId().getUetr();
            assertThat(uetr).isNotEmpty();
            assertThat(uetr).hasSize(36); // UUID format
        });
    }

    @Test
    @DisplayName("Should set settlement method to INDA for FedNow")
    void shouldSetSettlementMethodToINDA() {
        // Given
        Pain001 pain001 = createValidPain001();

        // When
        Mono<Pacs008> result = converter.convert(pain001, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            assertThat(pacs008.getGroupHeader().getSettlementMethod()).isEqualTo("INDA");
        });
    }

    @Test
    @DisplayName("Should map debtor from customer to debtor agent")
    void shouldMapDebtorToDebtorAgent() {
        // Given
        Pain001 pain001 = createValidPain001();
        String debtorName = "ACME Corporation";
        pain001.getPaymentInformation().get(0).setDebtor(createParty(debtorName, "123456789"));

        // When
        Mono<Pacs008> result = converter.convert(pain001, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            assertThat(pacs008.getCreditTransferTransactionInformation().get(0)
                    .getDebtor().getName()).isEqualTo(debtorName);
        });
    }

    @Test
    @DisplayName("Should handle multiple payment transactions")
    void shouldHandleMultipleTransactions() {
        // Given
        Pain001 pain001 = createPain001WithMultipleTransactions();

        // When
        Mono<Pacs008> result = converter.convert(pain001, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            assertThat(pacs008.getCreditTransferTransactionInformation()).hasSize(3);
            assertThat(pacs008.getGroupHeader().getNumberOfTransactions()).isEqualTo(3);

            // Verify total settlement amount
            assertThat(pacs008.getGroupHeader().getTotalInterbankSettlementAmount()
                    .getValue()).isEqualByComparingTo("300.00");
        });
    }

    @Test
    @DisplayName("Should handle remittance information")
    void shouldHandleRemittanceInformation() {
        // Given
        String remittanceInfo = "Invoice #12345 payment";
        Pain001 pain001 = createValidPain001();
        pain001.getPaymentInformation().get(0).getCreditTransferTransactionInformation()
                .get(0).setRemittanceInformation(List.of(remittanceInfo));

        // When
        Mono<Pacs008> result = converter.convert(pain001, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            List<String> actualRemittance = pacs008.getCreditTransferTransactionInformation()
                    .get(0).getRemittanceInformation();
            assertThat(actualRemittance).containsExactly(remittanceInfo);
        });
    }

    // ==================== Helper Methods ====================

    private Pain001 createValidPain001() {
        return Pain001.builder()
                .groupHeader(Pain001.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .numberOfTransactions(1)
                        .initiatingParty(createParty("Initiator Bank", null))
                        .build())
                .paymentInformation(List.of(
                        Pain001.PaymentInformation.builder()
                                .paymentInformationId("PMT-001")
                                .paymentMethod("TRF")
                                .requestedExecutionDate(getCurrentTimestamp().toLocalDate())
                                .debtor(createParty("John Doe", "987654321"))
                                .debtorAgent(createBank("DEBTUS33", "Debtor Bank"))
                                .creditTransferTransactionInformation(List.of(
                                        Pain001.CreditTransferTransaction.builder()
                                                .paymentId(Pain001.PaymentIdentification.builder()
                                                        .endToEndId("E2E123")
                                                        .instructionId("INSTR123")
                                                        .build())
                                                .amount(createUsdAmount("100.00"))
                                                .creditorAgent(createBank("CREDUS33", "Creditor Bank"))
                                                .creditor(createParty("Jane Smith", "123456789"))
                                                .remittanceInformation(List.of("Payment for services"))
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

    private Pain001 createPain001WithMultipleTransactions() {
        return Pain001.builder()
                .groupHeader(Pain001.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .numberOfTransactions(3)
                        .initiatingParty(createParty("Initiator Bank", null))
                        .build())
                .paymentInformation(List.of(
                        Pain001.PaymentInformation.builder()
                                .paymentInformationId("PMT-001")
                                .paymentMethod("TRF")
                                .requestedExecutionDate(getCurrentTimestamp().toLocalDate())
                                .debtor(createParty("John Doe", "987654321"))
                                .debtorAgent(createBank("DEBTUS33", "Debtor Bank"))
                                .creditTransferTransactionInformation(List.of(
                                        createCreditTransferTransaction("100.00", "E2E-1"),
                                        createCreditTransferTransaction("100.00", "E2E-2"),
                                        createCreditTransferTransaction("100.00", "E2E-3")
                                ))
                                .build()
                ))
                .build();
    }

    private Pain001.CreditTransferTransaction createCreditTransferTransaction(String amount, String e2eId) {
        return Pain001.CreditTransferTransaction.builder()
                .paymentId(Pain001.PaymentIdentification.builder()
                        .endToEndId(e2eId)
                        .instructionId("INSTR-" + e2eId)
                        .build())
                .amount(createUsdAmount(amount))
                .creditorAgent(createBank("CREDUS33", "Creditor Bank"))
                .creditor(createParty("Creditor", "123456789"))
                .build();
    }
}
