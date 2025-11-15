package com.fednow.iso20022.converter.phase2;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.camt.Camt029;
import com.fednow.iso20022.domain.pain.Pain002;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Camt029ToPain002Converter.
 * Tests investigation result to customer status conversion.
 */
@DisplayName("Camt029ToPain002Converter Tests")
class Camt029ToPain002ConverterTest extends AbstractConverterTest {

    private Camt029ToPain002Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Camt029ToPain002Converter();
    }

    @Test
    @DisplayName("Should convert resolved investigation to accepted status")
    void shouldConvertResolvedInvestigationToAcceptedStatus() {
        // Given
        Camt029 camt029 = createInvestigationResult("RSLV", "ACPT");

        // When
        Mono<Pain002> result = converter.convert(camt029, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            assertThat(pain002).isNotNull();
            assertThat(pain002.getOriginalGroupInformationAndStatus()).isNotNull();
            assertThat(pain002.getOriginalGroupInformationAndStatus().getGroupStatus())
                    .isEqualTo("ACCP");

            assertThat(pain002.getTransactionInformationAndStatus()).hasSize(1);
            Pain002.TransactionInformationAndStatus txnStatus =
                    pain002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getTransactionStatus()).isEqualTo("ACCP");
        });
    }

    @Test
    @DisplayName("Should convert pending investigation to pending status")
    void shouldConvertPendingInvestigation() {
        // Given
        Camt029 camt029 = createInvestigationResult("PNDG", "PDNG");

        // When
        Mono<Pain002> result = converter.convert(camt029, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            assertThat(pain002.getOriginalGroupInformationAndStatus().getGroupStatus())
                    .isEqualTo("PDNG");

            Pain002.TransactionInformationAndStatus txnStatus =
                    pain002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getTransactionStatus()).isEqualTo("PDNG");
        });
    }

    @Test
    @DisplayName("Should convert cancelled investigation to rejected status")
    void shouldConvertCancelledInvestigation() {
        // Given
        Camt029 camt029 = createInvestigationResult("CNCL", "CNCL");

        // When
        Mono<Pain002> result = converter.convert(camt029, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            assertThat(pain002.getOriginalGroupInformationAndStatus().getGroupStatus())
                    .isEqualTo("RJCT");

            Pain002.TransactionInformationAndStatus txnStatus =
                    pain002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getTransactionStatus()).isEqualTo("RJCT");
        });
    }

    @Test
    @DisplayName("Should handle no-resolution investigation")
    void shouldHandleNoResolutionInvestigation() {
        // Given
        Camt029 camt029 = createInvestigationWithRejection("NRES", "NFND");

        // When
        Mono<Pain002> result = converter.convert(camt029, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            assertThat(pain002.getOriginalGroupInformationAndStatus().getGroupStatus())
                    .isEqualTo("RJCT");

            Pain002.TransactionInformationAndStatus txnStatus =
                    pain002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getTransactionStatus()).isEqualTo("RJCT");
            assertThat(txnStatus.getStatusReasonInformation()).isNotNull();
            assertThat(txnStatus.getStatusReasonInformation().get(0).getReasonCode())
                    .isEqualTo("NFND");
        });
    }

    @Test
    @DisplayName("Should include customer-friendly message for payment not found")
    void shouldIncludeCustomerFriendlyMessageForPaymentNotFound() {
        // Given
        Camt029 camt029 = createInvestigationWithRejection("NRES", "NFND");

        // When
        Mono<Pain002> result = converter.convert(camt029, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            List<String> additionalInfo = pain002.getTransactionInformationAndStatus().get(0)
                    .getStatusReasonInformation().get(0).getAdditionalInformation();
            assertThat(additionalInfo.get(0))
                    .contains("Payment not found in our records");
        });
    }

    @Test
    @DisplayName("Should handle timed-out investigation")
    void shouldHandleTimedOutInvestigation() {
        // Given
        Camt029 camt029 = createInvestigationWithRejection("NRES", "TIMO");

        // When
        Mono<Pain002> result = converter.convert(camt029, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            Pain002.TransactionInformationAndStatus txnStatus =
                    pain002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getStatusReasonInformation().get(0).getReasonCode())
                    .isEqualTo("TIMO");
            assertThat(txnStatus.getStatusReasonInformation().get(0)
                    .getAdditionalInformation().get(0))
                    .contains("Investigation timed out");
        });
    }

    @Test
    @DisplayName("Should preserve original payment identifiers")
    void shouldPreserveOriginalPaymentIdentifiers() {
        // Given
        String expectedE2E = "ORIG-E2E-INV-123";
        String expectedTxnId = "ORIG-TXN-INV-456";
        String expectedUetr = generateUetr();

        Camt029 camt029 = createInvestigationResult("RSLV", "ACPT");
        camt029.getResolutionOfInvestigation().getInvestigatedCase()
                .setOriginalEndToEndId(expectedE2E);
        camt029.getResolutionOfInvestigation().getInvestigatedCase()
                .setOriginalTransactionId(expectedTxnId);
        camt029.getResolutionOfInvestigation().getInvestigatedCase()
                .setOriginalUetr(expectedUetr);

        // When
        Mono<Pain002> result = converter.convert(camt029, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            Pain002.TransactionInformationAndStatus txnStatus =
                    pain002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getOriginalEndToEndId()).isEqualTo(expectedE2E);
            assertThat(txnStatus.getOriginalTransactionId()).isEqualTo(expectedTxnId);
            assertThat(txnStatus.getOriginalUetr()).isEqualTo(expectedUetr);
        });
    }

    @Test
    @DisplayName("Should handle modified payment confirmation")
    void shouldHandleModifiedPaymentConfirmation() {
        // Given
        Camt029 camt029 = createInvestigationResult("RSLV", "MODI");

        // When
        Mono<Pain002> result = converter.convert(camt029, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            Pain002.TransactionInformationAndStatus txnStatus =
                    pain002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getTransactionStatus()).isEqualTo("ACCP");
            assertThat(txnStatus.getStatusReasonInformation().get(0)
                    .getAdditionalInformation().get(0))
                    .contains("Payment was modified and processed");
        });
    }

    // ==================== Helper Methods ====================

    private Camt029 createInvestigationResult(String investigationStatus, String confirmationCode) {
        return Camt029.builder()
                .resolutionOfInvestigation(Camt029.ResolutionOfInvestigation.builder()
                        .investigationStatus(investigationStatus)
                        .investigatedCase(Camt029.InvestigatedCase.builder()
                                .originalCaseId("CASE-001")
                                .originalMessageId(generateMessageId())
                                .originalMessageNameIdentification("pacs.008.001.11")
                                .originalCreationDateTime(getCurrentTimestamp())
                                .originalEndToEndId("E2E123")
                                .originalTransactionId(generateTransactionId())
                                .originalUetr(generateUetr())
                                .build())
                        .investigationResult(List.of(
                                Camt029.InvestigationResult.builder()
                                        .investigationExecutionConfirmation(
                                                Camt029.InvestigationExecutionConfirmation.builder()
                                                        .confirmationCode(confirmationCode)
                                                        .build())
                                        .build()
                        ))
                        .build())
                .build();
    }

    private Camt029 createInvestigationWithRejection(String investigationStatus, String reasonCode) {
        return Camt029.builder()
                .resolutionOfInvestigation(Camt029.ResolutionOfInvestigation.builder()
                        .investigationStatus(investigationStatus)
                        .investigatedCase(Camt029.InvestigatedCase.builder()
                                .originalCaseId("CASE-001")
                                .originalMessageId(generateMessageId())
                                .originalMessageNameIdentification("pacs.008.001.11")
                                .originalCreationDateTime(getCurrentTimestamp())
                                .originalEndToEndId("E2E123")
                                .originalTransactionId(generateTransactionId())
                                .originalUetr(generateUetr())
                                .build())
                        .investigationResult(List.of(
                                Camt029.InvestigationResult.builder()
                                        .rejectionReason(Camt029.RejectionReason.builder()
                                                .reasonCode(reasonCode)
                                                .build())
                                        .build()
                        ))
                        .build())
                .build();
    }
}
