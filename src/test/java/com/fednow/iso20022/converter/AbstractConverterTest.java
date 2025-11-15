package com.fednow.iso20022.converter;

import com.fednow.iso20022.converter.core.ConverterContext;
import com.fednow.iso20022.domain.common.Amount;
import com.fednow.iso20022.domain.common.BankIdentifier;
import com.fednow.iso20022.domain.common.PartyIdentification;
import org.junit.jupiter.api.BeforeEach;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Base class for all converter tests providing common test utilities and helpers.
 */
public abstract class AbstractConverterTest {

    protected ConverterContext context;

    @BeforeEach
    void setUp() {
        context = ConverterContext.builder().build();
    }

    // ==================== Test Data Builders ====================

    protected Amount createAmount(String value, String currency) {
        return Amount.builder()
                .value(new BigDecimal(value))
                .currency(currency)
                .build();
    }

    protected Amount createUsdAmount(String value) {
        return createAmount(value, "USD");
    }

    protected BankIdentifier createBank(String bic, String name) {
        return BankIdentifier.builder()
                .bic(bic)
                .name(name)
                .build();
    }

    protected PartyIdentification createParty(String name, String accountNumber) {
        return PartyIdentification.builder()
                .name(name)
                .accountNumber(accountNumber)
                .build();
    }

    protected PartyIdentification createPartyWithAddress(String name, String accountNumber,
                                                        String addressLine, String country) {
        return PartyIdentification.builder()
                .name(name)
                .accountNumber(accountNumber)
                .addressLine(addressLine)
                .country(country)
                .build();
    }

    protected String generateMessageId() {
        return "MSG-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String generateEndToEndId() {
        return "E2E-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected String generateUetr() {
        return UUID.randomUUID().toString();
    }

    protected ZonedDateTime getCurrentTimestamp() {
        return ZonedDateTime.now();
    }

    // ==================== Assertion Helpers ====================

    protected void assertNotNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new AssertionError(fieldName + " should not be null");
        }
    }

    protected void assertNotEmpty(String str, String fieldName) {
        if (str == null || str.isEmpty()) {
            throw new AssertionError(fieldName + " should not be empty");
        }
    }

    protected void assertAmountEquals(Amount expected, Amount actual) {
        assertNotNull(actual, "Amount");
        if (expected.getValue().compareTo(actual.getValue()) != 0) {
            throw new AssertionError(
                    String.format("Amount value mismatch: expected %s but got %s",
                            expected.getValue(), actual.getValue()));
        }
        if (!expected.getCurrency().equals(actual.getCurrency())) {
            throw new AssertionError(
                    String.format("Currency mismatch: expected %s but got %s",
                            expected.getCurrency(), actual.getCurrency()));
        }
    }

    // ==================== StepVerifier Helpers ====================

    protected <T> void verifyConversionSuccess(reactor.core.publisher.Mono<T> mono,
                                              java.util.function.Consumer<T> assertions) {
        StepVerifier.create(mono)
                .assertNext(assertions)
                .verifyComplete();
    }

    protected <T> void verifyConversionError(reactor.core.publisher.Mono<T> mono,
                                            Class<? extends Throwable> expectedError) {
        StepVerifier.create(mono)
                .expectError(expectedError)
                .verify();
    }
}
