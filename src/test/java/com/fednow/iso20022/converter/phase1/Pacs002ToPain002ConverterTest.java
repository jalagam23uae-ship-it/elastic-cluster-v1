package com.fednow.iso20022.converter.phase1;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs002;
import com.fednow.iso20022.domain.pain.Pain002;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pacs002ToPain002Converter.
 * Tests interbank status report to customer status report conversion.
 */
@DisplayName("Pacs002ToPain002Converter Tests")
class Pacs002ToPain002ConverterTest extends AbstractConverterTest {

    private Pacs002ToPain002Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pacs002ToPain002Converter();
    }

    @Test
    @DisplayName("Should convert accepted status to customer status")
    void shouldConvertAcceptedStatus() {
        // Given
        Pacs002 pacs002 = createPacs002WithStatus("ACCP");

        // When
        Mono<Pain002> result = converter.convert(pacs002, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            assertThat(pain002).isNotNull();
            assertThat(pain002.getGroupHeader()).isNotNull();
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
    @DisplayName("Should convert rejected status with reason code")
    void shouldConvertRejectedStatus() {
        // Given
        Pacs002 pacs002 = createPacs002WithRejection("RJCT", "AC01");

        // When
        Mono<Pain002> result = converter.convert(pacs002, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            Pain002.TransactionInformationAndStatus txnStatus =
                    pain002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getTransactionStatus()).isEqualTo("RJCT");
            assertThat(txnStatus.getStatusReasonInformation()).isNotNull();
            assertThat(txnStatus.getStatusReasonInformation().get(0).getReasonCode())
                    .isEqualTo("AC01");
        });
    }

    @Test
    @DisplayName("Should preserve UETR from interbank message")
    void shouldPreserveUetr() {
        // Given
        String expectedUetr = generateUetr();
        Pacs002 pacs002 = createPacs002WithStatus("ACCP");
        pacs002.getTransactionInformationAndStatus().get(0).setOriginalUetr(expectedUetr);

        // When
        Mono<Pain002> result = converter.convert(pacs002, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            String actualUetr = pain002.getTransactionInformationAndStatus()
                    .get(0).getOriginalUetr();
            assertThat(actualUetr).isEqualTo(expectedUetr);
        });
    }

    @Test
    @DisplayName("Should preserve end-to-end ID")
    void shouldPreserveEndToEndId() {
        // Given
        String expectedE2E = "CUSTOMER-E2E-789";
        Pacs002 pacs002 = createPacs002WithStatus("ACCP");
        pacs002.getTransactionInformationAndStatus().get(0).setOriginalEndToEndId(expectedE2E);

        // When
        Mono<Pain002> result = converter.convert(pacs002, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            String actualE2E = pain002.getTransactionInformationAndStatus()
                    .get(0).getOriginalEndToEndId();
            assertThat(actualE2E).isEqualTo(expectedE2E);
        });
    }

    @Test
    @DisplayName("Should handle pending status")
    void shouldHandlePendingStatus() {
        // Given
        Pacs002 pacs002 = createPacs002WithStatus("PDNG");

        // When
        Mono<Pain002> result = converter.convert(pacs002, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            Pain002.TransactionInformationAndStatus txnStatus =
                    pain002.getTransactionInformationAndStatus().get(0);
            assertThat(txnStatus.getTransactionStatus()).isEqualTo("PDNG");
        });
    }

    @Test
    @DisplayName("Should handle multiple transaction statuses")
    void shouldHandleMultipleTransactions() {
        // Given
        Pacs002 pacs002 = createPacs002WithMultipleStatuses();

        // When
        Mono<Pain002> result = converter.convert(pacs002, context);

        // Then
        verifyConversionSuccess(result, pain002 -> {
            assertThat(pain002.getTransactionInformationAndStatus()).hasSize(3);

            // Verify different statuses
            assertThat(pain002.getTransactionInformationAndStatus().get(0).getTransactionStatus())
                    .isEqualTo("ACCP");
            assertThat(pain002.getTransactionInformationAndStatus().get(1).getTransactionStatus())
                    .isEqualTo("RJCT");
            assertThat(pain002.getTransactionInformationAndStatus().get(2).getTransactionStatus())
                    .isEqualTo("PDNG");
        });
    }

    // ==================== Helper Methods ====================

    private Pacs002 createPacs002WithStatus(String status) {
        return Pacs002.builder()
                .groupHeader(Pacs002.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .build())
                .transactionInformationAndStatus(List.of(
                        Pacs002.TransactionStatus.builder()
                                .statusId("STS-001")
                                .originalEndToEndId("E2E123")
                                .originalTransactionId(generateTransactionId())
                                .originalUetr(generateUetr())
                                .transactionStatus(status)
                                .build()
                ))
                .build();
    }

    private Pacs002 createPacs002WithRejection(String status, String reasonCode) {
        return Pacs002.builder()
                .groupHeader(Pacs002.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .build())
                .transactionInformationAndStatus(List.of(
                        Pacs002.TransactionStatus.builder()
                                .statusId("STS-001")
                                .originalEndToEndId("E2E123")
                                .originalTransactionId(generateTransactionId())
                                .originalUetr(generateUetr())
                                .transactionStatus(status)
                                .statusReasonInformation(List.of(
                                        Pacs002.StatusReasonInformation.builder()
                                                .reasonCode(reasonCode)
                                                .additionalInformation(List.of("Invalid account number"))
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

    private Pacs002 createPacs002WithMultipleStatuses() {
        return Pacs002.builder()
                .groupHeader(Pacs002.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .build())
                .transactionInformationAndStatus(List.of(
                        createTransactionStatus("ACCP", "E2E-1", null),
                        createTransactionStatus("RJCT", "E2E-2", "AC01"),
                        createTransactionStatus("PDNG", "E2E-3", null)
                ))
                .build();
    }

    private Pacs002.TransactionStatus createTransactionStatus(String status, String e2eId, String reasonCode) {
        Pacs002.TransactionStatus.TransactionStatusBuilder builder = Pacs002.TransactionStatus.builder()
                .statusId("STS-" + e2eId)
                .originalEndToEndId(e2eId)
                .originalTransactionId(generateTransactionId())
                .originalUetr(generateUetr())
                .transactionStatus(status);

        if (reasonCode != null) {
            builder.statusReasonInformation(List.of(
                    Pacs002.StatusReasonInformation.builder()
                            .reasonCode(reasonCode)
                            .build()
            ));
        }

        return builder.build();
    }
}
