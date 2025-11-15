package com.fednow.iso20022.converter.phase4;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.camt.Camt052;
import com.fednow.iso20022.domain.common.Amount;
import com.fednow.iso20022.domain.pacs.Pacs008;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pacs008ToCamt052Converter.
 * Tests payment to account report conversion.
 */
@DisplayName("Pacs008ToCamt052Converter Tests")
class Pacs008ToCamt052ConverterTest extends AbstractConverterTest {

    private Pacs008ToCamt052Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs008ToCamt052Converter();
    }

    @Test
    @DisplayName("Should convert payment to account report")
    void shouldConvertPaymentToAccountReport() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            assertThat(camt052).isNotNull();
            assertThat(camt052.getGroupHeader()).isNotNull();
            assertThat(camt052.getReport()).hasSize(1);

            Camt052.Report report = camt052.getReport().get(0);
            assertThat(report.getAccount()).isNotNull();
            assertThat(report.getAccount().getAccountNumber()).isEqualTo("1234567890");
        });
    }

    @Test
    @DisplayName("Should create credit entry for incoming payment")
    void shouldCreateCreditEntry() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            Camt052.Entry entry = camt052.getReport().get(0).getEntry().get(0);
            assertThat(entry.getCreditDebitIndicator()).isEqualTo("CRDT");
            assertThat(entry.getAmount().getValue()).isEqualByComparingTo("100.00");
        });
    }

    @Test
    @DisplayName("Should include current balance when provided")
    void shouldIncludeCurrentBalance() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");
        context.setAttribute("currentBalance", Amount.builder()
                .value(new BigDecimal("5000.00"))
                .currency("USD")
                .build());

        // When
        Mono<Camt052> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            Camt052.Report report = camt052.getReport().get(0);
            assertThat(report.getBalance()).isNotNull();
            assertThat(report.getBalance().get(0).getAmount().getValue())
                    .isEqualByComparingTo("5000.00");
        });
    }

    @Test
    @DisplayName("Should include transaction details")
    void shouldIncludeTransactionDetails() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            Camt052.Entry entry = camt052.getReport().get(0).getEntry().get(0);
            assertThat(entry.getEntryDetails()).hasSize(1);

            Camt052.TransactionDetails txnDetails = entry.getEntryDetails().get(0);
            assertThat(txnDetails.getReferences()).isNotNull();
            assertThat(txnDetails.getReferences().getEndToEndId()).isEqualTo("E2E123");
            assertThat(txnDetails.getReferences().getUetr()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should set booking and value dates")
    void shouldSetDates() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            Camt052.Entry entry = camt052.getReport().get(0).getEntry().get(0);
            assertThat(entry.getBookingDate()).isNotNull();
            assertThat(entry.getValueDate()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should include bank transaction code")
    void shouldIncludeBankTransactionCode() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            Camt052.Entry entry = camt052.getReport().get(0).getEntry().get(0);
            assertThat(entry.getBankTransactionCode()).isNotNull();
            // For instant payment credit: PMNT-RCDT-ESCT
            assertThat(entry.getBankTransactionCode()).contains("PMNT");
        });
    }

    @Test
    @DisplayName("Should include related parties")
    void shouldIncludeRelatedParties() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            Camt052.TransactionDetails txnDetails = camt052.getReport().get(0)
                    .getEntry().get(0).getEntryDetails().get(0);
            assertThat(txnDetails.getRelatedParties()).isNotNull();
            assertThat(txnDetails.getRelatedParties().getDebtor()).isNotNull();
            assertThat(txnDetails.getRelatedParties().getCreditor()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should include remittance information")
    void shouldIncludeRemittanceInformation() {
        // Given
        String remittanceInfo = "Monthly salary payment";
        Pacs008 pacs008 = createValidPacs008();
        pacs008.getCreditTransferTransactionInformation().get(0)
                .setRemittanceInformation(List.of(remittanceInfo));
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            Camt052.TransactionDetails txnDetails = camt052.getReport().get(0)
                    .getEntry().get(0).getEntryDetails().get(0);
            assertThat(txnDetails.getRemittanceInformation()).containsExactly(remittanceInfo);
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
                                .remittanceInformation(List.of("Payment for services"))
                                .build()
                ))
                .build();
    }
}
