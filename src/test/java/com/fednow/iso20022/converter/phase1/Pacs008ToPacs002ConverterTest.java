package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pacs.Pacs008;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pacs008ToPacs002Converter.
 * Tests payment status report generation from payment instruction.
 */
@DisplayName("Pacs008ToPacs002Converter Tests")
class Pacs008ToPacs002ConverterTest extends AbstractConverterTest {

    private Pacs008ToPacs002Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs008ToPacs002Converter();
    }

    @Test
    @DisplayName("Should create accepted status report")
    void shouldCreateAcceptedStatusReport() {
        // Given
        Pacs008 pacs008 = createValidPacs008();

        // When
        Mono<Pacs002> result = converter.createAcceptedStatus(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002).isNotNull();
            assertThat(pacs002.getGroupHeader()).isNotNull();
            assertThat(pacs002.getTransactionInformationAndStatus()).hasSize(1);

            Pacs002.TransactionStatus txnStatus = pacs002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getTransactionStatus()).isEqualTo("ACCP");
            assertThat(txnStatus.getOriginalEndToEndId()).isEqualTo("E2E123");
        });
    }

    @Test
    @DisplayName("Should create rejected status report")
    void shouldCreateRejectedStatusReport() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        String rejectionReason = "AC01"; // Invalid account number

        // When
        Mono<Pacs002> result = converter.createRejectedStatus(pacs008, rejectionReason, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            Pacs002.TransactionStatus txnStatus = pacs002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getTransactionStatus()).isEqualTo("RJCT");
            assertThat(txnStatus.getStatusReasonInformation()).isNotNull();
            assertThat(txnStatus.getStatusReasonInformation().get(0).getReasonCode())
                    .isEqualTo(rejectionReason);
        });
    }

    @Test
    @DisplayName("Should reverse agent fields (InstgAgt becomes InstdAgt)")
    void shouldReverseAgentFields() {
        // Given
        Pacs008 pacs008 = createValidPacs008();

        // When
        Mono<Pacs002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002.getGroupHeader().getInstructingAgent()).isNotNull();
            assertThat(pacs002.getGroupHeader().getInstructedAgent()).isNotNull();

            // Instructing agent in pacs.002 should be instructed agent from pacs.008
            assertThat(pacs002.getGroupHeader().getInstructingAgent().getBic())
                    .isEqualTo(pacs008.getCreditTransferTransactionInformation()
                            .get(0).getInstructedAgent().getBic());
        });
    }

    @Test
    @DisplayName("Should preserve UETR for tracking")
    void shouldPreserveUetr() {
        // Given
        String expectedUetr = generateUetr();
        Pacs008 pacs008 = createValidPacs008();
        pacs008.getCreditTransferTransactionInformation().get(0)
                .getPaymentId().setUetr(expectedUetr);

        // When
        Mono<Pacs002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            String actualUetr = pacs002.getTransactionInformationAndStatus()
                    .get(0).getOriginalUetr();
            assertThat(actualUetr).isEqualTo(expectedUetr);
        });
    }

    @Test
    @DisplayName("Should handle multiple transactions")
    void shouldHandleMultipleTransactions() {
        // Given
        Pacs008 pacs008 = createPacs008WithMultipleTransactions();

        // When
        Mono<Pacs002> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            assertThat(pacs002.getTransactionInformationAndStatus()).hasSize(3);

            // All should have accepted status
            pacs002.getTransactionInformationAndStatus().forEach(txnStatus ->
                    assertThat(txnStatus.getTransactionStatus()).isEqualTo("ACCP"));
        });
    }

    @Test
    @DisplayName("Should create pending status")
    void shouldCreatePendingStatus() {
        // Given
        Pacs008 pacs008 = createValidPacs008();

        // When
        Mono<Pacs002> result = converter.createPendingStatus(pacs008, context);

        // Then
        verifyConversionSuccess(result, pacs002 -> {
            Pacs002.TransactionStatus txnStatus = pacs002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getTransactionStatus()).isEqualTo("PDNG");
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

    private Pacs008 createPacs008WithMultipleTransactions() {
        return Pacs008.builder()
                .groupHeader(Pacs008.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .numberOfTransactions(3)
                        .settlementMethod("INDA")
                        .totalInterbankSettlementAmount(createUsdAmount("300.00"))
                        .build())
                .creditTransferTransactionInformation(List.of(
                        createCreditTransferTransaction("100.00", "E2E-1"),
                        createCreditTransferTransaction("100.00", "E2E-2"),
                        createCreditTransferTransaction("100.00", "E2E-3")
                ))
                .build();
    }

    private Pacs008.CreditTransferTransaction createCreditTransferTransaction(String amount, String e2eId) {
        return Pacs008.CreditTransferTransaction.builder()
                .paymentId(Pacs008.PaymentIdentification.builder()
                        .endToEndId(e2eId)
                        .transactionId(generateTransactionId())
                        .uetr(generateUetr())
                        .build())
                .interbankSettlementAmount(createUsdAmount(amount))
                .instructingAgent(createBank("INSTUS33", "Instructing Bank"))
                .instructedAgent(createBank("INSTDUS33", "Instructed Bank"))
                .debtor(createParty("Debtor", "987654321"))
                .creditor(createParty("Creditor", "123456789"))
                .build();
    }
}
