package com.fednow.iso20022.converter.phase3;

import com.fednow.iso20022.converter.AbstractConverterTest;
import com.fednow.iso20022.domain.pacs.Pacs028;
import com.fednow.iso20022.domain.pain.Pain013;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Pain013ToPacs028Converter.
 * Tests activation request to status request conversion.
 */
@DisplayName("Pain013ToPacs028Converter Tests")
class Pain013ToPacs028ConverterTest extends AbstractConverterTest {

    private Pain013ToPacs028Converter converter;

    @BeforeEach
    void setUpConverter() {
        converter = new Pain013ToPacs028Converter();
    }

    @Test
    @DisplayName("Should convert activation request to status request")
    void shouldConvertActivationRequestToStatusRequest() {
        // Given
        Pain013 pain013 = createValidPain013("ACTV");

        // When
        Mono<Pacs028> result = converter.convert(pain013, context);

        // Then
        verifyConversionSuccess(result, pacs028 -> {
            assertThat(pacs028).isNotNull();
            assertThat(pacs028.getGroupHeader()).isNotNull();
            assertThat(pacs028.getStatusRequest()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should handle activate request")
    void shouldHandleActivateRequest() {
        // Given
        Pain013 pain013 = createValidPain013("ACTV");

        // When
        Mono<Pacs028> result = converter.convert(pain013, context);

        // Then
        verifyConversionSuccess(result, pacs028 -> {
            assertThat(pacs028.getStatusRequest().getRequestType()).isEqualTo("ACTV");
        });
    }

    @Test
    @DisplayName("Should handle deactivate (cancel) request")
    void shouldHandleDeactivateRequest() {
        // Given
        Pain013 pain013 = createValidPain013("CANC");

        // When
        Mono<Pacs028> result = converter.convert(pain013, context);

        // Then
        verifyConversionSuccess(result, pacs028 -> {
            assertThat(pacs028.getStatusRequest().getRequestType()).isEqualTo("CANC");
        });
    }

    @Test
    @DisplayName("Should preserve original payment instruction reference")
    void shouldPreserveOriginalPaymentInstructionReference() {
        // Given
        String expectedInstructionId = "PMT-INSTR-123";
        String expectedE2E = "E2E-STANDING-789";

        Pain013 pain013 = createValidPain013("ACTV");
        pain013.getCreditorPaymentActivationRequest().getPaymentInformation()
                .setOriginalInstructionId(expectedInstructionId);
        pain013.getCreditorPaymentActivationRequest().getPaymentInformation()
                .setOriginalEndToEndId(expectedE2E);

        // When
        Mono<Pacs028> result = converter.convert(pain013, context);

        // Then
        verifyConversionSuccess(result, pacs028 -> {
            assertThat(pacs028.getStatusRequest().getOriginalInstructionId())
                    .isEqualTo(expectedInstructionId);
            assertThat(pacs028.getStatusRequest().getOriginalEndToEndId())
                    .isEqualTo(expectedE2E);
        });
    }

    @Test
    @DisplayName("Should include activation reason")
    void shouldIncludeActivationReason() {
        // Given
        String activationReason = "Customer requested to resume subscription";
        Pain013 pain013 = createValidPain013("ACTV");
        pain013.getCreditorPaymentActivationRequest()
                .setActivationReason(List.of(activationReason));

        // When
        Mono<Pacs028> result = converter.convert(pain013, context);

        // Then
        verifyConversionSuccess(result, pacs028 -> {
            assertThat(pacs028.getStatusRequest().getAdditionalInformation())
                    .contains(activationReason);
        });
    }

    @Test
    @DisplayName("Should query status before activation")
    void shouldQueryStatusBeforeActivation() {
        // Given
        Pain013 pain013 = createValidPain013("ACTV");

        // When
        Mono<Pacs028> result = converter.convert(pain013, context);

        // Then
        verifyConversionSuccess(result, pacs028 -> {
            // Status request should be created to verify current state
            assertThat(pacs028.getStatusRequest()).isNotNull();
            assertThat(pacs028.getStatusRequest().getRequestType()).isEqualTo("ACTV");
        });
    }

    @Test
    @DisplayName("Should query status before deactivation")
    void shouldQueryStatusBeforeDeactivation() {
        // Given
        Pain013 pain013 = createValidPain013("CANC");

        // When
        Mono<Pacs028> result = converter.convert(pain013, context);

        // Then
        verifyConversionSuccess(result, pacs028 -> {
            // Status request should be created to verify no in-flight payments
            assertThat(pacs028.getStatusRequest()).isNotNull();
            assertThat(pacs028.getStatusRequest().getRequestType()).isEqualTo("CANC");
        });
    }

    @Test
    @DisplayName("Should preserve creditor party information")
    void shouldPreserveCreditorPartyInformation() {
        // Given
        String creditorName = "Subscription Service";
        Pain013 pain013 = createValidPain013("ACTV");
        pain013.getCreditorPaymentActivationRequest().setCreditor(
                createParty(creditorName, "123456789"));

        // When
        Mono<Pacs028> result = converter.convert(pain013, context);

        // Then
        verifyConversionSuccess(result, pacs028 -> {
            assertThat(pacs028.getStatusRequest().getRequestingParty()).isNotNull();
            assertThat(pacs028.getStatusRequest().getRequestingParty().getName())
                    .isEqualTo(creditorName);
        });
    }

    // ==================== Helper Methods ====================

    private Pain013 createValidPain013(String activationType) {
        return Pain013.builder()
                .groupHeader(Pain013.GroupHeader.builder()
                        .messageId(generateMessageId())
                        .creationDateTime(getCurrentTimestamp())
                        .initiatingParty(createParty("Creditor Bank", null))
                        .build())
                .creditorPaymentActivationRequest(Pain013.CreditorPaymentActivationRequest.builder()
                        .requestId("ACT-REQ-001")
                        .activationType(activationType)
                        .paymentInformation(Pain013.PaymentInformation.builder()
                                .originalInstructionId("INSTR-001")
                                .originalEndToEndId("E2E-STANDING-001")
                                .build())
                        .creditor(createParty("Service Provider", "123456789"))
                        .creditorAgent(createBank("CREDUS33", "Creditor Bank"))
                        .build())
                .build();
    }
}
