package com.fednow.iso20022.converter.phase3;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs003;
import com.fednow.iso20022.domain.pain.Pain008;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pain008ToPacs003Converter.
 * Tests customer direct debit to interbank direct debit conversion.
 */
@DisplayName("Pain008ToPacs003Converter Tests")
class Pain008ToPacs003ConverterTest extends AbstractConverterTest {

    private Pain008ToPacs003Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pain008ToPacs003Converter();
    }

    @Test
    @DisplayName("Should convert customer direct debit to interbank direct debit")
    void shouldConvertCustomerDirectDebitToInterbankDirectDebit() {
        // Given
        Pain008 pain008 = createValidPain008("FRST");

        // When
        Mono<Pacs003> result = converter.convert(pain008, context);

        // Then
        verifyConversionSuccess(result, pacs003 -> {
            assertThat(pacs003).isNotNull();
            assertThat(pacs003.getGroupHeader()).isNotNull();
            assertThat(pacs003.getDirectDebitTransactionInformation()).hasSize(1);

            Pacs003.DirectDebitTransaction txn =
                    pacs003.getDirectDebitTransactionInformation().get(0);
            assertThat(txn.getPaymentId()).isNotNull();
            assertThat(txn.getPaymentId().getEndToEndId()).isNotNull();
            assertThat(txn.getPaymentId().getUetr()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("Should preserve mandate information")
    void shouldPreserveMandateInformation() {
        // Given
        String expectedMandateId = "MANDATE-12345";
        LocalDate expectedSignatureDate = LocalDate.now().minusDays(30);

        Pain008 pain008 = createValidPain008("FRST");
        pain008.getPaymentInformation().get(0).getDirectDebitTransactionInformation().get(0)
                .getMandateRelatedInformation().setMandateId(expectedMandateId);
        pain008.getPaymentInformation().get(0).getDirectDebitTransactionInformation().get(0)
                .getMandateRelatedInformation().setDateOfSignature(expectedSignatureDate);

        // When
        Mono<Pacs003> result = converter.convert(pain008, context);

        // Then
        verifyConversionSuccess(result, pacs003 -> {
            Pacs003.MandateRelatedInformation mandateInfo =
                    pacs003.getDirectDebitTransactionInformation().get(0)
                            .getMandateRelatedInformation();
            assertThat(mandateInfo.getMandateId()).isEqualTo(expectedMandateId);
            assertThat(mandateInfo.getDateOfSignature()).isEqualTo(expectedSignatureDate);
        });
    }

    @Test
    @DisplayName("Should handle first collection in series (FRST)")
    void shouldHandleFirstCollection() {
        // Given
        Pain008 pain008 = createValidPain008("FRST");

        // When
        Mono<Pacs003> result = converter.convert(pain008, context);

        // Then
        verifyConversionSuccess(result, pacs003 -> {
            String sequenceType = pacs003.getDirectDebitTransactionInformation().get(0)
                    .getMandateRelatedInformation().getSequenceType();
            assertThat(sequenceType).isEqualTo("FRST");
        });
    }

    @Test
    @DisplayName("Should handle recurring collection (RCUR)")
    void shouldHandleRecurringCollection() {
        // Given
        Pain008 pain008 = createValidPain008("RCUR");

        // When
        Mono<Pacs003> result = converter.convert(pain008, context);

        // Then
        verifyConversionSuccess(result, pacs003 -> {
            String sequenceType = pacs003.getDirectDebitTransactionInformation().get(0)
                    .getMandateRelatedInformation().getSequenceType();
            assertThat(sequenceType).isEqualTo("RCUR");
        });
    }

    @Test
    @DisplayName("Should handle final collection (FNAL)")
    void shouldHandleFinalCollection() {
        // Given
        Pain008 pain008 = createValidPain008("FNAL");

        // When
        Mono<Pacs003> result = converter.convert(pain008, context);

        // Then
        verifyConversionSuccess(result, pacs003 -> {
            String sequenceType = pacs003.getDirectDebitTransactionInformation().get(0)
                    .getMandateRelatedInformation().getSequenceType();
            assertThat(sequenceType).isEqualTo("FNAL");
        });
    }

    @Test
    @DisplayName("Should handle one-off collection (OOFF)")
    void shouldHandleOneOffCollection() {
        // Given
        Pain008 pain008 = createValidPain008("OOFF");

        // When
        Mono<Pacs003> result = converter.convert(pain008, context);

        // Then
        verifyConversionSuccess(result, pacs003 -> {
            String sequenceType = pacs003.getDirectDebitTransactionInformation().get(0)
                    .getMandateRelatedInformation().getSequenceType();
            assertThat(sequenceType).isEqualTo("OOFF");
        });
    }

    @Test
    @DisplayName("Should generate UETR for direct debit tracking")
    void shouldGenerateUetr() {
        // Given
        Pain008 pain008 = createValidPain008("FRST");

        // When
        Mono<Pacs003> result = converter.convert(pain008, context);

        // Then
        verifyConversionSuccess(result, pacs003 -> {
            String uetr = pacs003.getDirectDebitTransactionInformation().get(0)
                    .getPaymentId().getUetr();
            assertThat(uetr).isNotEmpty();
            assertThat(uetr).hasSize(36); // UUID format
        });
    }

    @Test
    @DisplayName("Should reverse debtor and creditor roles")
    void shouldReverseDebtorAndCreditorRoles() {
        // Given
        String creditorName = "Utility Company"; // Initiating the pull
        String debtorName = "Customer"; // Being pulled from

        Pain008 pain008 = createValidPain008("FRST");
        pain008.getPaymentInformation().get(0).setCreditor(createParty(creditorName, "123456789"));
        pain008.getPaymentInformation().get(0).getDirectDebitTransactionInformation().get(0)
                .setDebtor(createParty(debtorName, "987654321"));

        // When
        Mono<Pacs003> result = converter.convert(pain008, context);

        // Then
        verifyConversionSuccess(result, pacs003 -> {
            // In direct debit, creditor initiates (pulls from debtor)
            assertThat(pacs003.getDirectDebitTransactionInformation().get(0)
                    .getCreditor().getName()).isEqualTo(creditorName);
            assertThat(pacs003.getDirectDebitTransactionInformation().get(0)
                    .getDebtor().getName()).isEqualTo(debtorName);
        });
    }

    // ==================== Helper Methods ====================

    private Pain008 createValidPain008(String sequenceType) {
        return Pain008.builder()
                .groupHeader(Pain008.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .numberOfTransactions(1)
                        .initiatingParty(createParty("Creditor Bank", null))
                        .build())
                .paymentInformation(List.of(
                        Pain008.PaymentInformation.builder()
                                .paymentInformationId("PMT-DD-001")
                                .paymentMethod("DD")
                                .requestedCollectionDate(getCurrentTimestamp().toLocalDate().plusDays(2))
                                .creditor(createParty("Utility Company", "123456789"))
                                .creditorAgent(createBank("CREDUS33", "Creditor Bank"))
                                .directDebitTransactionInformation(List.of(
                                        Pain008.DirectDebitTransaction.builder()
                                                .paymentId(Pain008.PaymentIdentification.builder()
                                                        .endToEndId("E2E-DD-123")
                                                        .instructionId("INSTR-DD-123")
                                                        .build())
                                                .instructedAmount(createUsdAmount("50.00"))
                                                .debtor(createParty("Customer", "987654321"))
                                                .debtorAgent(createBank("DEBTUS33", "Debtor Bank"))
                                                .mandateRelatedInformation(
                                                        Pain008.MandateRelatedInformation.builder()
                                                                .mandateId("MANDATE-001")
                                                                .dateOfSignature(LocalDate.now().minusDays(30))
                                                                .sequenceType(sequenceType)
                                                                .build())
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }
}
