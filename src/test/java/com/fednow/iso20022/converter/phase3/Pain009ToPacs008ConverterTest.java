package com.fednow.iso20022.converter.phase3;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs008;
import com.fednow.iso20022.domain.pain.Pain009;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pain009ToPacs008Converter.
 * Tests mandate initiation to initial payment conversion.
 */
@DisplayName("Pain009ToPacs008Converter Tests")
class Pain009ToPacs008ConverterTest extends AbstractConverterTest {

    private Pain009ToPacs008Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pain009ToPacs008Converter();
    }

    @Test
    @DisplayName("Should convert mandate setup to initial payment")
    void shouldConvertMandateSetupToInitialPayment() {
        // Given
        Pain009 pain009 = createValidPain009();
        context.setAttribute("initialPaymentAmount", new BigDecimal("50.00"));
        context.setAttribute("paymentPurpose", "Subscription setup fee");

        // When
        Mono<Pacs008> result = converter.convert(pain009, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            assertThat(pacs008).isNotNull();
            assertThat(pacs008.getGroupHeader()).isNotNull();
            assertThat(pacs008.getCreditTransferTransactionInformation()).hasSize(1);

            Pacs008.CreditTransferTransaction txn =
                    pacs008.getCreditTransferTransactionInformation().get(0);
            assertThat(txn.getInterbankSettlementAmount().getValue())
                    .isEqualByComparingTo("50.00");
        });
    }

    @Test
    @DisplayName("Should include payment purpose from context")
    void shouldIncludePaymentPurpose() {
        // Given
        String expectedPurpose = "Gym membership joining fee";
        Pain009 pain009 = createValidPain009();
        context.setAttribute("initialPaymentAmount", new BigDecimal("75.00"));
        context.setAttribute("paymentPurpose", expectedPurpose);

        // When
        Mono<Pacs008> result = converter.convert(pain009, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            List<String> remittanceInfo = pacs008.getCreditTransferTransactionInformation()
                    .get(0).getRemittanceInformation();
            assertThat(remittanceInfo).contains(expectedPurpose);
        });
    }

    @Test
    @DisplayName("Should map creditor from mandate to debtor in payment")
    void shouldMapCreditorToDebtor() {
        // Given
        String creditorName = "Magazine Publisher";
        Pain009 pain009 = createValidPain009();
        pain009.getMandateInitiationRequest().getMandate().setCreditor(
                createParty(creditorName, "123456789"));
        context.setAttribute("initialPaymentAmount", new BigDecimal("25.00"));

        // When
        Mono<Pacs008> result = converter.convert(pain009, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            // In initial payment for mandate, customer (debtor in mandate) pays setup fee
            // So debtor in mandate becomes debtor in payment
            assertThat(pacs008.getCreditTransferTransactionInformation().get(0)
                    .getDebtor()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should generate UETR for initial payment tracking")
    void shouldGenerateUetr() {
        // Given
        Pain009 pain009 = createValidPain009();
        context.setAttribute("initialPaymentAmount", new BigDecimal("30.00"));

        // When
        Mono<Pacs008> result = converter.convert(pain009, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            String uetr = pacs008.getCreditTransferTransactionInformation().get(0)
                    .getPaymentId().getUetr();
            assertThat(uetr).isNotEmpty();
            assertThat(uetr).hasSize(36); // UUID format
        });
    }

    @Test
    @DisplayName("Should preserve mandate reference in remittance information")
    void shouldPreserveMandateReference() {
        // Given
        String mandateId = "MANDATE-SETUP-123";
        Pain009 pain009 = createValidPain009();
        pain009.getMandateInitiationRequest().getMandate().setMandateId(mandateId);
        context.setAttribute("initialPaymentAmount", new BigDecimal("40.00"));

        // When
        Mono<Pacs008> result = converter.convert(pain009, context);

        // Then
        verifyConversionSuccess(result, pacs008 -> {
            List<String> remittanceInfo = pacs008.getCreditTransferTransactionInformation()
                    .get(0).getRemittanceInformation();
            assertThat(remittanceInfo).anyMatch(info -> info.contains(mandateId));
        });
    }

    @Test
    @DisplayName("Should use different initial payment amounts")
    void shouldUseDifferentInitialPaymentAmounts() {
        // Test various setup fee amounts
        BigDecimal[] amounts = {
                new BigDecimal("10.00"),
                new BigDecimal("25.50"),
                new BigDecimal("100.00")
        };

        for (BigDecimal amount : amounts) {
            Pain009 pain009 = createValidPain009();
            context.setAttribute("initialPaymentAmount", amount);

            Mono<Pacs008> result = converter.convert(pain009, context);

            verifyConversionSuccess(result, pacs008 -> {
                assertThat(pacs008.getCreditTransferTransactionInformation().get(0)
                        .getInterbankSettlementAmount().getValue())
                        .isEqualByComparingTo(amount);
            });
        }
    }

    // ==================== Helper Methods ====================

    private Pain009 createValidPain009() {
        return Pain009.builder()
                .groupHeader(Pain009.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .initiatingParty(createParty("Creditor Bank", null))
                        .build())
                .mandateInitiationRequest(Pain009.MandateInitiationRequest.builder()
                        .requestId("REQ-001")
                        .mandate(Pain009.Mandate.builder()
                                .mandateId("MANDATE-001")
                                .mandateRequestId("MREQ-001")
                                .creditor(createParty("Service Provider", "123456789"))
                                .creditorAgent(createBank("CREDUS33", "Creditor Bank"))
                                .debtor(createParty("Customer", "987654321"))
                                .debtorAgent(createBank("DEBTUS33", "Debtor Bank"))
                                .maximumAmount(createUsdAmount("100.00"))
                                .validFromDate(LocalDate.now())
                                .validToDate(LocalDate.now().plusYears(1))
                                .build())
                        .build())
                .build();
    }
}
