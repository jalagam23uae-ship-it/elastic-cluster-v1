package com.fednow.iso20022.converter.phase4;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.camt.Camt053;
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
 * Unit tests for MultiplePacs008ToCamt053Converter.
 * Tests multiple payments to official account statement conversion.
 */
@DisplayName("MultiplePacs008ToCamt053Converter Tests")
class MultiplePacs008ToCamt053ConverterTest extends AbstractConverterTest {

    private MultiplePacs008ToCamt053Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new MultiplePacs008ToCamt053Converter();
    }

    @Test
    @DisplayName("Should convert multiple payments to official statement")
    void shouldConvertMultiplePaymentsToOfficialStatement() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");
        context.setAttribute("statementNumber", "2024-001");
        context.setAttribute("frequency", "DAIL");

        // When
        Mono<Camt053> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt053 -> {
            assertThat(camt053).isNotNull();
            assertThat(camt053.getStatement()).hasSize(1);

            Camt053.Statement statement = camt053.getStatement().get(0);
            assertThat(statement.getStatementId()).isNotNull();
            assertThat(statement.getEntry()).hasSize(3);
        });
    }

    @Test
    @DisplayName("Should include statement numbering")
    void shouldIncludeStatementNumbering() {
        // Given
        String expectedStatementNumber = "2024-MONTHLY-01";
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");
        context.setAttribute("statementNumber", expectedStatementNumber);
        context.setAttribute("frequency", "MNTH");

        // When
        Mono<Camt053> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt053 -> {
            Camt053.Statement statement = camt053.getStatement().get(0);
            assertThat(statement.getStatementId()).contains(expectedStatementNumber);
        });
    }

    @Test
    @DisplayName("Should handle daily statement frequency")
    void shouldHandleDailyStatementFrequency() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");
        context.setAttribute("frequency", "DAIL");

        // When
        Mono<Camt053> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt053 -> {
            Camt053.Statement statement = camt053.getStatement().get(0);
            assertThat(statement.getFrequency()).isEqualTo("DAIL");
        });
    }

    @Test
    @DisplayName("Should handle monthly statement frequency")
    void shouldHandleMonthlyStatementFrequency() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");
        context.setAttribute("frequency", "MNTH");

        // When
        Mono<Camt053> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt053 -> {
            Camt053.Statement statement = camt053.getStatement().get(0);
            assertThat(statement.getFrequency()).isEqualTo("MNTH");
        });
    }

    @Test
    @DisplayName("Should include both opening and closing balances")
    void shouldIncludeBothOpeningAndClosingBalances() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");
        context.setAttribute("openingBalance", Amount.builder()
                .value(new BigDecimal("10000.00"))
                .currency("USD")
                .build());
        context.setAttribute("openingAvailableBalance", Amount.builder()
                .value(new BigDecimal("9500.00"))
                .currency("USD")
                .build());

        // When
        Mono<Camt053> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt053 -> {
            Camt053.Statement statement = camt053.getStatement().get(0);
            assertThat(statement.getBalance()).hasSizeGreaterThanOrEqualTo(2);

            // Opening booked balance
            boolean hasOpeningBooked = statement.getBalance().stream()
                    .anyMatch(bal -> "OPBD".equals(bal.getType()));
            assertThat(hasOpeningBooked).isTrue();

            // Closing booked balance
            boolean hasClosingBooked = statement.getBalance().stream()
                    .anyMatch(bal -> "CLBD".equals(bal.getType()));
            assertThat(hasClosingBooked).isTrue();
        });
    }

    @Test
    @DisplayName("Should include transaction summary")
    void shouldIncludeTransactionSummary() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt053> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt053 -> {
            Camt053.Statement statement = camt053.getStatement().get(0);
            assertThat(statement.getTransactionSummary()).isNotNull();
            assertThat(statement.getTransactionSummary().getTotalEntries()).isEqualTo(3);
            assertThat(statement.getTransactionSummary().getTotalCreditEntries()).isEqualTo(3);
            assertThat(statement.getTransactionSummary().getTotalDebitEntries()).isEqualTo(0);
        });
    }

    @Test
    @DisplayName("Should calculate total credit amount")
    void shouldCalculateTotalCreditAmount() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt053> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt053 -> {
            Camt053.Statement statement = camt053.getStatement().get(0);
            Camt053.TransactionSummary summary = statement.getTransactionSummary();

            // Total credits: 100 + 200 + 300 = 600
            assertThat(summary.getTotalCreditAmount().getValue())
                    .isEqualByComparingTo("600.00");
        });
    }

    @Test
    @DisplayName("Should handle statement period")
    void shouldHandleStatementPeriod() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        ZonedDateTime fromTime = getCurrentTimestamp().minusDays(1).withHour(0).withMinute(0);
        ZonedDateTime toTime = getCurrentTimestamp().withHour(23).withMinute(59);

        context.setAttribute("accountNumber", "1234567890");
        context.setAttribute("statementFromTime", fromTime);
        context.setAttribute("statementToTime", toTime);

        // When
        Mono<Camt053> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt053 -> {
            Camt053.Statement statement = camt053.getStatement().get(0);
            assertThat(statement.getFromDateTime()).isNotNull();
            assertThat(statement.getToDateTime()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should mark as official statement")
    void shouldMarkAsOfficialStatement() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt053> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt053 -> {
            Camt053.Statement statement = camt053.getStatement().get(0);
            assertThat(statement.isElectronicSequenceNumber()).isNotNull();
            assertThat(statement.getLegalSequenceNumber()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should include all transaction details")
    void shouldIncludeAllTransactionDetails() {
        // Given
        List<Pacs008> payments = createMultiplePayments();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt053> result = converter.convert(payments, context);

        // Then
        verifyConversionSuccess(result, camt053 -> {
            List<Camt053.Entry> entries = camt053.getStatement().get(0).getEntry();

            entries.forEach(entry -> {
                assertThat(entry.getEntryDetails()).isNotEmpty();
                Camt053.TransactionDetails txnDetails = entry.getEntryDetails().get(0);
                assertThat(txnDetails.getReferences()).isNotNull();
                assertThat(txnDetails.getRelatedParties()).isNotNull();
            });
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
