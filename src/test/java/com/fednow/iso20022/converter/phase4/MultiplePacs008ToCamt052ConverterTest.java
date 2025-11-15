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
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for MultiplePacs008ToCamt052Converter.
 * Tests multiple payments to consolidated account report conversion.
 */
@DisplayName("MultiplePacs008ToCamt052Converter Tests")
class MultiplePacs008ToCamt052ConverterTest extends AbstractConverterTest {

    private MultiplePacs008ToCamt052Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new MultiplePacs008ToCamt052Converter();
    }

    @Test
    @DisplayName("Should convert multiple payments to consolidated report")
    void shouldConvertMultiplePaymentsToConsolidatedReport() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            assertThat(camt052).isNotNull();
            assertThat(camt052.getReport()).hasSize(1);

            Camt052.Report report = camt052.getReport().get(0);
            assertThat(report.getEntry()).hasSize(3);
        });
    }

    @Test
    @DisplayName("Should calculate opening and closing balances")
    void shouldCalculateOpeningAndClosingBalances() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");
        context.setAttribute("openingBalance", Amount.builder()
                .value(new BigDecimal("1000.00"))
                .currency("USD")
                .build());

        // When
        Mono<Camt052> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            Camt052.Report report = camt052.getReport().get(0);
            assertThat(report.getBalance()).isNotEmpty();

            // Should have opening balance
            boolean hasOpeningBalance = report.getBalance().stream()
                    .anyMatch(bal -> "OPBD".equals(bal.getType()));
            assertThat(hasOpeningBalance).isTrue();

            // Should have closing balance
            boolean hasClosingBalance = report.getBalance().stream()
                    .anyMatch(bal -> "CLBD".equals(bal.getType()));
            assertThat(hasClosingBalance).isTrue();
        });
    }

    @Test
    @DisplayName("Should aggregate all transaction entries")
    void shouldAggregateAllTransactionEntries() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            List<Camt052.Entry> entries = camt052.getReport().get(0).getEntry();
            assertThat(entries).hasSize(3);

            // All should be credit entries
            entries.forEach(entry ->
                    assertThat(entry.getCreditDebitIndicator()).isEqualTo("CRDT"));
        });
    }

    @Test
    @DisplayName("Should handle report period")
    void shouldHandleReportPeriod() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        ZonedDateTime fromTime = getCurrentTimestamp().minusHours(24);
        ZonedDateTime toTime = getCurrentTimestamp();

        context.setAttribute("accountNumber", "1234567890");
        context.setAttribute("reportFromTime", fromTime);
        context.setAttribute("reportToTime", toTime);

        // When
        Mono<Camt052> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            Camt052.Report report = camt052.getReport().get(0);
            assertThat(report.getReportFromDateTime()).isNotNull();
            assertThat(report.getReportToDateTime()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should preserve transaction order")
    void shouldPreserveTransactionOrder() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            List<Camt052.Entry> entries = camt052.getReport().get(0).getEntry();
            assertThat(entries).hasSize(3);

            // Verify amounts are in order
            assertThat(entries.get(0).getAmount().getValue()).isEqualByComparingTo("100.00");
            assertThat(entries.get(1).getAmount().getValue()).isEqualByComparingTo("200.00");
            assertThat(entries.get(2).getAmount().getValue()).isEqualByComparingTo("300.00");
        });
    }

    @Test
    @DisplayName("Should calculate total credits")
    void shouldCalculateTotalCredits() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");
        context.setAttribute("openingBalance", Amount.builder()
                .value(new BigDecimal("1000.00"))
                .currency("USD")
                .build());

        // When
        Mono<Pacs008> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            // Total credits should be 100 + 200 + 300 = 600
            // Closing balance should be 1000 + 600 = 1600
            Camt052.Report report = camt052.getReport().get(0);

            BigDecimal closingBalance = report.getBalance().stream()
                    .filter(bal -> "CLBD".equals(bal.getType()))
                    .map(bal -> bal.getAmount().getValue())
                    .findFirst()
                    .orElse(BigDecimal.ZERO);

            assertThat(closingBalance).isEqualByComparingTo("1600.00");
        });
    }

    @Test
    @DisplayName("Should handle empty payment list")
    void shouldHandleEmptyPaymentList() {
        // Given
        List<Pacs008> payments = List.of();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt052> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt052 -> {
            Camt052.Report report = camt052.getReport().get(0);
            assertThat(report.getEntry()).isEmpty();
        });
    }

    // ==================== Helper Methods ====================

    private List<Pacs008> createMultiplePayments() {
        return List.of(
                createPayment("100.00", "E2E-1"),
                createPayment("200.00", "E2E-2"),
                createPayment("300.00", "E2E-3")
        );
    }

    private Pacs008 createPayment(String amount, String e2eId) {
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
                                        .endToEndId(e2eId)
                                        .transactionId(generateTransactionId())
                                        .uetr(generateUetr())
                                        .build())
                                .interbankSettlementAmount(createUsdAmount(amount))
                                .instructingAgent(createBank("INSTUS33", "Instructing Bank"))
                                .instructedAgent(createBank("INSTDUS33", "Instructed Bank"))
                                .debtor(createParty("Debtor", "987654321"))
                                .creditor(createParty("Creditor", "123456789"))
                                .build()
                ))
                .build();
    }
}
