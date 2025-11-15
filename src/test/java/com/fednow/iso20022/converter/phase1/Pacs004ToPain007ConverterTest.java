package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs004;
import com.fednow.iso20022.domain.pain.Pain007;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pacs004ToPain007Converter.
 * Tests payment return to customer return notification conversion.
 */
@DisplayName("Pacs004ToPain007Converter Tests")
class Pacs004ToPain007ConverterTest extends AbstractConverterTest {

    private Pacs004ToPain007Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs004ToPain007Converter();
    }

    @Test
    @DisplayName("Should convert payment return to customer notification")
    void shouldConvertPaymentReturnToCustomerNotification() {
        // Given
        Pacs004 pacs004 = createPacs004WithReturn("AM04"); // Insufficient funds

        // When
        Mono<Pain007> result = converter.convert(pacs004, context);

        // Then
        verifyConversionSuccess(result, pain007 -> {
            assertThat(pain007).isNotNull();
            assertThat(pain007.getGroupHeader()).isNotNull();
            assertThat(pain007.getPaymentReturnInformation()).hasSize(1);

            Pain007.PaymentReturnInformation returnInfo =
                    pain007.getPaymentReturnInformation().get(0);
            assertThat(returnInfo.getOriginalEndToEndId()).isNotNull();
            assertThat(returnInfo.getReturnReasonInformation()).isNotNull();
            assertThat(returnInfo.getReturnReasonInformation().get(0).getReasonCode())
                    .isEqualTo("AM04");
        });
    }

    @Test
    @DisplayName("Should preserve original payment identifiers")
    void shouldPreserveOriginalPaymentIdentifiers() {
        // Given
        String expectedE2E = "ORIG-E2E-456";
        String expectedTxnId = "ORIG-TXN-789";
        String expectedUetr = generateUetr();

        Pacs004 pacs004 = createPacs004WithReturn("AC01");
        pacs004.getPaymentReturnInformation().get(0).setOriginalEndToEndId(expectedE2E);
        pacs004.getPaymentReturnInformation().get(0).setOriginalTransactionId(expectedTxnId);
        pacs004.getPaymentReturnInformation().get(0).setOriginalUetr(expectedUetr);

        // When
        Mono<Pain007> result = converter.convert(pacs004, context);

        // Then
        verifyConversionSuccess(result, pain007 -> {
            Pain007.PaymentReturnInformation returnInfo =
                    pain007.getPaymentReturnInformation().get(0);
            assertThat(returnInfo.getOriginalEndToEndId()).isEqualTo(expectedE2E);
            assertThat(returnInfo.getOriginalTransactionId()).isEqualTo(expectedTxnId);
            assertThat(returnInfo.getOriginalUetr()).isEqualTo(expectedUetr);
        });
    }

    @Test
    @DisplayName("Should include return amount")
    void shouldIncludeReturnAmount() {
        // Given
        Pacs004 pacs004 = createPacs004WithReturn("AM04");
        pacs004.getPaymentReturnInformation().get(0)
                .setReturnedInterbankSettlementAmount(createUsdAmount("250.00"));

        // When
        Mono<Pain007> result = converter.convert(pacs004, context);

        // Then
        verifyConversionSuccess(result, pain007 -> {
            Pain007.PaymentReturnInformation returnInfo =
                    pain007.getPaymentReturnInformation().get(0);
            assertThat(returnInfo.getReturnedAmount()).isNotNull();
            assertThat(returnInfo.getReturnedAmount().getValue())
                    .isEqualByComparingTo("250.00");
            assertThat(returnInfo.getReturnedAmount().getCurrency()).isEqualTo("USD");
        });
    }

    @Test
    @DisplayName("Should map common return reason codes")
    void shouldMapReturnReasonCodes() {
        // Test different return reason codes
        String[] returnReasons = {"AM04", "AC01", "AC04", "AG01", "CUST"};

        for (String reasonCode : returnReasons) {
            Pacs004 pacs004 = createPacs004WithReturn(reasonCode);

            Mono<Pain007> result = converter.convert(pacs004, context);

            verifyConversionSuccess(result, pain007 -> {
                String actualReason = pain007.getPaymentReturnInformation().get(0)
                        .getReturnReasonInformation().get(0).getReasonCode();
                assertThat(actualReason).isEqualTo(reasonCode);
            });
        }
    }

    @Test
    @DisplayName("Should include additional return information")
    void shouldIncludeAdditionalReturnInformation() {
        // Given
        String additionalInfo = "Account holder name mismatch";
        Pacs004 pacs004 = createPacs004WithReturn("AC01");
        pacs004.getPaymentReturnInformation().get(0).getReturnReasonInformation().get(0)
                .setAdditionalInformation(List.of(additionalInfo));

        // When
        Mono<Pain007> result = converter.convert(pacs004, context);

        // Then
        verifyConversionSuccess(result, pain007 -> {
            List<String> actualInfo = pain007.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getAdditionalInformation();
            assertThat(actualInfo).containsExactly(additionalInfo);
        });
    }

    @Test
    @DisplayName("Should handle multiple payment returns")
    void shouldHandleMultipleReturns() {
        // Given
        Pacs004 pacs004 = createPacs004WithMultipleReturns();

        // When
        Mono<Pain007> result = converter.convert(pacs004, context);

        // Then
        verifyConversionSuccess(result, pain007 -> {
            assertThat(pain007.getPaymentReturnInformation()).hasSize(3);

            // Verify each return has its own reason
            assertThat(pain007.getPaymentReturnInformation().get(0)
                    .getReturnReasonInformation().get(0).getReasonCode()).isEqualTo("AM04");
            assertThat(pain007.getPaymentReturnInformation().get(1)
                    .getReturnReasonInformation().get(0).getReasonCode()).isEqualTo("AC01");
            assertThat(pain007.getPaymentReturnInformation().get(2)
                    .getReturnReasonInformation().get(0).getReasonCode()).isEqualTo("AC04");
        });
    }

    // ==================== Helper Methods ====================

    private Pacs004 createPacs004WithReturn(String returnReasonCode) {
        return Pacs004.builder()
                .groupHeader(Pacs004.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .build())
                .paymentReturnInformation(List.of(
                        Pacs004.PaymentReturnInformation.builder()
                                .returnId("RTN-001")
                                .originalEndToEndId("E2E123")
                                .originalTransactionId(generateTransactionId())
                                .originalUetr(generateUetr())
                                .returnedInterbankSettlementAmount(createUsdAmount("100.00"))
                                .returnReasonInformation(List.of(
                                        Pacs004.ReturnReasonInformation.builder()
                                                .reasonCode(returnReasonCode)
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

    private Pacs004 createPacs004WithMultipleReturns() {
        return Pacs004.builder()
                .groupHeader(Pacs004.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .build())
                .paymentReturnInformation(List.of(
                        createPaymentReturn("AM04", "E2E-1", "100.00"),
                        createPaymentReturn("AC01", "E2E-2", "200.00"),
                        createPaymentReturn("AC04", "E2E-3", "300.00")
                ))
                .build();
    }

    private Pacs004.PaymentReturnInformation createPaymentReturn(
            String returnReasonCode, String e2eId, String amount) {
        return Pacs004.PaymentReturnInformation.builder()
                .returnId("RTN-" + e2eId)
                .originalEndToEndId(e2eId)
                .originalTransactionId(generateTransactionId())
                .originalUetr(generateUetr())
                .returnedInterbankSettlementAmount(createUsdAmount(amount))
                .returnReasonInformation(List.of(
                        Pacs004.ReturnReasonInformation.builder()
                                .reasonCode(returnReasonCode)
                                .build()
                ))
                .build();
    }
}
