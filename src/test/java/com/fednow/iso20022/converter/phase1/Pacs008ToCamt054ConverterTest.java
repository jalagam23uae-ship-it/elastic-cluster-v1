package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.camt.Camt054;
import com.fednow.iso20022.domain.pacs.Pacs008;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pacs008ToCamt054Converter.
 * Tests payment to debit/credit notification conversion.
 */
@DisplayName("Pacs008ToCamt054Converter Tests")
class Pacs008ToCamt054ConverterTest extends AbstractConverterTest {

    private Pacs008ToCamt054Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs008ToCamt054Converter();
    }

    @Test
    @DisplayName("Should convert payment to credit notification")
    void shouldConvertPaymentToCreditNotification() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt054> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt054 -> {
            assertThat(camt054).isNotNull();
            assertThat(camt054.getGroupHeader()).isNotNull();
            assertThat(camt054.getNotification()).hasSize(1);

            Camt054.Notification notification = camt054.getNotification().get(0);
            assertThat(notification.getAccount()).isNotNull();
            assertThat(notification.getAccount().getAccountNumber()).isEqualTo("1234567890");
            assertThat(notification.getEntry()).hasSize(1);
        });
    }

    @Test
    @DisplayName("Should create credit entry for incoming payment")
    void shouldCreateCreditEntry() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt054> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt054 -> {
            Camt054.Entry entry = camt054.getNotification().get(0).getEntry().get(0);
            assertThat(entry.getCreditDebitIndicator()).isEqualTo("CRDT");
            assertThat(entry.getAmount()).isNotNull();
            assertThat(entry.getAmount().getValue()).isEqualByComparingTo("100.00");
        });
    }

    @Test
    @DisplayName("Should include transaction details in entry")
    void shouldIncludeTransactionDetails() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt054> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt054 -> {
            Camt054.Entry entry = camt054.getNotification().get(0).getEntry().get(0);
            assertThat(entry.getEntryDetails()).hasSize(1);

            Camt054.TransactionDetails txnDetails = entry.getEntryDetails().get(0);
            assertThat(txnDetails.getReferences()).isNotNull();
            assertThat(txnDetails.getReferences().getEndToEndId()).isEqualTo("E2E123");
            assertThat(txnDetails.getReferences().getUetr()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should include related parties")
    void shouldIncludeRelatedParties() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt054> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt054 -> {
            Camt054.TransactionDetails txnDetails = camt054.getNotification().get(0)
                    .getEntry().get(0).getEntryDetails().get(0);
            assertThat(txnDetails.getRelatedParties()).isNotNull();
            assertThat(txnDetails.getRelatedParties().getDebtor()).isNotNull();
            assertThat(txnDetails.getRelatedParties().getCreditor()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should set booking date and value date")
    void shouldSetDates() {
        // Given
        Pacs008 pacs008 = createValidPacs008();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt054> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt054 -> {
            Camt054.Entry entry = camt054.getNotification().get(0).getEntry().get(0);
            assertThat(entry.getBookingDate()).isNotNull();
            assertThat(entry.getValueDate()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should include remittance information")
    void shouldIncludeRemittanceInformation() {
        // Given
        String remittanceInfo = "Invoice payment #98765";
        Pacs008 pacs008 = createValidPacs008();
        pacs008.getCreditTransferTransactionInformation().get(0)
                .setRemittanceInformation(List.of(remittanceInfo));
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt054> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt054 -> {
            Camt054.TransactionDetails txnDetails = camt054.getNotification().get(0)
                    .getEntry().get(0).getEntryDetails().get(0);
            assertThat(txnDetails.getRemittanceInformation()).containsExactly(remittanceInfo);
        });
    }

    @Test
    @DisplayName("Should handle multiple payments in one notification")
    void shouldHandleMultiplePayments() {
        // Given
        Pacs008 pacs008 = createPacs008WithMultipleTransactions();
        context.setAttribute("accountNumber", "1234567890");

        // When
        Mono<Camt054> result = converter.convert(pacs008, context);

        // Then
        verifyConversionSuccess(result, camt054 -> {
            assertThat(camt054.getNotification().get(0).getEntry()).hasSize(3);

            // All should be credit entries
            camt054.getNotification().get(0).getEntry().forEach(entry ->
                    assertThat(entry.getCreditDebitIndicator()).isEqualTo("CRDT"));
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

    private Pacs008 createPacs008WithMultipleTransactions() {
        return Pacs008.builder()
                .groupHeader(Pacs008.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .numberOfTransactions(3)
                        .settlementMethod("INDA")
                        .build())
                .creditTransferTransactionInformation(List.of(
                        createCreditTransferTransaction("100.00", "E2E-1"),
                        createCreditTransferTransaction("200.00", "E2E-2"),
                        createCreditTransferTransaction("300.00", "E2E-3")
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
